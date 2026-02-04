package com.Match_Service.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= PLAYERS (denormalised emails) ================= */
    @Column(name = "player1_email", nullable = false)
    private String player1Email;

    @Column(name = "player2_email")
    private String player2Email;   // null until opponent joins

    /* ================= GAME STATE ================= */
    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    private String currentTurnEmail;

    private Integer currentPly;

    @Column(name = "fen_current", nullable = false, length = 200)
    private String fenCurrent;

    @Column(name = "last_move_uci", length = 10)
    private String lastMoveUci;

    /* ================= TIMER ================= */
    @Column(name = "white_time")
    private Integer whiteTime;   // seconds

    @Column(name = "black_time")
    private Integer blackTime;   // seconds

    @Column(name = "last_move_time")
    private Long lastMoveTime;   // epoch millis

    /* ================= MOVES ================= */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ply ASC")
    private List<Move> moves = new ArrayList<>();

    /* ================= TIMESTAMPS ================= */
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    /* ================= HELPERS ================= */
    public void addMove(Move move) {
        this.moves.add(move);
        move.setMatch(this);
        this.currentPly = move.getPly();
    }
}