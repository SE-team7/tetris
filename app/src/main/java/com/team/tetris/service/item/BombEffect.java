package com.team.tetris.service.item;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

import java.util.ArrayList;
import java.util.List;

public class BombEffect implements ItemEffect {
    @Override
    public ClearResult calculate(Board board, Block block) {
        Position center = block.getItemPosition();
        List<Position> cellsToDelete = new ArrayList<>();

        for (int deltaRow = -1; deltaRow <= 1; deltaRow++) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
                int row = center.getRow() + deltaRow;
                int col = center.getCol() + deltaCol;

                if (board.isInside(row, col)) {
                    cellsToDelete.add(new Position(row, col));
                }
            }
        }

        return new ClearResult(
                ItemType.BOMB,
                List.of(),
                cellsToDelete,
                List.of(),
                -1,
                0,
                0
        );
    }
}