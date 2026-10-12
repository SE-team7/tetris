package com.team.tetris.domain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class BoardItemApplyTest {

    // BOMB와 CROSS는 한 칸이므로 원하는 위치에 테스트 데이터를 배치할 수 있습니다.
    private void putCell(Board board, int row, int col, BlockType type) {
        board.lockBlock(new Block(type, new Position(row, col)));
    }

    private void assertBoardEquals(BlockType[][] expected, Board board) {
        BlockType[][] actual = board.getCells();
        for (int row = 0; row < Board.ROWS; row++) {
            assertArrayEquals(expected[row], actual[row], "row=" + row);
        }
    }

    @Test
    void bombDeletesAreaAndItselfWithoutMovingOtherCells() {
        Board board = new Board();

        for (int row = 9; row <= 11; row++) {
            for (int col = 4; col <= 6; col++) {
                putCell(board, row, col, BlockType.BOMB);
            }
        }

        putCell(board, 8, 5, BlockType.CROSS);
        putCell(board, 12, 5, BlockType.CROSS);
        putCell(board, 10, 3, BlockType.CROSS);
        putCell(board, 10, 7, BlockType.CROSS);

        Block bomb = new Block(BlockType.BOMB, new Position(10, 5));
        board.applyClear(board.findItemEffect(bomb));

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[8][5] = BlockType.CROSS;
        expected[12][5] = BlockType.CROSS;
        expected[10][3] = BlockType.CROSS;
        expected[10][7] = BlockType.CROSS;

        assertBoardEquals(expected, board);
    }

    @Test
    void crossDeletesRowAndColumnThenMovesUpperRowsDown() {
        Board board = new Board();

        for (int row = 5; row < Board.ROWS; row++) {
            putCell(board, row, 5, BlockType.CROSS);
        }
        for (int col = 0; col < Board.COLS; col++) {
            if (col != 5) {
                putCell(board, 10, col, BlockType.BOMB);
            }
        }

        // 숨김 영역의 셀은 열 삭제 대상에서 제외되지만, 행 삭제로 한 행 내려옴
        putCell(board, 3, 5, BlockType.BOMB);
        putCell(board, 9, 2, BlockType.BOMB);
        putCell(board, 11, 2, BlockType.BOMB);

        Block cross = new Block(BlockType.CROSS, new Position(10, 5));
        board.applyClear(board.findItemEffect(cross));

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[4][5] = BlockType.BOMB;
        expected[10][2] = BlockType.BOMB;
        expected[11][2] = BlockType.BOMB;

        assertBoardEquals(expected, board);
    }

    @Test
    void lineClearDeletesNonFullRowAndMovesUpperRowsDown() {
        Board board = new Board();

        Block block = new Block(
                BlockType.I, new Position(10, 5),
                ItemType.LINE_CLEAR, 3);
        board.lockBlock(block);

        putCell(board, 10, 2, BlockType.BOMB);
        putCell(board, 6, 1, BlockType.CROSS);
        putCell(board, 11, 2, BlockType.CROSS);

        assertEquals(0, board.findFullLines().getScoreLineCount());

        ClearResult result = board.findItemEffect(block);
        board.applyClear(result);

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[8][5] = BlockType.I;
        expected[9][5] = BlockType.I;
        expected[10][5] = BlockType.I;
        expected[7][1] = BlockType.CROSS;
        expected[11][2] = BlockType.CROSS;

        assertBoardEquals(expected, board);
        assertEquals(1, result.getScoreLineCount());
        assertEquals(1, result.getItemCounterLineCount());
    }

    @Test
    void gapDestroyCompactsTwoColumnsKeepingOrderAndBlock() {
        Board board = new Board();

        Block gap = new Block(
                BlockType.GAP_DESTROY, new Position(10, 4));
        board.lockBlock(gap);

        putCell(board, 14, 4, BlockType.BOMB);
        putCell(board, 20, 4, BlockType.CROSS);
        putCell(board, 18, 5, BlockType.BOMB);

        // 압축 시작 행 위와 다른 열은 그대로 유지
        putCell(board, 9, 4, BlockType.CROSS);
        putCell(board, 9, 5, BlockType.BOMB);
        putCell(board, 14, 6, BlockType.CROSS);

        board.applyClear(board.findItemEffect(gap));

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[24][4] = BlockType.CROSS;
        expected[23][4] = BlockType.BOMB;
        expected[22][4] = BlockType.GAP_DESTROY;

        expected[24][5] = BlockType.BOMB;
        expected[23][5] = BlockType.GAP_DESTROY;

        expected[9][4] = BlockType.CROSS;
        expected[9][5] = BlockType.BOMB;
        expected[14][6] = BlockType.CROSS;

        assertBoardEquals(expected, board);
    }

    @Test
    void compactionDoesNotChangeAlreadyPackedColumn() {
        Board board = new Board();
        putCell(board, 22, 4, BlockType.BOMB);
        putCell(board, 23, 4, BlockType.CROSS);
        putCell(board, 24, 4, BlockType.BOMB);

        BlockType[][] before = board.getCells();

        board.applyClear(new ClearResult(
                ItemType.GAP_DESTROY,
                List.of(), List.of(), List.of(4),
                10, 0, 0));

        assertBoardEquals(before, board);
    }

    @Test
    void compactionHandlesEmptyColumn() {
        Board board = new Board();
        putCell(board, 9, 4, BlockType.BOMB);
        putCell(board, 20, 6, BlockType.CROSS);

        BlockType[][] before = board.getCells();

        board.applyClear(new ClearResult(
                ItemType.GAP_DESTROY,
                List.of(), List.of(), List.of(4),
                10, 0, 0));

        assertBoardEquals(before, board);
    }

    @Test
    void gapDestroyCanCreateFullLineThenNormalClearRemovesIt() {
        Board board = new Board();

        for (int col = 0; col < Board.COLS; col++) {
            if (col != 4 && col != 5) {
                putCell(board, 24, col, BlockType.BOMB);
            }
        }

        Block gap = new Block(
                BlockType.GAP_DESTROY, new Position(10, 4));
        board.lockBlock(gap);

        board.applyClear(board.findItemEffect(gap));

        assertEquals(BlockType.GAP_DESTROY, board.getCells()[24][4]);
        assertEquals(BlockType.GAP_DESTROY, board.getCells()[24][5]);
        assertEquals(List.of(24), board.findFullLines().getRowsToDelete());

        assertEquals(1, board.clearLines());
        assertBoardEquals(
                new BlockType[Board.ROWS][Board.COLS], board);
    }

    @Test
    void weightAppliesCurrentMovedCellDeletion() {
        Board board = new Board();
        Block weight = new Block(BlockType.WEIGHT, new Position(10, 4));

        for (Position cell : weight.getMovedCells(1, 0)) {
            putCell(board, cell.getRow(), cell.getCol(), BlockType.BOMB);
        }

        // 현재 효과의 범위 밖에 있는 칸
        putCell(board, 12, 4, BlockType.CROSS);
        putCell(board, 11, 2, BlockType.CROSS);

        ClearResult result = board.findItemEffect(weight);
        board.applyClear(result);

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[12][4] = BlockType.CROSS;
        expected[11][2] = BlockType.CROSS;

        assertBoardEquals(expected, board);
        assertEquals(0, result.getScoreLineCount());
        assertEquals(0, result.getItemCounterLineCount());
    }

    @Test
    void deletesMultipleRowsUsingOriginalCoordinates() {
        Board board = new Board();
        putCell(board, 18, 1, BlockType.BOMB);
        putCell(board, 20, 2, BlockType.CROSS);
        putCell(board, 22, 3, BlockType.BOMB);
        putCell(board, 23, 4, BlockType.CROSS);
        putCell(board, 24, 5, BlockType.BOMB);

        board.applyClear(new ClearResult(
                ItemType.NONE,
                List.of(20, 23), List.of(), List.of(),
                -1, 2, 2));

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[20][1] = BlockType.BOMB;
        expected[23][3] = BlockType.BOMB;
        expected[24][5] = BlockType.BOMB;

        assertBoardEquals(expected, board);
    }

    @Test
    void deletingTopRowDoesNotMoveLowerRows() {
        Board board = new Board();
        putCell(board, 0, 1, BlockType.BOMB);
        putCell(board, 1, 2, BlockType.CROSS);
        putCell(board, 24, 3, BlockType.BOMB);

        board.applyClear(new ClearResult(
                ItemType.LINE_CLEAR,
                List.of(0), List.of(), List.of(),
                -1, 1, 1));

        BlockType[][] expected = new BlockType[Board.ROWS][Board.COLS];
        expected[1][2] = BlockType.CROSS;
        expected[24][3] = BlockType.BOMB;

        assertBoardEquals(expected, board);
    }

    @Test
    void emptyResultLeavesBoardUnchanged() {
        Board board = new Board();
        putCell(board, 10, 5, BlockType.BOMB);
        BlockType[][] before = board.getCells();

        board.applyClear(ClearResult.empty(ItemType.NONE));

        assertBoardEquals(before, board);
    }

    @Test
    void nullResultIsRejectedWithoutChangingBoard() {
        Board board = new Board();
        putCell(board, 10, 5, BlockType.BOMB);
        BlockType[][] before = board.getCells();

        assertThrows(IllegalArgumentException.class,
                () -> board.applyClear(null));

        assertBoardEquals(before, board);
    }
}
