package com.team.tetris.domain;

public class Board {
    public static final int ROWS = 25;
    public static final int COLS = 10;

    private final BlockType[][] cells;

    public Board() {
        cells = new BlockType[ROWS][COLS];
    }
    //이동가능 확인
    public boolean canMove(Block block, int deltaRow, int deltaCol) {
        return canPlace(block.getMovedCells(deltaRow, deltaCol));
    }
    //회전 가능 확인
    public boolean canRotate(Block block) {
        return canPlace(block.getRotatedCells());
    }
    //현재 위치에 블록 배치 가능한지 확인
    public boolean canPlace(Block block) {
        return canPlace(block.getCells());
    }
    //보드에 블록 고정
    public void lockBlock(Block block) {
        Position[] blockCells = block.getCells();

        if (!canPlace(blockCells)) {
            throw new IllegalArgumentException("block cannot be locked here");
        }

        for (Position cell : blockCells) {
            int row = cell.getRow();
            int col = cell.getCol();

            cells[row][col] = block.getType();
        }
    }
    //줄 삭제 +삭제한 줄 개수 반환
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
    //고정된 블록 상태 반환
    public BlockType[][] getCells() {
        BlockType[][] copy = new BlockType[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(cells[row], 0, copy[row], 0, COLS);
        }

        return copy;
    }
    //보드 초기화
    public void reset() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                cells[row][col] = null;
            }
        }
    }
    // 좌표 배열이 현재 보드에 배치 가능한지 확인
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
    // 보드 범위 안인지 확인
    private boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }
    // 해당 줄이 꽉 찼는지 확인
    private boolean isLineFull(int row) {
        for (int col = 0; col < COLS; col++) {
            if (cells[row][col] == null) {
                return false;
            }
        }
        return true;
    }
    //한 줄 삭제 후 위쪽 줄을 한 칸씩 아래로 이동
    private void clearLine(int targetRow) {
        for (int row = targetRow; row > 0; row--) {
            System.arraycopy(cells[row - 1], 0, cells[row], 0, COLS);
        }

        for (int col = 0; col < COLS; col++) {
            cells[0][col] = null;
        }
    }
}
