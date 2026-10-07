package com.team.tetris.service;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Position;

import java.util.Random;

public class BlockFactory {
    //보드 바로 위에 생성됨
    private static final int SPAWN_ROW = 4;

    //보드 위 한줄 비우고 생성됨
    //private static final int SPAWN_COL = 3;

    private final Random random = new Random();

    //동일 확률로 블록 랜덤 생성
    public Block createRandomBlock() {
        BlockType[] types = BlockType.values();
        return createBlock(types[random.nextInt(types.length)]);
    }

    public Block createBlock(BlockType type) {
        return new Block(
                type,
                new Position(SPAWN_ROW, getSpawnCol(type))
        );
    }
    //가로 중앙 생성, 홀수면 오른쪽 기준
    private int getSpawnCol(BlockType type) {
        return switch (type) {
            case I -> 5;
            case O, J, L, S, Z, T -> 4;
        };
    }
}