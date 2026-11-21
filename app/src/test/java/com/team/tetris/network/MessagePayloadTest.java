package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

/** MessagePayload: 값 검사, 배열 복사(외부 변경 차단), 블록 상태 복원 */
class MessagePayloadTest {

    @Test
    void config_validatesValues() {
        assertThrows(NullPointerException.class, () -> new MessagePayload.Config(null, Difficulty.EASY, 0));
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Config(" ", Difficulty.EASY, 0));
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Config("NORMAL", Difficulty.EASY, -1));
        assertEquals(180, new MessagePayload.Config("TIME_LIMIT", Difficulty.EASY, 180).timeLimitSeconds());
    }

    @Test
    void start_rejectsNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Start(-1));
    }

    @Test
    void gameOver_rejectsNegativeTime() {
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.GameOver(-1, 0));
    }

    @Test
    void result_rejectsDrawWithWinnerAndMissingReason() {
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Result("PLAYER1", true, "TIME_UP"));
        assertThrows(NullPointerException.class, () -> new MessagePayload.Result("PLAYER1", false, null));
    }

    @Test
    void attack_acceptsOneToTenRows() {
        assertEquals(1, new MessagePayload.Attack(NetworkTestData.attackRows(1, 0)).rowCount());
        assertEquals(10, new MessagePayload.Attack(NetworkTestData.attackRows(10, 0)).rowCount());
        assertThrows(IllegalArgumentException.class,
                () -> new MessagePayload.Attack(NetworkTestData.attackRows(0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new MessagePayload.Attack(NetworkTestData.attackRows(11, 0)));
    }

    @Test
    void attack_rejectsWrongRowWidthAndNullRows() {
        assertThrows(NullPointerException.class, () -> new MessagePayload.Attack(null));
        assertThrows(IllegalArgumentException.class,
                () -> new MessagePayload.Attack(new BlockType[][] {new BlockType[Board.COLS - 1]}));
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Attack(new BlockType[][] {null}));
    }

    @Test
    void attack_copiesInputAndOutput() {
        BlockType[][] rows = NetworkTestData.attackRows(2, 0);
        MessagePayload.Attack attack = new MessagePayload.Attack(rows);

        rows[0][1] = null;
        attack.rows()[0][2] = null;

        assertEquals(NetworkTestData.attackRows(2, 0)[0][1], attack.rows()[0][1]);
        assertEquals(NetworkTestData.attackRows(2, 0)[0][2], attack.rows()[0][2]);
    }

    @Test
    void attack_equalsByContent() {
        MessagePayload.Attack a = new MessagePayload.Attack(NetworkTestData.attackRows(2, 0));

        assertEquals(a, new MessagePayload.Attack(NetworkTestData.attackRows(2, 0)));
        assertEquals(a.hashCode(), new MessagePayload.Attack(NetworkTestData.attackRows(2, 0)).hashCode());
        assertNotEquals(a, new MessagePayload.Attack(NetworkTestData.attackRows(2, 1)));
        assertTrue(a.toString().contains("rows=2"));
    }

    @Test
    void snapshot_copiesArrays() {
        MessagePayload.Snapshot snapshot = NetworkTestData.snapshot(10);

        snapshot.board()[Board.ROWS - 1][0] = null;
        snapshot.attackCells()[Board.ROWS - 1][0] = false;
        snapshot.pendingAttack()[0][1] = null;

        assertEquals(BlockType.T, snapshot.board()[Board.ROWS - 1][0]);
        assertTrue(snapshot.attackCells()[Board.ROWS - 1][0]);
        assertNotNull(snapshot.pendingAttack()[0][1]);
    }

    @Test
    void snapshot_rejectsWrongBoardSize() {
        MessagePayload.Snapshot base = NetworkTestData.snapshot(0);
        BlockType[][] shortBoard = new BlockType[Board.ROWS - 1][Board.COLS];
        boolean[][] narrowCells = new boolean[Board.ROWS][Board.COLS - 1];

        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Snapshot(shortBoard,
                base.attackCells(), null, null, 0, 1, 0, base.pendingAttack(), false));
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Snapshot(base.board(),
                narrowCells, null, null, 0, 1, 0, base.pendingAttack(), false));
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Snapshot(base.board(),
                new boolean[1][Board.COLS], null, null, 0, 1, 0, base.pendingAttack(), false));
        assertThrows(IllegalArgumentException.class, () -> new MessagePayload.Snapshot(base.board(),
                base.attackCells(), null, null, 0, 1, 0, NetworkTestData.attackRows(11, 0), false));
    }

    @Test
    void snapshot_equalsByContent() {
        MessagePayload.Snapshot a = NetworkTestData.snapshot(10);

        assertEquals(a, NetworkTestData.snapshot(10));
        assertEquals(a.hashCode(), NetworkTestData.snapshot(10).hashCode());
        assertNotEquals(a, NetworkTestData.snapshot(11));
        assertTrue(a.toString().contains("score=10"));
    }

    @Test
    void blockState_restoresPositionAndRotation() {
        Block block = new Block(BlockType.T, new Position(6, 3));
        block.rotate();
        block.rotate();
        block.move(1, 2);

        MessagePayload.BlockState state = MessagePayload.BlockState.from(block);
        Block restored = state.toBlock();

        assertEquals(BlockType.T, restored.getType());
        assertEquals(7, restored.getPosition().getRow());
        assertEquals(5, restored.getPosition().getCol());
        assertEquals(2, restored.getRotation());
        assertCellsEqual(block.getCells(), restored.getCells());
    }

    @Test
    void blockState_restoresLineClearItemCell() {
        Block block = new Block(BlockType.L, new Position(5, 4), ItemType.LINE_CLEAR, 3);
        block.rotate();

        MessagePayload.BlockState state = MessagePayload.BlockState.from(block);
        Block restored = state.toBlock();

        assertEquals(3, state.itemCellIndex());
        assertEquals(ItemType.LINE_CLEAR, restored.getItemType());
        assertEquals(block.getItemPosition().getRow(), restored.getItemPosition().getRow());
        assertEquals(block.getItemPosition().getCol(), restored.getItemPosition().getCol());
    }

    @Test
    void blockState_restoresItemBlocks() {
        for (BlockType type : new BlockType[] {BlockType.WEIGHT, BlockType.BOMB, BlockType.CROSS, BlockType.GAP_DESTROY}) {
            Block restored = MessagePayload.BlockState.from(new Block(type, new Position(3, 3))).toBlock();
            assertEquals(type, restored.getType());
            assertTrue(restored.hasItem(), type.name());
        }
    }

    @Test
    void blockState_fromNullIsNull() {
        assertNull(MessagePayload.BlockState.from(null));
    }

    @Test
    void blockState_validatesValues() {
        assertThrows(NullPointerException.class, () -> new MessagePayload.BlockState(null, 0, 0, 0, ItemType.NONE, -1));
        assertThrows(NullPointerException.class, () -> new MessagePayload.BlockState(BlockType.I, 0, 0, 0, null, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new MessagePayload.BlockState(BlockType.I, 0, 0, 4, ItemType.NONE, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new MessagePayload.BlockState(BlockType.I, 0, 0, -1, ItemType.NONE, -1));
    }

    private static void assertCellsEqual(Position[] expected, Position[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].getRow(), actual[i].getRow());
            assertEquals(expected[i].getCol(), actual[i].getCol());
        }
    }
}
