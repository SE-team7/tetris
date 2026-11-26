package com.team.tetris.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * 대전 결과: 승자, 무승부 여부, 종료 사유.
 *  - 승리:      winner != null, isDraw() == false
 *  - 무승부:    winner == null, isDraw() == true  (동시 게임오버·시간 종료 동점)
 *  - 연결 끊김: winner == null, isDraw() == false
 * P2P에서 판정 담당이 확정한 결과를 그대로 전달할 수 있도록 Serializable.
 */
public final class BattleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PlayerId winner;
    private final boolean draw;
    private final BattleEndReason reason;

    private BattleResult(PlayerId winner, boolean draw, BattleEndReason reason) {
        this.winner = winner;
        this.draw = draw;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    // 승자가 있는 결과 (게임오버·시간 종료·상대 퇴장)
    public static BattleResult win(PlayerId winner, BattleEndReason reason) {
        Objects.requireNonNull(winner, "winner");
        if (reason == BattleEndReason.CONNECTION_LOST) {
            throw new IllegalArgumentException("connection lost has no winner");
        }
        return new BattleResult(winner, false, reason);
    }

    // 무승부 (동시 게임오버·시간 종료 동점)
    public static BattleResult draw(BattleEndReason reason) {
        if (reason != BattleEndReason.GAME_OVER && reason != BattleEndReason.TIME_UP) {
            throw new IllegalArgumentException("draw is only for GAME_OVER or TIME_UP: " + reason);
        }
        return new BattleResult(null, true, reason);
    }

    // 연결 끊김: 승자 없이 종료
    public static BattleResult connectionLost() {
        return new BattleResult(null, false, BattleEndReason.CONNECTION_LOST);
    }

    // 승자 없음(무승부·연결 끊김)이면 null
    public PlayerId getWinner() {
        return winner;
    }

    public boolean isDraw() {
        return draw;
    }

    public BattleEndReason getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BattleResult other)) {
            return false;
        }
        return winner == other.winner && draw == other.draw && reason == other.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(winner, draw, reason);
    }

    @Override
    public String toString() {
        return "BattleResult{winner=" + winner + ", draw=" + draw + ", reason=" + reason + "}";
    }
}
