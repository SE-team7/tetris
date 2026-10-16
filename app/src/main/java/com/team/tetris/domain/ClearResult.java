package com.team.tetris.domain;
//아이템모드에서 사용하는 삭제, 압축 정보 저장 클래스

import java.util.List;
import java.util.Objects;

public final class ClearResult {
    private final ItemType itemType;
    private final List<Integer> rowsToDelete;
    private final List<Position> cellsToDelete;
    private final List<Integer> columnsToCompact;
    private final int compactStartRow;
    private final int scoreLineCount;
    private final int itemCounterLineCount;

    public ClearResult(
            ItemType itemType,
            List<Integer> rowsToDelete,
            List<Position> cellsToDelete,
            List<Integer> columnsToCompact,
            int compactStartRow,
            int scoreLineCount,
            int itemCounterLineCount
    ) {
        this.itemType = Objects.requireNonNull(itemType);
        this.rowsToDelete = List.copyOf(rowsToDelete);
        this.cellsToDelete = List.copyOf(cellsToDelete);
        this.columnsToCompact = List.copyOf(columnsToCompact);

        if (scoreLineCount < 0 || itemCounterLineCount < 0) {
            throw new IllegalArgumentException("line counts must not be negative");
        }

        for (int row : this.rowsToDelete) {
            if (row < 0 || row >= Board.ROWS) {
                throw new IllegalArgumentException("invalid row: " + row);
            }
        }

        for (Position cell : this.cellsToDelete) {
            if (cell.getRow() < 0 || cell.getRow() >= Board.ROWS
                    || cell.getCol() < 0 || cell.getCol() >= Board.COLS) {
                throw new IllegalArgumentException("invalid cell position");
            }
        }

        for (int col : this.columnsToCompact) {
            if (col < 0 || col >= Board.COLS) {
                throw new IllegalArgumentException("invalid column: " + col);
            }
        }

        if (this.columnsToCompact.isEmpty()) {
            if (compactStartRow != -1) {
                throw new IllegalArgumentException(
                        "compactStartRow must be -1 when no columns are compacted"
                );
            }
        } else if (compactStartRow < 0 || compactStartRow > Board.ROWS) {
            throw new IllegalArgumentException("invalid compactStartRow");
        }

        this.compactStartRow = compactStartRow;
        this.scoreLineCount = scoreLineCount;
        this.itemCounterLineCount = itemCounterLineCount;
    }

    public static ClearResult empty(ItemType itemType) {
        return new ClearResult(
                itemType,
                List.of(),
                List.of(),
                List.of(),
                -1,
                0,
                0
        );
    }

    public boolean isEmpty() {
        return rowsToDelete.isEmpty()
                && cellsToDelete.isEmpty()
                && columnsToCompact.isEmpty();
    }

    public ItemType getItemType() {
        return itemType;
    }

    public List<Integer> getRowsToDelete() {
        return rowsToDelete;
    }

    public List<Position> getCellsToDelete() {
        return cellsToDelete;
    }

    public List<Integer> getColumnsToCompact() {
        return columnsToCompact;
    }

    public int getCompactStartRow() {
        return compactStartRow;
    }

    public int getScoreLineCount() {
        return scoreLineCount;
    }

    public int getItemCounterLineCount() {
        return itemCounterLineCount;
    }
}