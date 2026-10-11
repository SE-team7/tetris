package com.team.tetris.service.item;

import java.util.ArrayList;
import java.util.List;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

public class CrossEffect implements ItemEffect {
    @Override
    public ClearResult calculate(Board board, Block block) {
        Position center = block.getItemPosition();

        if (center.getRow() < 5
                || !board.isInside(center.getRow(), center.getCol())) {
            return ClearResult.empty(ItemType.CROSS);
        }

        List<Position> cellsToDelete = new ArrayList<>();

        for (int row = 5; row < Board.ROWS; row++) {
            cellsToDelete.add(new Position(row, center.getCol()));
        }

        return new ClearResult(
                ItemType.CROSS,
                List.of(center.getRow()),
                cellsToDelete,
                List.of(),
                -1,
                1,
                1
        );
    }
}