package com.Match_Service.Controller;

import com.Match_Service.dto.MatchDTO;
import com.Match_Service.Model.Match;
import com.Match_Service.Service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
// ❌ REMOVED @CrossOrigin - WebSocketConfig handles this
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {

    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{matchId}/move")
    public void handleMove(
            @DestinationVariable Long matchId,
            Map<String, String> payload,
            Principal principal
    ) {
        log.info("🎨 WS Move - Match: {}, Player: {}, UCI: {}",
                matchId,
                principal != null ? principal.getName() : "NULL",
                payload.get("uci"));

        if (principal == null) {
            log.error("❌ No principal");
            return;
        }

        String uci = payload.get("uci");
        if (uci == null || uci.length() < 4) {
            log.error("❌ Invalid UCI");
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("error", "Invalid UCI format")
            );
            return;
        }

        try {
            Match updatedMatch = matchService.makeMove(matchId, principal.getName(), uci);
            MatchDTO dto = MatchDTO.fromMatch(updatedMatch);

            log.info("✅ Broadcasting to /topic/game/{}", matchId);
            messagingTemplate.convertAndSend("/topic/game/" + matchId, dto);

        } catch (RuntimeException e) {
            log.error("❌ Error: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("error", e.getMessage())
            );
        }
    }
}