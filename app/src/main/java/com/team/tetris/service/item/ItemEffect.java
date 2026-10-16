package com.team.tetris.service.item;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;

public interface ItemEffect {
    ClearResult calculate(Board board, Block block);
}
