package com.team.tetris.service;

import java.util.Random;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Position;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.ItemType;

public class BlockFactory {
    //보드 바로 위에 생성됨
    private static final int SPAWN_ROW = 4;

    //보드 위 한줄 비우고 생성됨
    //private static final int SPAWN_COL = 3;

    private static final BlockType[] NORMAL_TYPES = {
        BlockType.I, BlockType.O, BlockType.T, BlockType.S,
        BlockType.Z, BlockType.J, BlockType.L
    };

    private static final ItemType[] ITEM_TYPES = {
        ItemType.WEIGHT, ItemType.LINE_CLEAR, ItemType.BOMB, ItemType.CROSS, ItemType.GAP_DESTROY
    };

    private final Random random = new Random();

    //난이도 별 블록 랜덤 생성
    public Block createRandomBlock(Difficulty difficulty) {
        if (difficulty == null) {throw new IllegalArgumentException("difficulty must not be null");
}

        double iProbability = switch (difficulty) {
            case EASY -> (1.0 / 7) * 1.2;
            case NORMAL -> 1.0 / 7;
            case HARD -> (1.0 / 7) * 0.8;
        };

        if (random.nextDouble() < iProbability) {
            return createBlock(BlockType.I);
        }

        // I를 제외한 나머지 동일 확률로 선택
        BlockType type = NORMAL_TYPES[
                1 + random.nextInt(NORMAL_TYPES.length - 1)
        ];

        return createBlock(type);
    } 

    // 아이템 모드에서는 난이도를 사용하지 않음.
    public Block createItemBlock() {
        ItemType itemType = ITEM_TYPES[
                random.nextInt(ITEM_TYPES.length)
        ];

        return switch (itemType) {
            case WEIGHT -> createBlock(BlockType.WEIGHT);
            case BOMB -> createBlock(BlockType.BOMB);
            case CROSS -> createBlock(BlockType.CROSS);
            case GAP_DESTROY -> createBlock(BlockType.GAP_DESTROY);
            case LINE_CLEAR -> createLineClearBlock();
            case NONE -> throw new IllegalStateException(
                    "NONE cannot be selected as an item"
            );
        };
    }

    private Block createLineClearBlock() {
        BlockType type = NORMAL_TYPES[
                random.nextInt(NORMAL_TYPES.length)
        ];

        int itemCellIndex = random.nextInt(4);

        return new Block(
                type,
                new Position(SPAWN_ROW, getSpawnCol(type)),
                ItemType.LINE_CLEAR,
                itemCellIndex
        );
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
            case I, Z, CROSS, BOMB-> 5;
            case O, J, L, S, T, WEIGHT, GAP_DESTROY-> 4;
        };
    }
}