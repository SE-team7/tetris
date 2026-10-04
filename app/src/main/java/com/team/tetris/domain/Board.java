package com.team.tetris.domain;

public class Board {
    public static final int HIDDEN_ROWS = 5;
    public static final int VISIBLE_ROWS = 20;
    public static final int ROWS = HIDDEN_ROWS + VISIBLE_ROWS;
    public static final int COLS = 10;

    private final BlockType[][] cells;

    public Board() {
        cells = new BlockType[ROWS][COLS];
    }

    public boolean canMove(Block block, int deltaRow, int deltaCol) {
        Position[] movedCells = new Position[block.getCells().length];
        Position[] currentCells = block.getCells();

        for (int i = 0; i < currentCells.length; i++) {
            movedCells[i] = new Position(
                    currentCells[i].getRow() + deltaRow,
                    currentCells[i].getCol() + deltaCol
            );
        }

        return canPlace(movedCells);
    }

    public boolean canRotate(Block block) {
        Block rotatedBlock = new Block(block.getType(), block.getPosition());

        for (int i = 0; i <= block.getRotation(); i++) {
            rotatedBlock.rotate();
        }

        return canPlace(rotatedBlock);
    }

    public boolean canPlace(Block block) {
        return canPlace(block.getCells());
    }

    public void lockBlock(Block block) {
        for (Position cell : block.getCells()) {
            int row = cell.getRow();
            int col = cell.getCol();

            if (!isInside(row, col)) {
                throw new IllegalArgumentException("block is outside of board");
            }
            cells[row][col] = block.getType();
        }
    }

    public int clearLines() {
        int clearedLines = 0;

        for (int row = ROWS - 1; row >= 0; row--) {
            if (isLineFull(row)) {
                clearLine(row);
                clearedLines++;
                row++;
            }
        }

        return clearedLines;
    }

    public BlockType[][] getCells() {
        BlockType[][] copy = new BlockType[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(cells[row], 0, copy[row], 0, COLS);
        }

        return copy;
    }

    public void reset() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                cells[row][col] = null;
            }
        }
    }

    private boolean canPlace(Position[] positions) {
        for (Position position : positions) {
            int row = position.getRow();
            int col = position.getCol();

            if (!isInside(row, col) || cells[row][col] != null) {
                return false;
            }
        }

        return true;
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    private boolean isLineFull(int row) {
        for (int col = 0; col < COLS; col++) {
            if (cells[row][col] == null) {
                return false;
            }
        }
        return true;
    }

    private void clearLine(int targetRow) {
        for (int row = targetRow; row > 0; row--) {
            System.arraycopy(cells[row - 1], 0, cells[row], 0, COLS);
        }

        for (int col = 0; col < COLS; col++) {
            cells[0][col] = null;
        }
    }
}
