package com.team.tetris.domain;

import java.util.ArrayList;
import java.util.List;

import com.team.tetris.service.item.BombEffect;
import com.team.tetris.service.item.CrossEffect;
import com.team.tetris.service.item.GapDestroyEffect;
import com.team.tetris.service.item.LineClearEffect;
import com.team.tetris.service.item.WeightEffect;

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
        return block.canRotate() && canPlace(block.getRotatedCells());
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
        ClearResult result = findFullLines();
        applyClear(result);
        return result.getScoreLineCount();
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
    public boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    //꽉 찬 행의 삭제 정보 계산 (보드 변경 없)
    public ClearResult findFullLines() {
        List<Integer> rowsToDelete = new ArrayList<>();

        for (int row = 0; row < ROWS; row++) {
            if (isLineFull(row)) {
                rowsToDelete.add(row);
            }
        }

        int lineCount = rowsToDelete.size();

        return new ClearResult(
                ItemType.NONE,
                rowsToDelete,
                List.of(),
                List.of(),
                -1,
                lineCount,
                lineCount
        );
    }

    //아이템 효과 계산 (보드 변경 없)
    public ClearResult findItemEffect(Block block) {
        return switch (block.getItemType()) {
            case NONE -> ClearResult.empty(ItemType.NONE);
            case WEIGHT -> new WeightEffect().calculate(this, block);
            case LINE_CLEAR -> new LineClearEffect().calculate(this, block);
            case BOMB -> new BombEffect().calculate(this, block);
            case CROSS -> new CrossEffect().calculate(this, block);
            case GAP_DESTROY -> new GapDestroyEffect().calculate(this, block);
        };
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

    public void applyClear(ClearResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        if (result.isEmpty()) {
            return;
        }

        // 1. 효과 적용 전 좌표를 기준으로 개별 셀 삭제
        for (Position cell : result.getCellsToDelete()) {
            cells[cell.getRow()][cell.getCol()] = null;
        }

        // 2. 지정된 열의 아래쪽 구간 압축
        for (int col : result.getColumnsToCompact()) {
            compactColumn(col, result.getCompactStartRow());
        }

        // 3. 지정된 행 삭제 및 위쪽 행 내려오기
        deleteRows(result.getRowsToDelete());
    }

    private void compactColumn(int col, int startRow) {
        int writeRow = ROWS - 1;

        // 아래부터 읽어 내려 쌓으므로 기존 블록의 위아래 순서를 유지
        for (int readRow = ROWS - 1; readRow >= startRow; readRow--) {
            if (cells[readRow][col] != null) {
                BlockType type = cells[readRow][col];

                cells[readRow][col] = null;
                cells[writeRow][col] = type;
                writeRow--;
            }
        }

        // 압축 후 위에 남은 구간 비우기
        for (int row = writeRow; row >= startRow; row--) {
            cells[row][col] = null;
        }
    }

    private void deleteRows(List<Integer> rowsToDelete) {
        if (rowsToDelete.isEmpty()) {
            return;
        }

        boolean[] deleted = new boolean[ROWS];

        for (int row : rowsToDelete) {
            deleted[row] = true;
        }

        int writeRow = ROWS - 1;

        for (int readRow = ROWS - 1; readRow >= 0; readRow--) {
            if (deleted[readRow]) {
                continue;
            }

            if (writeRow != readRow) {
                System.arraycopy(
                        cells[readRow], 0,
                        cells[writeRow], 0,
                        COLS
                );
            }

            writeRow--;
        }

        for (int row = writeRow; row >= 0; row--) {
            for (int col = 0; col < COLS; col++) {
                cells[row][col] = null;
            }
        }
    }

}
