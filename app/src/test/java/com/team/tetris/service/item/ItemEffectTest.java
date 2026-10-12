package com.team.tetris.service.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

class ItemEffectTest {

    private final Board board = new Board();

    // Position의 equals() 구현 여부와 관계없이 좌표를 비교합니다.
    private static Set<String> coordinates(List<Position> cells) {
        Set<String> result = new HashSet<>();
        for (Position cell : cells) {
            result.add(cell.getRow() + "," + cell.getCol());
        }
        return result;
    }

    private static void assertCounts(
            ClearResult result, int scoreLines, int counterLines) {
        assertEquals(scoreLines, result.getScoreLineCount());
        assertEquals(counterLines, result.getItemCounterLineCount());
    }

    @Test
    void bombDeletesExactlyThreeByThreeArea() {
        Block block = new Block(BlockType.BOMB, new Position(10, 5));

        ClearResult result = new BombEffect().calculate(board, block);

        assertEquals(ItemType.BOMB, result.getItemType());
        assertEquals(9, result.getCellsToDelete().size());
        assertEquals(Set.of(
                "9,4", "9,5", "9,6",
                "10,4", "10,5", "10,6",
                "11,4", "11,5", "11,6"
        ), coordinates(result.getCellsToDelete()));
        assertTrue(result.getRowsToDelete().isEmpty());
        assertTrue(result.getColumnsToCompact().isEmpty());
        assertEquals(-1, result.getCompactStartRow());
        assertCounts(result, 0, 0);
    }

    @Test
    void bombClipsAreaAtTopLeftCorner() {
        Block block = new Block(BlockType.BOMB, new Position(0, 0));

        ClearResult result = new BombEffect().calculate(board, block);

        assertEquals(4, result.getCellsToDelete().size());
        assertEquals(Set.of(
                "0,0", "0,1", "1,0", "1,1"
        ), coordinates(result.getCellsToDelete()));
        assertCounts(result, 0, 0);
    }

    @Test
    void bombClipsAreaAtBottomRightCorner() {
        int row = Board.ROWS - 1;
        int col = Board.COLS - 1;
        Block block = new Block(BlockType.BOMB, new Position(row, col));

        ClearResult result = new BombEffect().calculate(board, block);

        assertEquals(4, result.getCellsToDelete().size());
        assertEquals(Set.of(
                (row - 1) + "," + (col - 1),
                (row - 1) + "," + col,
                row + "," + (col - 1),
                row + "," + col
        ), coordinates(result.getCellsToDelete()));
    }

    @Test
    void crossDeletesCenterRowAndVisibleColumn() {
        Block block = new Block(BlockType.CROSS, new Position(10, 5));

        ClearResult result = new CrossEffect().calculate(board, block);

        assertEquals(ItemType.CROSS, result.getItemType());
        assertEquals(List.of(10), result.getRowsToDelete());

        Set<String> expected = new HashSet<>();
        for (int row = 5; row < Board.ROWS; row++) {
            expected.add(row + ",5");
        }

        assertEquals(Board.ROWS - 5, result.getCellsToDelete().size());
        assertEquals(expected, coordinates(result.getCellsToDelete()));
        assertTrue(result.getColumnsToCompact().isEmpty());
        assertEquals(-1, result.getCompactStartRow());
        assertCounts(result, 1, 1);
    }

    @Test
    void crossDoesNothingInHiddenRows() {
        Block block = new Block(BlockType.CROSS, new Position(4, 5));

        ClearResult result = new CrossEffect().calculate(board, block);

        assertEquals(ItemType.CROSS, result.getItemType());
        assertTrue(result.isEmpty());
        assertCounts(result, 0, 0);
    }

    @Test
    void crossWorksAtFirstVisibleRow() {
        Block block = new Block(BlockType.CROSS, new Position(5, 0));

        ClearResult result = new CrossEffect().calculate(board, block);

        assertEquals(List.of(5), result.getRowsToDelete());
        assertEquals(Board.ROWS - 5, result.getCellsToDelete().size());
        assertTrue(result.getCellsToDelete().stream()
                .allMatch(cell ->
                        cell.getRow() >= 5 && cell.getCol() == 0));
        assertCounts(result, 1, 1);
    }

    @Test
    void crossDoesNothingOutsideBoard() {
        Position[] invalidPositions = {
                new Position(Board.ROWS, 5),
                new Position(10, -1),
                new Position(10, Board.COLS)
        };

        for (Position position : invalidPositions) {
            ClearResult result = new CrossEffect().calculate(
                    board, new Block(BlockType.CROSS, position));

            assertTrue(result.isEmpty());
            assertCounts(result, 0, 0);
        }
    }

    @Test
    void gapDestroySelectsTwoColumnsAndStartRow() {
        Block block = new Block(
                BlockType.GAP_DESTROY, new Position(10, 4));

        ClearResult result = new GapDestroyEffect().calculate(board, block);

        assertEquals(ItemType.GAP_DESTROY, result.getItemType());
        assertEquals(List.of(4, 5), result.getColumnsToCompact());
        assertEquals(10, result.getCompactStartRow());
        assertTrue(result.getRowsToDelete().isEmpty());
        assertTrue(result.getCellsToDelete().isEmpty());
        assertCounts(result, 0, 0);
    }

    @Test
    void gapDestroyWorksAtRightAndBottomBoundary() {
        Block block = new Block(
                BlockType.GAP_DESTROY,
                new Position(Board.ROWS - 1, Board.COLS - 2));

        ClearResult result = new GapDestroyEffect().calculate(board, block);

        assertEquals(
                List.of(Board.COLS - 2, Board.COLS - 1),
                result.getColumnsToCompact());
        assertEquals(Board.ROWS - 1, result.getCompactStartRow());
    }

    @Test
    void lineClearDeletesAttachedCellRowForEveryCellIndex() {
        // 초기 I 블록의 셀 행: 7, 8, 9, 10
        for (int index = 0; index < 4; index++) {
            Block block = new Block(
                    BlockType.I, new Position(10, 5),
                    ItemType.LINE_CLEAR, index);

            ClearResult result =
                    new LineClearEffect().calculate(board, block);

            assertEquals(ItemType.LINE_CLEAR, result.getItemType());
            assertEquals(List.of(7 + index), result.getRowsToDelete());
            assertTrue(result.getCellsToDelete().isEmpty());
            assertTrue(result.getColumnsToCompact().isEmpty());
            assertEquals(-1, result.getCompactStartRow());
            assertCounts(result, 1, 1);
        }
    }

    @Test
    void lineClearUsesAttachedCellPositionAfterRotationAndMovement() {
        Block block = new Block(
                BlockType.T, new Position(10, 5),
                ItemType.LINE_CLEAR, 0);

        block.rotate();
        block.move(2, -1);

        ClearResult result = new LineClearEffect().calculate(board, block);

        //T 블록의 0번 셀은 회전·이동 후 (10, 4)
        assertEquals(List.of(10), result.getRowsToDelete());
        assertCounts(result, 1, 1);
    }

    @Test
    void lineClearRejectsBlockWithoutItem() {
        Block block = new Block(BlockType.T, new Position(10, 5));

        assertThrows(IllegalArgumentException.class,
                () -> new LineClearEffect().calculate(board, block));
    }

    @Test
    void lineClearRejectsAttachedCellOutsideBoard() {
        Block block = new Block(
                BlockType.I, new Position(2, 5),
                ItemType.LINE_CLEAR, 0);

        // 부착 셀은 (-1, 5)
        assertThrows(IllegalArgumentException.class,
                () -> new LineClearEffect().calculate(board, block));
    }

    @Test
    void weightCalculatesAllSixMovedCells() {
        Block block = new Block(BlockType.WEIGHT, new Position(10, 4));

        ClearResult result = new WeightEffect().calculate(board, block);

        assertEquals(ItemType.WEIGHT, result.getItemType());
        assertEquals(6, result.getCellsToDelete().size());
        assertEquals(Set.of(
                "11,3", "11,4", "11,5", "11,6",
                "10,4", "10,5"
        ), coordinates(result.getCellsToDelete()));
        assertTrue(result.getRowsToDelete().isEmpty());
        assertTrue(result.getColumnsToCompact().isEmpty());
        assertEquals(-1, result.getCompactStartRow());
        assertCounts(result, 0, 0);

        // 계산만 수행하며 블록을 실제로 이동시키지 않음
        assertEquals(10, block.getPosition().getRow());
        assertEquals(4, block.getPosition().getCol());
    }

    @Test
    void weightExcludesMovedCellsBelowBoard() {
        Block block = new Block(
                BlockType.WEIGHT, new Position(Board.ROWS - 1, 4));

        ClearResult result = new WeightEffect().calculate(board, block);

        assertEquals(2, result.getCellsToDelete().size());
        assertEquals(Set.of(
                (Board.ROWS - 1) + ",4",
                (Board.ROWS - 1) + ",5"
        ), coordinates(result.getCellsToDelete()));
        assertCounts(result, 0, 0);
    }
}
