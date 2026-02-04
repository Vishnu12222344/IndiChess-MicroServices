package com.Match_Service.Controller;

import com.Match_Service.Model.ChatMessage;
import com.Match_Service.Repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/match/{matchId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(
            @PathVariable Long matchId,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        List<ChatMessage> messages = chatMessageRepository.findByMatchIdOrderBySentAtAsc(matchId);

        List<Map<String, Object>> messageDtos = messages.stream()
                .map(msg -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", msg.getId());
                    dto.put("senderEmail", msg.getSenderEmail());
                    dto.put("message", msg.getMessage());
                    dto.put("sentAt", msg.getSentAt().toString());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(messageDtos);
    }
}