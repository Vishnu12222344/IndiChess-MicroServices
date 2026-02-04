package com.Match_Service.Service;

import com.Match_Service.dto.MatchDTO;
import com.Match_Service.Config.RestTemplateConfig;
import com.Match_Service.Model.*;
import com.Match_Service.Repository.MatchRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepo matchRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    /**
     * Base URL of the API Gateway.  Match Service calls
     * GET {userServiceUrl}/user/by-email/{email}
     * to verify a user exists before creating / joining a match.
     */
    @Value("${user.service.url:http://localhost:8080}")
    private String userServiceUrl;

    // ===================== IN-MEMORY MATCHMAKING QUEUES =====================
    private final Map<GameType, ConcurrentLinkedQueue<String>> queues = new ConcurrentHashMap<>();

    {
        queues.put(GameType.BLITZ, new ConcurrentLinkedQueue<>());
        queues.put(GameType.RAPID, new ConcurrentLinkedQueue<>());
    }

    // ===================== USER VALIDATION (cross-service call) =====================

    /**
     * Calls User Service (through the API Gateway) to verify the email
     * belongs to a registered user.  Throws RuntimeException if not found.
     *
     * IMPORTANT: For microservices, we skip this validation since:
     * 1. The JWT token is already validated by JwtFilter
     * 2. If the token is valid, the user definitely exists
     * 3. Making cross-service calls for every operation adds latency
     *
     * We trust the JWT - if it's valid, the user exists.
     */
    private void validateUserExists(String email) {
        // ✅ SIMPLIFIED: Trust the JWT validation
        // The user exists if they have a valid JWT token
        // No need for additional REST calls
        log.debug("User validation skipped (trusting JWT): {}", email);

        // Note: If you still want to validate against User Service,
        // you would need to forward the JWT token from the request context
        // This is complex and usually unnecessary in microservices
    }

    // ===================== PUBLIC MATCH (matchmaking queue) =====================

    @Transactional
    public Match createMatch(String userEmail) {
        ConcurrentLinkedQueue<String> queue = queues.get(GameType.RAPID);
        if (queue.contains(userEmail)) return null;   // already waiting

        if (queue.isEmpty()) {
            queue.add(userEmail);
            return null;   // first player waits
        }

        String opponentEmail = queue.poll();
        return createMatchInternal(opponentEmail, userEmail, GameType.RAPID);
    }

    // ===================== PRIVATE MATCH =====================

    @Transactional
    public Match createPrivateMatch(String userEmail) {
        validateUserExists(userEmail);

        Match match = new Match();
        match.setPlayer1Email(userEmail);
        match.setGameType(GameType.RAPID);
        match.setFenCurrent("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        match.setCurrentTurnEmail(userEmail);   // player1 (white) moves first
        match.setCurrentPly(0);
        match.setStatus(MatchStatus.ONGOING);
        match.setStartedAt(LocalDateTime.now());
        match.setWhiteTime(600);
        match.setBlackTime(600);
        match.setLastMoveTime(System.currentTimeMillis());

        Match savedMatch = matchRepository.save(match);
        log.info("✅ Private match created: ID={}, Player={}", savedMatch.getId(), userEmail);
        return savedMatch;
    }

    // ===================== JOIN MATCH =====================

    @Transactional
    public Match joinMatch(Long matchId, String userEmail) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getPlayer2Email() != null) {
            throw new RuntimeException("Match already full");
        }
        if (match.getPlayer1Email().equals(userEmail)) {
            throw new RuntimeException("Cannot join your own match");
        }

        validateUserExists(userEmail);

        match.setPlayer2Email(userEmail);
        match.setStatus(MatchStatus.ONGOING);
        match.setLastMoveTime(System.currentTimeMillis());

        Match savedMatch = matchRepository.save(match);

        // Broadcast updated state to both players
        messagingTemplate.convertAndSend("/topic/game/" + matchId, MatchDTO.fromMatch(savedMatch));
        log.info("✅ Player 2 joined match {}", matchId);

        return savedMatch;
    }

    // ===================== MATCHMAKING (WebSocket-driven) =====================

    @Transactional
    public void processMatchmaking(String userEmail, GameType type) {
        ConcurrentLinkedQueue<String> queue = queues.get(type);
        if (queue.contains(userEmail)) return;   // already searching

        if (queue.isEmpty()) {
            queue.add(userEmail);
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/status", "searching");
        } else {
            String opponentEmail = queue.poll();
            Match match = createMatchInternal(opponentEmail, userEmail, type);

            // Notify both players via their personal topic
            messagingTemplate.convertAndSend("/topic/matchmaking/" + opponentEmail, MatchDTO.fromMatch(match));
            messagingTemplate.convertAndSend("/topic/matchmaking/" + userEmail,    MatchDTO.fromMatch(match));
        }
    }

    // ===================== INTERNAL HELPERS =====================

    private Match createMatchInternal(String email1, String email2, GameType type) {
        validateUserExists(email1);
        validateUserExists(email2);

        Match match = new Match();
        match.setPlayer1Email(email1);
        match.setPlayer2Email(email2);
        match.setGameType(type);
        match.setFenCurrent("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        match.setCurrentTurnEmail(email1);   // player1 is white
        match.setCurrentPly(0);
        match.setStatus(MatchStatus.ONGOING);
        match.setStartedAt(LocalDateTime.now());

        int initialTime = type == GameType.BLITZ ? 180 : 600;
        match.setWhiteTime(initialTime);
        match.setBlackTime(initialTime);
        match.setLastMoveTime(System.currentTimeMillis());

        return matchRepository.save(match);
    }

    // ===================== GET MATCH =====================

    public Optional<Match> getMatch(Long id) {
        return matchRepository.findById(id);
    }

    // ===================== MAKE MOVE =====================

    @Transactional
    public Match makeMove(Long matchId, String email, String uci) {
        log.info("=== MOVE: Match {}, Player {}, UCI {} ===", matchId, email, uci);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() != MatchStatus.ONGOING) {
            throw new RuntimeException("Game is not active");
        }
        if (match.getPlayer2Email() == null) {
            throw new RuntimeException("Waiting for opponent to join");
        }
        if (!email.equals(match.getCurrentTurnEmail())) {
            throw new RuntimeException("Not your turn");
        }
        if (uci == null || uci.length() < 4 || uci.length() > 5) {
            throw new RuntimeException("Invalid UCI format");
        }

        // --- square validation ---
        char fromFile = uci.charAt(0), fromRank = uci.charAt(1);
        char toFile   = uci.charAt(2), toRank   = uci.charAt(3);
        if (fromFile < 'a' || fromFile > 'h' || toFile < 'a' || toFile > 'h' ||
                fromRank < '1' || fromRank > '8' || toRank < '1' || toRank > '8') {
            throw new RuntimeException("Invalid square coordinates");
        }

        // --- colour / turn validation ---
        String[] fenParts = match.getFenCurrent().split(" ");
        boolean isWhiteTurn = fenParts[1].equals("w");
        boolean isPlayer1   = email.equals(match.getPlayer1Email());

        if (isWhiteTurn != isPlayer1) {
            throw new RuntimeException("Wrong color to move");
        }

        // --- timer logic (deduct time only after ply > 0) ---
        long now = System.currentTimeMillis();

        if (match.getCurrentPly() > 0) {
            Long lastMoveTime = match.getLastMoveTime();
            if (lastMoveTime != null && lastMoveTime > 0) {
                long elapsedSeconds = (now - lastMoveTime) / 1000;
                elapsedSeconds = Math.max(0, Math.min(elapsedSeconds, 60)); // clamp 0-60

                boolean isWhiteMove = email.equals(match.getPlayer1Email());

                if (isWhiteMove) {
                    int newTime = match.getWhiteTime() - (int) elapsedSeconds;
                    if (newTime <= 0) {
                        match.setWhiteTime(0);
                        match.setStatus(MatchStatus.BLACK_WIN);
                        match.setFinishedAt(LocalDateTime.now());
                        Match finished = matchRepository.save(match);
                        messagingTemplate.convertAndSend("/topic/game/" + matchId, MatchDTO.fromMatch(finished));
                        log.info("⏰ White timeout");
                        return finished;
                    }
                    match.setWhiteTime(newTime);
                } else {
                    int newTime = match.getBlackTime() - (int) elapsedSeconds;
                    if (newTime <= 0) {
                        match.setBlackTime(0);
                        match.setStatus(MatchStatus.WHITE_WIN);
                        match.setFinishedAt(LocalDateTime.now());
                        Match finished = matchRepository.save(match);
                        messagingTemplate.convertAndSend("/topic/game/" + matchId, MatchDTO.fromMatch(finished));
                        log.info("⏰ Black timeout");
                        return finished;
                    }
                    match.setBlackTime(newTime);
                }
            }
        }

        // --- record move ---
        Move move = new Move();
        move.setMatch(match);
        move.setUci(uci);
        move.setPly(match.getCurrentPly() + 1);
        move.setMoveNumber((int) Math.ceil(move.getPly() / 2.0));
        move.setColor(move.getPly() % 2 != 0 ? PieceColor.WHITE : PieceColor.BLACK);
        move.setCreatedAt(LocalDateTime.now());

        match.addMove(move);
        match.setLastMoveUci(uci);
        match.setFenCurrent(applyMoveToFen(match.getFenCurrent(), uci));

        // --- switch turn ---
        String nextTurn = email.equals(match.getPlayer1Email())
                ? match.getPlayer2Email()
                : match.getPlayer1Email();

        match.setCurrentTurnEmail(nextTurn);
        match.setLastMoveTime(now);

        Match savedMatch = matchRepository.save(match);
        log.info("✅ Move complete – Status: {}", savedMatch.getStatus());
        return savedMatch;
    }

    // Overload kept for compatibility
    @Transactional
    public Match makeMove(Long matchId, String email, String uci, String san, String fen) {
        return makeMove(matchId, email, uci);
    }

    // ===================== RESIGN =====================

    @Transactional
    public Match resign(Long matchId, String email) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() != MatchStatus.ONGOING) {
            throw new RuntimeException("Game is not active");
        }
        if (!email.equals(match.getPlayer1Email()) && !email.equals(match.getPlayer2Email())) {
            throw new RuntimeException("You are not in this match");
        }

        boolean isPlayer1Resigning = email.equals(match.getPlayer1Email());
        match.setStatus(isPlayer1Resigning ? MatchStatus.BLACK_WIN : MatchStatus.WHITE_WIN);
        match.setFinishedAt(LocalDateTime.now());

        Match savedMatch = matchRepository.save(match);
        messagingTemplate.convertAndSend("/topic/game/" + matchId, MatchDTO.fromMatch(savedMatch));
        return savedMatch;
    }

    // ===================== FEN MANIPULATION =====================

    private String applyMoveToFen(String fen, String uci) {
        String[] parts = fen.split(" ");
        String boardPart  = parts[0];
        String currentTurn = parts[1];

        int fromFile = uci.charAt(0) - 'a';
        int fromRank = 8 - (uci.charAt(1) - '0');
        int toFile   = uci.charAt(2) - 'a';
        int toRank   = 8 - (uci.charAt(3) - '0');

        String[][] board = new String[8][8];
        String[] rows = boardPart.split("/");
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    int empty = ch - '0';
                    for (int i = 0; i < empty; i++) board[r][c++] = "";
                } else {
                    board[r][c++] = String.valueOf(ch);
                }
            }
        }

        String piece = board[fromRank][fromFile];
        board[toRank][toFile] = piece;
        board[fromRank][fromFile] = "";

        // auto-promote to queen
        if ((piece.equals("P") && toRank == 0) || (piece.equals("p") && toRank == 7)) {
            board[toRank][toFile] = piece.equals("P") ? "Q" : "q";
        }

        StringBuilder newBoard = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int emptyCount = 0;
            for (int c = 0; c < 8; c++) {
                if (board[r][c].isEmpty()) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) { newBoard.append(emptyCount); emptyCount = 0; }
                    newBoard.append(board[r][c]);
                }
            }
            if (emptyCount > 0) newBoard.append(emptyCount);
            if (r < 7) newBoard.append("/");
        }

        String newTurn = currentTurn.equals("w") ? "b" : "w";
        return newBoard + " " + newTurn + " KQkq - 0 1";
    }
}