package com.Match_Service.Controller;

import com.Match_Service.Model.GameType;
import com.Match_Service.Service.MatchService;
import lombok.Data;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MatchmakingController {

    private final MatchService matchService;

    public MatchmakingController(MatchService matchService) {
        this.matchService = matchService;
    }

    @MessageMapping("/matchmaking/join")
    public void joinQueue(@Payload MatchmakingRequest request, Principal principal) {
        // principal.getName() returns the email extracted from the JWT
        // by WebSocketAuthInterceptor — no change needed here
        matchService.processMatchmaking(principal.getName(), request.getGameType());
    }

    @Data
    public static class MatchmakingRequest {
        private GameType gameType;
    }
}