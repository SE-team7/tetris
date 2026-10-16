package com.team.tetris.service.item;

import java.util.List;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

public class GapDestroyEffect implements ItemEffect {
    @Override
    public ClearResult calculate(Board board, Block block) {
        Position[] cells = block.getCells();

        List<Integer> columns = List.of(
                cells[0].getCol(),
                cells[1].getCol()
        );

        return new ClearResult(
                ItemType.GAP_DESTROY,
                List.of(),
                List.of(),
                columns,
                cells[0].getRow(),
                0,
                0
        );
    }
}
