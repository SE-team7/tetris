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

        // 초기 세로 I는 기준 열만 차지
        Block left = new Block(BlockType.I, new Position(10, 0));

        assertTrue(board.canPlace(left));
        assertFalse(board.canMove(left, 0, -1));

        // O는 기준 열과 오른쪽 열을 차지
        Block right = new Block(
                BlockType.O,
                new Position(10, Board.COLS - 2)
        );

        assertTrue(board.canPlace(right));
        assertFalse(board.canMove(right, 0, 1));

        Block bottom = new Block(
                BlockType.O,
                new Position(Board.ROWS - 1, 4)
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

        Block moving = new Block(
                BlockType.O,
                new Position(10, 2)
        );

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
                // 1차에서는 O의 변화 없는 회전도 충돌 검사상 허용
                assertTrue(
                        board.canRotate(block),
                        type + " rotation " + block.getRotation()
                                + " should be allowed in empty space"
                );

                block.rotate();
            }
        }
    }

    @Test
    void rejectsRotationOutsideRightWall() {
        Board board = new Board();

        // 현재 세로 I: (7,9), (8,9), (9,9), (10,9)
        // 회전 후 가로 I: (8,8), (8,9), (8,10), (8,11)
        Block block = new Block(
                BlockType.I,
                new Position(10, 9)
        );

        assertTrue(board.canPlace(block));
        assertFalse(board.canRotate(block));

        // 회전 가능 여부 확인은 실제 상태를 변경하지 않음
        assertEquals(0, block.getRotation());
        assertEquals(10, block.getPosition().getRow());
        assertEquals(9, block.getPosition().getCol());
    }

    @Test
    void rejectsRotationIntoLockedBlock() {
        Board board = new Board();

        // 고정 O: (8,6), (8,7), (9,6), (9,7)
        board.lockBlock(
                new Block(BlockType.O, new Position(9, 6))
        );

        // 현재 세로 I: (7,5), (8,5), (9,5), (10,5)
        // 회전 후 가로 I: (8,4), (8,5), (8,6), (8,7)
        Block block = new Block(
                BlockType.I,
                new Position(10, 5)
        );

        assertTrue(board.canPlace(block));
        assertFalse(board.canRotate(block));
        assertEquals(0, block.getRotation());
    }

    @Test
    void locksBlockInExpectedCells() {
        Board board = new Board();

        board.lockBlock(
                new Block(BlockType.O, new Position(10, 4))
        );

        BlockType[][] expected =
                new BlockType[Board.ROWS][Board.COLS];

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

        int bottomRow = Board.ROWS - 1;

        lockHorizontalI(board, bottomRow, 0);
        lockHorizontalI(board, bottomRow, 4);
        // 바닥의 8, 9열은 빈칸

        BlockType[][] before = board.getCells();

        assertEquals(0, board.clearLines());
        assertBoardEquals(before, board.getCells());
    }

    @Test
    void clearsOneLineAndMovesCellsAboveDown() {
        Board board = new Board();

        int bottomRow = Board.ROWS - 1;

        lockHorizontalI(board, bottomRow, 0);
        lockHorizontalI(board, bottomRow, 4);

        board.lockBlock(
                new Block(BlockType.O, new Position(bottomRow, 8))
        );

        // 바닥 행은 가득 참
        // 바로 위 행에는 O 두 칸이 남음
        assertEquals(1, board.clearLines());

        BlockType[][] expected =
                new BlockType[Board.ROWS][Board.COLS];

        expected[bottomRow][8] = BlockType.O;
        expected[bottomRow][9] = BlockType.O;

        assertBoardEquals(expected, board.getCells());
        assertEquals(0, board.clearLines());
    }

    @Test
    void clearsTwoAdjacentLinesAndMovesCellsAboveDown() {
        Board board = new Board();

        int bottomRow = Board.ROWS - 1;

        // O 다섯 개로 아래 두 행을 채움
        for (int col = 0; col < Board.COLS; col += 2) {
            board.lockBlock(
                    new Block(
                            BlockType.O,
                            new Position(bottomRow, col)
                    )
            );
        }

        // 삭제될 두 행 바로 위에 가로 I 배치
        lockHorizontalI(board, bottomRow - 2, 0);

        assertEquals(2, board.clearLines());

        BlockType[][] expected =
                new BlockType[Board.ROWS][Board.COLS];

        for (int col = 0; col < 4; col++) {
            expected[bottomRow][col] = BlockType.I;
        }

        assertBoardEquals(expected, board.getCells());
    }

    @Test
    void clearsFourAdjacentLines() {
        Board board = new Board();

        int bottomRow = Board.ROWS - 1;

        // 세로 I로 아래 네 행을 채움
        for (int col = 0; col < Board.COLS; col++) {
            board.lockBlock(
                    new Block(
                            BlockType.I,
                            new Position(bottomRow, col)
                    )
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
        /*
         * 새 I 회전축 기준:
         * 90도 회전 후 상대좌표는
         * (-2,-1), (-2,0), (-2,1), (-2,2).
         *
         * 지정한 row행의 col~col+3에 배치하려면
         * 기준 위치를 (row+2, col+1)로 설정.
         */
        Block block = new Block(
                BlockType.I,
                new Position(row + 2, col + 1)
        );

        block.rotate();
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