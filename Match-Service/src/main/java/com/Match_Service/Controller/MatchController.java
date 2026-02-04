package com.Match_Service.Controller;

import com.Match_Service.Model.Match;
import com.Match_Service.Service.MatchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/match")
// ❌ REMOVED @CrossOrigin - Gateway handles CORS now
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /* ================= START PUBLIC MATCH ================= */
    @PostMapping("/start")
    public ResponseEntity<Match> startMatch(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        Match match = matchService.createMatch(principal.getName());

        if (match == null) {
            // User is waiting in queue – 202 Accepted
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok(match);
    }

    /* ================= CREATE PRIVATE MATCH ================= */
    @PostMapping("/create-private")
    public ResponseEntity<Match> createPrivateMatch(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            Match match = matchService.createPrivateMatch(principal.getName());
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /* ================= JOIN MATCH ================= */
    @PostMapping("/{id}/join")
    public ResponseEntity<Match> joinMatch(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            Match match = matchService.joinMatch(id, principal.getName());
            return ResponseEntity.ok(match);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /* ================= GET MATCH ================= */
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatch(@PathVariable Long id) {
        return matchService.getMatch(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /* ================= MAKE MOVE (REST fallback) ================= */
    @PostMapping("/{id}/move")
    public ResponseEntity<?> makeMove(
            @PathVariable Long id,
            @RequestBody MoveRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        try {
            Match updatedMatch = matchService.makeMove(id, principal.getName(), request.getUci());
            return ResponseEntity.ok(updatedMatch);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* ================= RESIGN ================= */
    @PostMapping("/{id}/resign")
    public ResponseEntity<?> resign(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        try {
            Match match = matchService.resign(id, principal.getName());
            return ResponseEntity.ok(match);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* ================= MOVE REQUEST DTO ================= */
    @Data
    public static class MoveRequest {
        private String uci;
        private String san;
        private String fenAfter;  // IGNORED – server is the source of truth
    }
}