package com.team.tetris.service;

import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Difficulty;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockFactoryTest {

    @Test
    void blockDistributionMatchesExpectedProbabilities() {
        int trials = 100_000;

        BlockType[] normalTypes = {
                BlockType.I, BlockType.O, BlockType.T,
                BlockType.S, BlockType.Z, BlockType.J,
                BlockType.L
        };

        for (Difficulty difficulty : Difficulty.values()) {
            BlockFactory factory = new BlockFactory();
            Map<BlockType, Integer> counts =
                    new EnumMap<>(BlockType.class);

            double expectedI = switch (difficulty) {
                case EASY -> (1.0 / 7) * 1.2;
                case NORMAL -> 1.0 / 7;
                case HARD -> (1.0 / 7) * 0.8;
            };

            double expectedOther = (1.0 - expectedI) / 6;

            for (int i = 0; i < trials; i++) {
                BlockType type =
                        factory.createRandomBlock(difficulty).getType();

                boolean isNormal = false;
                for (BlockType normalType : normalTypes) {
                    if (type == normalType) {
                        isNormal = true;
                        break;
                    }
                }

                assertTrue(isNormal, "일반 모드에 아이템 블록이 생성됨");
                counts.merge(type, 1, Integer::sum);
            }

            for (BlockType type : normalTypes) {
                double expected = (type == BlockType.I)
                        ? expectedI : expectedOther;

                double actual =
                        counts.getOrDefault(type, 0) / (double) trials;

                // 예상 확률 대비 상대 오차 5% 이내
                double relativeError =
                        Math.abs(actual - expected) / expected;

                assertTrue(
                        relativeError <= 0.05,
                        difficulty + " / " + type
                                + ": 예상=" + expected
                                + ", 실제=" + actual
                                + ", 상대 오차=" + relativeError
                );
            }
        }
    }
}
