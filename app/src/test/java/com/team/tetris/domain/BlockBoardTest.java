package com.team.tetris.domain;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

public class BlockBoardTest {

    @Test
    void allBlocksRotateFourTimesAndReturnToOriginalCells() {
        for (BlockType type : BlockType.values()) {
            Block block = new Block(
                    type,
                    new Position(10, 5)
            );

            Position[] original = block.getCells();

            for (int i = 0; i < 4; i++) {
                block.rotate();
            }

            assertEquals(
                    0,
                    block.getRotation(),
                    type + " rotation should return to 0"
            );

            assertCellsEqual(
                    original,
                    block.getCells(),
                    type + " should return to original cells after 4 rotations"
            );
        }
    }

    @Test
    void getRotatedCellsDoesNotChangeBlockState() {
        for (BlockType type : BlockType.values()) {
            Block block = new Block(
                    type,
                    new Position(10, 5)
            );

            int originalRow = block.getPosition().getRow();
            int originalCol = block.getPosition().getCol();
            int originalRotation = block.getRotation();

            block.getRotatedCells();

            assertEquals(
                    originalRow,
                    block.getPosition().getRow(),
                    type + " row changed"
            );

            assertEquals(
                    originalCol,
                    block.getPosition().getCol(),
                    type + " col changed"
            );

            assertEquals(
                    originalRotation,
                    block.getRotation(),
                    type + " rotation changed"
            );
        }
    }

    @Test
    void rotatedCellsMatchActualRotateResult() {
        for (BlockType type : BlockType.values()) {
            Block block = new Block(
                    type,
                    new Position(10, 5)
            );

            Position[] expected = block.getRotatedCells();

            block.rotate();

            Position[] actual = block.getCells();

            assertCellsEqual(
                    expected,
                    actual,
                    type + " getRotatedCells and rotate result differ"
            );
        }
    }

    @Test
    void printAllBlockRotations() {
        for (BlockType type : BlockType.values()) {

            Block block = new Block(
                    type,
                    new Position(10, 5)
            );

            System.out.println();
            System.out.println("===== " + type + " =====");

            for (int i = 0; i < 4; i++) {

                System.out.println(
                        "rotation = " + block.getRotation()
                );

                printCells(block.getCells());

                block.rotate();
            }
        }
    }

    @Test
    void boardRejectsMoveOutsideLeftWall() {
        Board board = new Board();

        Block block = new Block(
                BlockType.I,
                new Position(4, 0)
        );

        assertFalse(
                board.canMove(block, 0, -1)
        );
    }

    @Test
    void boardRejectsMoveBelowBottom() {
        Board board = new Board();

        Block block = new Block(
                BlockType.O,
                new Position(Board.ROWS - 1, 4)
        );

        assertFalse(
                board.canMove(block, 1, 0)
        );
    }

    @Test
    void boardDetectsCollisionWithLockedBlock() {
        Board board = new Board();

        Block first = new Block(
                BlockType.O,
                new Position(10, 4)
        );

        board.lockBlock(first);

        Block second = new Block(
                BlockType.O,
                new Position(10, 4)
        );

        assertFalse(
                board.canPlace(second)
        );
    }

    private void assertCellsEqual(
            Position[] expected,
            Position[] actual,
            String message
    ) {
        Position[] expectedSorted = sortPositions(expected);
        Position[] actualSorted = sortPositions(actual);

        assertEquals(
                expectedSorted.length,
                actualSorted.length,
                message
        );

        for (int i = 0; i < expectedSorted.length; i++) {
            assertEquals(
                    expectedSorted[i].getRow(),
                    actualSorted[i].getRow(),
                    message + " row mismatch"
            );

            assertEquals(
                    expectedSorted[i].getCol(),
                    actualSorted[i].getCol(),
                    message + " col mismatch"
            );
        }
    }

    private Position[] sortPositions(Position[] positions) {
        Position[] copy = Arrays.copyOf(
                positions,
                positions.length
        );

        Arrays.sort(
                copy,
                Comparator
                        .comparingInt(Position::getRow)
                        .thenComparingInt(Position::getCol)
        );

        return copy;
    }

    private void printCells(Position[] cells) {
        Position[] sorted = sortPositions(cells);

        for (Position p : sorted) {
            System.out.println(
                    "("
                    + p.getRow()
                    + ", "
                    + p.getCol()
                    + ")"
            );
        }

        System.out.println();
    }
}