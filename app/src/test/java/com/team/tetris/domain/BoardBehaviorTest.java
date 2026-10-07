package com.team.tetris.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class BoardBehaviorTest {

    @Test
    void allowsMovementInEmptySpace() {
        Board board = new Board();
        Block block = new Block(BlockType.T, new Position(10, 5));

        assertTrue(board.canMove(block, 0, -1));
        assertTrue(board.canMove(block, 0, 1));
        assertTrue(board.canMove(block, 1, 0));
    }

    @Test
    void rejectsMovementOutsideWallsAndBottom() {
        Board board = new Board();

        // I의 초기 모양은 세로이므로 col=0이 왼쪽 끝
        Block left = new Block(BlockType.I, new Position(10, 0));
        assertTrue(board.canPlace(left));
        assertFalse(board.canMove(left, 0, -1));

        // O는 기준 col과 col+1을 차지
        Block right = new Block(
                BlockType.O, new Position(10, Board.COLS - 2)
        );
        assertTrue(board.canPlace(right));
        assertFalse(board.canMove(right, 0, 1));

        Block bottom = new Block(
                BlockType.O, new Position(Board.ROWS - 1, 4)
        );
        assertTrue(board.canPlace(bottom));
        assertFalse(board.canMove(bottom, 1, 0));
    }

    @Test
    void rejectsMovementIntoLockedBlock() {
        Board board = new Board();

        board.lockBlock(
                new Block(BlockType.O, new Position(10, 4))
        );

        Block moving = new Block(BlockType.O, new Position(10, 2));

        assertTrue(board.canPlace(moving));
        assertFalse(board.canMove(moving, 0, 1));
        assertTrue(board.canMove(moving, 0, -1));
    }

    @Test
    void allowsRotationInEmptySpace() {
        Board board = new Board();

        for (BlockType type : BlockType.values()) {
            Block block = new Block(type, new Position(10, 5));

            for (int i = 0; i < 4; i++) {
                assertTrue(board.canRotate(block), type + " 회전 가능");
                block.rotate();
            }
        }
    }

    @Test
    void rejectsRotationOutsideRightWall() {
        Board board = new Board();

        // 현재 세로 I는 보드 안에 있지만,
        // 회전하면 col=8,9,10,11을 차지
        Block block = new Block(BlockType.I, new Position(10, 8));

        assertTrue(board.canPlace(block));
        assertFalse(board.canRotate(block));

        // 가능 여부 확인만으로 상태가 바뀌면 안 됨
        assertEquals(0, block.getRotation());
    }

    @Test
    void rejectsRotationIntoLockedBlock() {
        Board board = new Board();

        // 고정된 O: (9,6), (9,7), (10,6), (10,7)
        board.lockBlock(
                new Block(BlockType.O, new Position(10, 6))
        );

        // 현재 I: (7,5), (8,5), (9,5), (10,5)
        // 회전 후: (10,5), (10,6), (10,7), (10,8)
        Block block = new Block(BlockType.I, new Position(10, 5));

        assertTrue(board.canPlace(block));
        assertFalse(board.canRotate(block));
    }

    @Test
    void locksBlockInExpectedCells() {
        Board board = new Board();

        board.lockBlock(
                new Block(BlockType.O, new Position(10, 4))
        );

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[9][4] = BlockType.O;
        expected[9][5] = BlockType.O;
        expected[10][4] = BlockType.O;
        expected[10][5] = BlockType.O;

        assertBoardEquals(expected, board.getCells());
    }

    @Test
    void rejectsInvalidLockWithoutChangingBoard() {
        Board board = new Board();

        board.lockBlock(
                new Block(BlockType.O, new Position(10, 4))
        );

        BlockType[][] before = board.getCells();

        assertThrows(
                IllegalArgumentException.class,
                () -> board.lockBlock(
                        new Block(BlockType.O, new Position(10, 4))
                )
        );

        assertBoardEquals(before, board.getCells());
    }

    @Test
    void doesNotClearIncompleteLine() {
        Board board = new Board();

        lockHorizontalI(board, 24, 0);
        lockHorizontalI(board, 24, 4);
        // 바닥의 col=8,9는 빈칸

        BlockType[][] before = board.getCells();

        assertEquals(0, board.clearLines());
        assertBoardEquals(before, board.getCells());
    }

    @Test
    void clearsOneLineAndMovesCellsAboveDown() {
        Board board = new Board();

        lockHorizontalI(board, 24, 0);
        lockHorizontalI(board, 24, 4);
        board.lockBlock(
                new Block(BlockType.O, new Position(24, 8))
        );
        // 24행은 가득 참, 23행에는 O 두 칸

        assertEquals(1, board.clearLines());

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[24][8] = BlockType.O;
        expected[24][9] = BlockType.O;

        // 남은 O 두 칸이 23행에서 24행으로 내려와야 함
        assertBoardEquals(expected, board.getCells());
        assertEquals(0, board.clearLines());
    }

    @Test
    void clearsTwoAdjacentLinesAndMovesCellsAboveDown() {
        Board board = new Board();

        // O 다섯 개로 23행과 24행을 모두 채움
        for (int col = 0; col < Board.COLS; col += 2) {
            board.lockBlock(
                    new Block(BlockType.O, new Position(24, col))
            );
        }

        lockHorizontalI(board, 22, 0);

        assertEquals(2, board.clearLines());

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        for (int col = 0; col < 4; col++) {
            expected[24][col] = BlockType.I;
        }

        assertBoardEquals(expected, board.getCells());
    }

    @Test
    void clearsFourAdjacentLines() {
        Board board = new Board();

        for (int col = 0; col < Board.COLS; col++) {
            board.lockBlock(
                    new Block(BlockType.I, new Position(24, col))
            );
        }

        assertEquals(4, board.clearLines());
        assertBoardEquals(
                new BlockType[Board.ROWS][Board.COLS],
                board.getCells()
        );
    }

    @Test
    void resetRemovesAllLockedCells() {
        Board board = new Board();

        board.lockBlock(
                new Block(BlockType.O, new Position(10, 4))
        );

        board.reset();

        assertBoardEquals(
                new BlockType[Board.ROWS][Board.COLS],
                board.getCells()
        );
    }

    @Test
    void changingReturnedArrayDoesNotChangeBoard() {
        Board board = new Board();

        board.lockBlock(
                new Block(BlockType.O, new Position(10, 4))
        );

        BlockType[][] returned = board.getCells();
        returned[9][4] = null;
        returned[0][0] = BlockType.I;

        BlockType[][] actual = board.getCells();

        assertEquals(BlockType.O, actual[9][4]);
        assertNull(actual[0][0]);
    }

    private void lockHorizontalI(Board board, int row, int col) {
        Block block = new Block(BlockType.I, new Position(row, col));
        block.rotate(); // 가로 I
        board.lockBlock(block);
    }

    private void assertBoardEquals(
            BlockType[][] expected,
            BlockType[][] actual
    ) {
        assertEquals(expected.length, actual.length);

        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(
                    expected[row],
                    actual[row],
                    row + "행의 보드 상태가 다릅니다."
            );
        }
    }
}