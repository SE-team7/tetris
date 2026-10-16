package com.team.tetris.service;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Difficulty;

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
                //다른 블럭에 비해 I의 등장 비율 조정
                case EASY -> 1.2 / (6 + 1.2);
                case NORMAL -> 1.0 / (6 + 1.0);
                case HARD -> 0.8 / (6 + 0.8);
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

                System.out.printf(
                    "%s / %s: count=%d/%d, expected=%.4f%%, actual=%.4f%%, relativeError=%.4f%%%n",
                    difficulty,
                    type,
                    counts.getOrDefault(type, 0),
                    trials,
                    expected * 100,
                    actual * 100,
                    relativeError * 100
                );

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
