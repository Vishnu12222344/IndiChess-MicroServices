package com.Match_Service.dto;

import com.Match_Service.Model.GameType;
import com.Match_Service.Model.Match;
import com.Match_Service.Model.MatchStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchDTO {

    private Long id;
    private MatchStatus status;
    private GameType gameType;

    // Player emails (strings — no nested User objects)
    private String player1Email;
    private String player2Email;

    // Game state
    private String fenCurrent;
    private String currentTurnEmail;
    private Integer currentPly;
    private String lastMoveUci;

    // Timers
    private Integer whiteTime;
    private Integer blackTime;
    private Long lastMoveTime;

    // Timestamps
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    /**
     * Factory method — maps directly from the new Match entity
     * (which already stores player emails as plain strings).
     */
    public static MatchDTO fromMatch(Match match) {
        MatchDTO dto = new MatchDTO();
        dto.setId(match.getId());
        dto.setStatus(match.getStatus());
        dto.setGameType(match.getGameType());
        dto.setPlayer1Email(match.getPlayer1Email());
        dto.setPlayer2Email(match.getPlayer2Email());
        dto.setFenCurrent(match.getFenCurrent());
        dto.setCurrentTurnEmail(match.getCurrentTurnEmail());
        dto.setCurrentPly(match.getCurrentPly());
        dto.setLastMoveUci(match.getLastMoveUci());
        dto.setWhiteTime(match.getWhiteTime());
        dto.setBlackTime(match.getBlackTime());
        dto.setLastMoveTime(match.getLastMoveTime());
        dto.setStartedAt(match.getStartedAt());
        dto.setFinishedAt(match.getFinishedAt());
        return dto;
    }
}