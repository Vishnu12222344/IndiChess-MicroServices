package com.Match_Service.Controller;

import com.Match_Service.Model.ChatMessage;
import com.Match_Service.Model.Match;
import com.Match_Service.Repository.ChatMessageRepository;
import com.Match_Service.Repository.MatchRepo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
// ❌ REMOVED @CrossOrigin - WebSocketConfig handles this
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageRepository chatMessageRepository;
    private final MatchRepo matchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{matchId}/chat")
    public void handleChatMessage(
            @DestinationVariable Long matchId,
            ChatMessageRequest request,
            Principal principal
    ) {
        if (principal == null) {
            log.error("❌ No principal for chat message");
            return;
        }

        String senderEmail = principal.getName();
        log.info("💬 Chat message - Match: {}, From: {}, Message: {}",
                matchId, senderEmail, request.getMessage());

        try {
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("Match not found"));

            // Validate sender is a participant (using email strings)
            if (!senderEmail.equals(match.getPlayer1Email()) &&
                    (match.getPlayer2Email() == null || !senderEmail.equals(match.getPlayer2Email()))) {
                throw new RuntimeException("Not a participant in this match");
            }

            // Persist
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMatch(match);
            chatMessage.setSenderEmail(senderEmail);
            chatMessage.setMessage(request.getMessage());
            chatMessage = chatMessageRepository.save(chatMessage);

            // Build response DTO
            Map<String, Object> messageDto = new HashMap<>();
            messageDto.put("id", chatMessage.getId());
            messageDto.put("senderEmail", chatMessage.getSenderEmail());
            messageDto.put("message", chatMessage.getMessage());
            messageDto.put("sentAt", chatMessage.getSentAt().toString());

            // Broadcast to match topic
            messagingTemplate.convertAndSend("/topic/game/" + matchId + "/chat", (Object) messageDto);
            log.info("✅ Chat message broadcast successfully");

        } catch (Exception e) {
            log.error("❌ Error sending chat message: {}", e.getMessage());
        }
    }

    @Data
    public static class ChatMessageRequest {
        private String message;
    }
}