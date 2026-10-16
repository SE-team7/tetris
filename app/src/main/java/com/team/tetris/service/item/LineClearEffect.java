package com.team.tetris.service.item;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

import java.util.List;

public class LineClearEffect implements ItemEffect {
    @Override
    public ClearResult calculate(Board board, Block block) {
        Position itemPosition = block.getItemPosition();

        if (itemPosition == null
                || !board.isInside(
                        itemPosition.getRow(),
                        itemPosition.getCol()
                )) {
            throw new IllegalArgumentException("invalid line-clear position");
        }

        return new ClearResult(
                ItemType.LINE_CLEAR,
                List.of(itemPosition.getRow()),
                List.of(),
                List.of(),
                -1,
                1,
                1
        );
    }
}