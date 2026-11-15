package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleEndReason;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Position;
import com.team.tetris.service.GameEngineService;

/** PayloadMapper: 2번 대전 도메인 객체 ↔ 전송용 데이터 변환 */
class PayloadMapperTest {

    // ---------------------------------------------------------------- 설정

    @Test
    void config_roundTripsEveryMode() {
        BattleConfig[] configs = {
                BattleConfig.normal(Difficulty.HARD),
                BattleConfig.item(),
                BattleConfig.timeLimit(Difficulty.EASY, 180),
                BattleConfig.timeLimit(Difficulty.NORMAL, 300),
                BattleConfig.timeLimit(Difficulty.HARD, 600)};

        for (BattleConfig config : configs) {
            assertEquals(config, PayloadMapper.toBattleConfig(PayloadMapper.toPayload(config)), config.toString());
        }
    }

    @Test
    void config_payloadUsesModeName() {
        MessagePayload.Config payload = PayloadMapper.toPayload(BattleConfig.timeLimit(Difficulty.EASY, 300));

        assertEquals("TIME_LIMIT", payload.mode());
        assertEquals(Difficulty.EASY, payload.difficulty());
        assertEquals(300, payload.timeLimitSeconds());
    }

    @Test
    void config_missingDifficultyUsesDefault() {
        BattleConfig config = PayloadMapper.toBattleConfig(new MessagePayload.Config("ITEM", null, 0));

        assertEquals(BattleConfig.item(), config);
    }

    @Test
    void config_rejectsValuesBreakingBattleRules() {
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleConfig(new MessagePayload.Config("UNKNOWN", Difficulty.EASY, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleConfig(new MessagePayload.Config("TIME_LIMIT", Difficulty.EASY, 100)));
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleConfig(new MessagePayload.Config("NORMAL", Difficulty.EASY, 180)));
    }

    // ---------------------------------------------------------------- 공격

    @Test
    void attack_roundTripsRowsAndEmptyCells() {
        BlockType[][] rows = NetworkTestData.attackRows(4, 3);

        AttackRows restored = PayloadMapper.toAttackRows(PayloadMapper.toPayload(new AttackRows(rows)));

        assertTrue(Arrays.deepEquals(rows, restored.getRows()));
        assertNull(restored.getRows()[0][3]);
    }

    @Test
    void attack_emptyOrNullIsNotSent() {
        assertNull(PayloadMapper.toPayload(AttackRows.empty()));
        assertNull(PayloadMapper.toPayload((AttackRows) null));
    }

    // ---------------------------------------------------------------- 화면 상태

    @Test
    void snapshot_roundTripsRealEngineSnapshot() {
        GameEngineService engine = new GameEngineService();
        engine.startGame(Difficulty.NORMAL, false);
        engine.tick();
        GameSnapshot original = engine.getSnapshot();

        GameSnapshot restored = PayloadMapper.toGameSnapshot(PayloadMapper.toPayload(original));

        assertSnapshotEquals(original, restored);
    }

    @Test
    void snapshot_roundTripsAttackCellsPendingAttackAndItemBlock() {
        BlockType[][] board = new BlockType[Board.ROWS][Board.COLS];
        boolean[][] attackCells = new boolean[Board.ROWS][Board.COLS];
        board[Board.ROWS - 1][0] = BlockType.Z;
        attackCells[Board.ROWS - 1][0] = true;
        Block current = new Block(BlockType.T, new Position(4, 4), ItemType.LINE_CLEAR, 1);
        current.rotate();
        Block next = new Block(BlockType.BOMB, new Position(1, 4));
        GameSnapshot original = new GameSnapshot(board, attackCells, current, next, 1500, 4, 2,
                new AttackRows(NetworkTestData.attackRows(3, 5)), true);

        GameSnapshot restored = PayloadMapper.toGameSnapshot(PayloadMapper.toPayload(original));

        assertSnapshotEquals(original, restored);
        assertTrue(restored.getAttackCells()[Board.ROWS - 1][0]);
        assertEquals(ItemType.LINE_CLEAR, restored.getCurrentBlock().getItemType());
        assertEquals(1, restored.getCurrentBlock().getRotation());
        assertEquals(BlockType.BOMB, restored.getNextBlock().getType());
        assertEquals(3, restored.getPendingAttack().getRowCount());
        assertTrue(restored.isGameOver());
    }

    @Test
    void snapshot_withoutBlocks() {
        GameSnapshot original = new GameSnapshot(new BlockType[Board.ROWS][Board.COLS],
                new boolean[Board.ROWS][Board.COLS], null, null, 0, 1, 7, null, false);

        GameSnapshot restored = PayloadMapper.toGameSnapshot(PayloadMapper.toPayload(original));

        assertNull(restored.getCurrentBlock());
        assertNull(restored.getNextBlock());
        assertTrue(restored.getPendingAttack().isEmpty());
    }

    @Test
    void snapshot_nullIsNull() {
        assertNull(PayloadMapper.toPayload((GameSnapshot) null));
    }

    // ---------------------------------------------------------------- 결과

    @Test
    void result_roundTripsEveryKind() {
        BattleResult[] results = {
                BattleResult.win(PlayerId.PLAYER1, BattleEndReason.GAME_OVER),
                BattleResult.win(PlayerId.PLAYER2, BattleEndReason.TIME_UP),
                BattleResult.win(PlayerId.PLAYER2, BattleEndReason.PLAYER_LEFT),
                BattleResult.draw(BattleEndReason.GAME_OVER),
                BattleResult.draw(BattleEndReason.TIME_UP),
                BattleResult.connectionLost()};

        for (BattleResult result : results) {
            assertEquals(result, PayloadMapper.toBattleResult(PayloadMapper.toPayload(result)), result.toString());
        }
    }

    @Test
    void result_connectionLostIsNotDraw() {
        MessagePayload.Result payload = PayloadMapper.toPayload(BattleResult.connectionLost());

        assertNull(payload.winner());
        assertFalse(payload.draw());
        assertEquals("CONNECTION_LOST", payload.reason());
    }

    @Test
    void result_nullIsNull() {
        assertNull(PayloadMapper.toPayload((BattleResult) null));
    }

    @Test
    void result_rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleResult(new MessagePayload.Result("PLAYER3", false, "GAME_OVER")));
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleResult(new MessagePayload.Result("PLAYER1", false, "BOOM")));
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleResult(new MessagePayload.Result(null, false, "GAME_OVER")));
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleResult(new MessagePayload.Result(null, true, "PLAYER_LEFT")));
        assertThrows(IllegalArgumentException.class,
                () -> PayloadMapper.toBattleResult(new MessagePayload.Result("PLAYER1", false, "CONNECTION_LOST")));
    }

    private static void assertSnapshotEquals(GameSnapshot expected, GameSnapshot actual) {
        assertTrue(Arrays.deepEquals(expected.getBoard(), actual.getBoard()), "board");
        assertTrue(Arrays.deepEquals(expected.getAttackCells(), actual.getAttackCells()), "attackCells");
        assertBlockEquals(expected.getCurrentBlock(), actual.getCurrentBlock());
        assertBlockEquals(expected.getNextBlock(), actual.getNextBlock());
        assertEquals(expected.getScore(), actual.getScore());
        assertEquals(expected.getLevel(), actual.getLevel());
        assertEquals(expected.getRemainingSkips(), actual.getRemainingSkips());
        assertTrue(Arrays.deepEquals(expected.getPendingAttack().getRows(), actual.getPendingAttack().getRows()));
        assertEquals(expected.isGameOver(), actual.isGameOver());
    }

    private static void assertBlockEquals(Block expected, Block actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getRotation(), actual.getRotation());
        assertEquals(expected.getItemType(), actual.getItemType());
        Position[] expectedCells = expected.getCells();
        Position[] actualCells = actual.getCells();
        for (int i = 0; i < expectedCells.length; i++) {
            assertEquals(expectedCells[i].getRow(), actualCells[i].getRow());
            assertEquals(expectedCells[i].getCol(), actualCells[i].getCol());
        }
    }
}
