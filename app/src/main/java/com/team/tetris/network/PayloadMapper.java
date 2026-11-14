package com.team.tetris.network;

import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleEndReason;
import com.team.tetris.domain.BattleMode;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;

/**
 * 대전 도메인 객체(2번) ↔ 전송용 데이터(MessagePayload) 변환.
 * 상대가 보낸 값이 도메인 규칙에 맞지 않으면 IllegalArgumentException.
 */
public final class PayloadMapper {

    private PayloadMapper() {
    }

    // ---------------------------------------------------------------- 설정

    public static MessagePayload.Config toPayload(BattleConfig config) {
        return new MessagePayload.Config(
                config.getMode().name(), config.getDifficulty(), config.getTimeLimitSeconds());
    }

    public static BattleConfig toBattleConfig(MessagePayload.Config config) {
        BattleMode mode = parse(BattleMode.class, config.mode());
        // 아이템 모드는 난이도를 쓰지 않으므로 없으면 기본값
        Difficulty difficulty = config.difficulty() == null ? Difficulty.NORMAL : config.difficulty();
        return new BattleConfig(mode, difficulty, config.timeLimitSeconds());
    }

    // ---------------------------------------------------------------- 공격

    /** 빈 공격이면 null (빈 공격은 보내지 않음) */
    public static MessagePayload.Attack toPayload(AttackRows attackRows) {
        if (attackRows == null || attackRows.isEmpty()) {
            return null;
        }
        return new MessagePayload.Attack(attackRows.getRows());
    }

    public static AttackRows toAttackRows(MessagePayload.Attack attack) {
        return new AttackRows(attack.rows());
    }

    // ---------------------------------------------------------------- 화면 상태

    public static MessagePayload.Snapshot toPayload(GameSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new MessagePayload.Snapshot(
                snapshot.getBoard(),
                snapshot.getAttackCells(),
                MessagePayload.BlockState.from(snapshot.getCurrentBlock()),
                MessagePayload.BlockState.from(snapshot.getNextBlock()),
                snapshot.getScore(),
                snapshot.getLevel(),
                snapshot.getRemainingSkips(),
                snapshot.getPendingAttack().getRows(),
                snapshot.isGameOver());
    }

    public static GameSnapshot toGameSnapshot(MessagePayload.Snapshot snapshot) {
        return new GameSnapshot(
                snapshot.board(),
                snapshot.attackCells(),
                snapshot.currentBlock() == null ? null : snapshot.currentBlock().toBlock(),
                snapshot.nextBlock() == null ? null : snapshot.nextBlock().toBlock(),
                snapshot.score(),
                snapshot.level(),
                snapshot.remainingSkips(),
                new AttackRows(snapshot.pendingAttack()),
                snapshot.gameOver());
    }

    // ---------------------------------------------------------------- 결과

    public static MessagePayload.Result toPayload(BattleResult result) {
        if (result == null) {
            return null;
        }
        PlayerId winner = result.getWinner();
        return new MessagePayload.Result(
                winner == null ? null : winner.name(), result.isDraw(), result.getReason().name());
    }

    public static BattleResult toBattleResult(MessagePayload.Result result) {
        BattleEndReason reason = parse(BattleEndReason.class, result.reason());
        if (result.draw()) {
            return BattleResult.draw(reason);
        }
        if (result.winner() == null) {
            if (reason != BattleEndReason.CONNECTION_LOST) {
                throw new IllegalArgumentException("result without winner must be draw or connection lost");
            }
            return BattleResult.connectionLost();
        }
        return BattleResult.win(parse(PlayerId.class, result.winner()), reason);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("unknown " + type.getSimpleName() + ": " + name, e);
        }
    }
}
