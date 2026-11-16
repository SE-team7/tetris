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
    private final boolean[][] attackCells;

    public Board() {
        cells = new BlockType[ROWS][COLS];
        attackCells = new boolean[ROWS][COLS];
    }
    //이동가능 확인
    public boolean canMove(Block block, int deltaRow, int deltaCol) {
        return canPlace(block.getMovedCells(deltaRow, deltaCol));
    }
    
    public boolean canRotate(Block block) {
        return canRotate(block, 0, 0);
    }
    //회전 가능 확인
    public boolean canRotate(Block block, int deltaRow, int deltaCol) {
        return block.canRotate() && canPlace(block.getRotatedCells(deltaRow, deltaCol));
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
            attackCells[row][col] = false;
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
                attackCells[row][col] = false;
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

    // 폭탄 삭제 칸을 제거하고 같은 열의 위쪽 셀을 내려 채움
    private void deleteBombCellsAndDrop(List<Position> cellsToDelete) {
        boolean[][] deleted = new boolean[ROWS][COLS];
        boolean[] affectedColumns = new boolean[COLS];

        // 이동 전에 삭제 대상 좌표를 모두 표시
        for (Position cell : cellsToDelete) {
            int row = cell.getRow();
            int col = cell.getCol();

            deleted[row][col] = true;
            affectedColumns[col] = true;
        }

        for (int col = 0; col < COLS; col++) {
            if (!affectedColumns[col]) {
                continue;
            }

            int writeRow = ROWS - 1;

            for (int readRow = ROWS - 1; readRow >= 0; readRow--) {
                if (deleted[readRow][col]) {
                    continue;
                }

                // 빈칸도 함께 옮겨 기존 빈칸 구조 유지
                if (writeRow != readRow) {
                    cells[writeRow][col] = cells[readRow][col];
                    attackCells[writeRow][col] = attackCells[readRow][col];
                }

                writeRow--;
            }

            // 내려온 만큼 위쪽 비우기
            for (int row = writeRow; row >= 0; row--) {
                cells[row][col] = null;
                attackCells[row][col] = false;
            }
        }
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

    // 반드시 applyClear() 전에 호출
    public AttackRows createAttackRows(
            ClearResult result,
            Position[] lastLockedCells
    ) {
        if (result == null || lastLockedCells == null) {
            throw new IllegalArgumentException(
                    "result and lastLockedCells must not be null"
            );
        }

        List<Integer> rowIndexes = result.getAttackRowIndexes();

        if (rowIndexes.size() < 2) {
            return AttackRows.empty();
        }

        boolean[][] excluded = new boolean[ROWS][COLS];

        // 마지막으로 고정한 블록의 칸은 공격 행에서 비우기
        for (Position cell : lastLockedCells) {
            int row = cell.getRow();
            int col = cell.getCol();

            if (isInside(row, col)) {
                excluded[row][col] = true;
            }
        }

        BlockType[][] rows = new BlockType[rowIndexes.size()][COLS];

        for (int index = 0; index < rowIndexes.size(); index++) {
            int sourceRow = rowIndexes.get(index);

            for (int col = 0; col < COLS; col++) {
                if (!excluded[sourceRow][col]) {
                    rows[index][col] = cells[sourceRow][col];
                }
            }
        }

        return new AttackRows(rows);
    }

    // 공격 행 삽입. 위로 밀려나간 고정 셀이 있으면 true
    public boolean insertAttackRows(AttackRows attackRows) {
        if (attackRows == null) {
            throw new IllegalArgumentException(
                    "attackRows must not be null"
            );
        }

        int rowCount = attackRows.getRowCount();

        if (rowCount > ROWS) {
            throw new IllegalArgumentException(
                    "attack row count must not exceed " + ROWS
            );
        }

        if (rowCount == 0) {
            return false;
        }

        boolean overflow = false;

        // 위로 밀려나갈 구간 확인
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < COLS; col++) {
                if (cells[row][col] != null) {
                    overflow = true;
                }
            }
        }

        // 기존 보드를 위로 이동
        for (int row = 0; row < ROWS - rowCount; row++) {
            copyRow(row + rowCount, row);
        }

        BlockType[][] incomingRows = attackRows.getRows();

        // 보드 아래에 공격 행 추가
        for (int index = 0; index < rowCount; index++) {
            int targetRow = ROWS - rowCount + index;

            System.arraycopy(
                    incomingRows[index], 0,
                    cells[targetRow], 0,
                    COLS
            );

            for (int col = 0; col < COLS; col++) {
                attackCells[targetRow][col] =
                        cells[targetRow][col] != null;
            }
        }

        return overflow;
    }

    // 공격으로 들어온 고정 셀인지 확인
    public boolean isAttackCell(int row, int col) {
        return isInside(row, col)
                && cells[row][col] != null
                && attackCells[row][col];
    }

    public void applyClear(ClearResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        if (result.isEmpty()) {
            return;
        }

        // 1. 폭탄은 삭제한 칸 위의 셀을 내리고, 나머지는 셀만 삭제
        if (result.getItemType() == ItemType.BOMB) {
            deleteBombCellsAndDrop(result.getCellsToDelete());
        } else {
            for (Position cell : result.getCellsToDelete()) {
                cells[cell.getRow()][cell.getCol()] = null;
                attackCells[cell.getRow()][cell.getCol()] = false;
            }
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
                boolean attack = attackCells[readRow][col];

                cells[readRow][col] = null;
                attackCells[readRow][col] = false;

                cells[writeRow][col] = type;
                attackCells[writeRow][col] = attack;

                writeRow--;
            }
        }

        // 압축 후 위에 남은 구간 비우기
        for (int row = writeRow; row >= startRow; row--) {
            cells[row][col] = null;
            attackCells[row][col] = false;
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
                copyRow(readRow, writeRow);
            }

            writeRow--;
        }

        for (int row = writeRow; row >= 0; row--) {
            for (int col = 0; col < COLS; col++) {
                cells[row][col] = null;
                attackCells[row][col] = false; 
            }
        }
    }

    private void copyRow(int sourceRow, int targetRow) {
        System.arraycopy(
                cells[sourceRow], 0,
                cells[targetRow], 0,
                COLS
        );

        System.arraycopy(
                attackCells[sourceRow], 0,
                attackCells[targetRow], 0,
                COLS
        );
    }

}