package com.team.tetris.service;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Position;

import java.util.Random;

public class BlockFactory {
    private final Random random = new Random();

    public Block createRandomBlock() {
        BlockType[] types = BlockType.values();
        return createBlock(types[random.nextInt(types.length)]);
    }

    public Block createBlock(BlockType type) {
        return new Block(type, new Position(1, Board.COLS / 2));
    }
}
