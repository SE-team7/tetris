package com.team.tetris.service.item;

import java.util.ArrayList;
import java.util.List;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

public class WeightEffect implements ItemEffect {
    @Override
    public ClearResult calculate(Board board, Block block) {
        List<Position> cellsToDelete = new ArrayList<>();

        // 무게추가 한 칸 내려갈 때 겹치는 고정 블록을 삭제
        for (Position cell : block.getMovedCells(1, 0)) {
            int row = cell.getRow();
            int col = cell.getCol();

            if (board.isInside(row, col)) {
                cellsToDelete.add(new Position(row, col));
            }
        }

        return new ClearResult(
                ItemType.WEIGHT,
                List.of(),
                cellsToDelete,
                List.of(),
                -1,
                0,
                0
        );
    }
}