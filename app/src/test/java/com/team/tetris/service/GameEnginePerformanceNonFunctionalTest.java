package com.team.tetris.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;

//각 동작을 1,000회 측정하여 엔진 메서드의 최대 처리 시간이 100ms 이내인지 확인
public class GameEnginePerformanceNonFunctionalTest {
    private static final long INPUT_LIMIT_NS = 100_000_000L; // 100ms

    @Test
    void inputProcessingCompletesWithin100ms() {
        GameEngineService engine = new GameEngineService();

        // JVM 워밍업
        for (int i = 0; i < 1_000; i++) {
            prepareGame(engine);
            engine.moveLeft();
            engine.moveRight();
            engine.rotate();
            engine.hardDrop();
        }

        long maxLeft = 0;
        long maxRight = 0;
        long maxRotate = 0;
        long maxHardDrop = 0;

        for (int i = 0; i < 1_000; i++) {
            //게임 초기화와 블록 생성은 측정에서 제외
            prepareGame(engine);

            maxLeft = Math.max(
                    maxLeft, measureNanos(() -> engine.moveLeft()));
            maxRight = Math.max(
                    maxRight, measureNanos(() -> engine.moveRight()));
            maxRotate = Math.max(
                    maxRotate, measureNanos(() -> engine.rotate()));
            maxHardDrop = Math.max(
                    maxHardDrop, measureNanos(engine::hardDrop));
        }

        assertWithinLimit("Move_Left", maxLeft);
        assertWithinLimit("Move_Right", maxRight);
        assertWithinLimit("Rotate", maxRotate);
        assertWithinLimit("Hard_Drop", maxHardDrop);
    }

    private void prepareGame(GameEngineService engine) {
        engine.startGame(Difficulty.NORMAL, false);
        engine.tick();

        assertFalse(engine.isGameOver());
        assertNotNull(engine.getCurrentBlock());
    }

    private long measureNanos(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return System.nanoTime() - start;
    }

    private void assertWithinLimit(String action, long elapsedNanos) {
        double elapsedMillis = elapsedNanos / 1_000_000.0;

        System.out.printf(
                "%s max processing time: %.3fms%n",
                action, elapsedMillis
        );

        assertTrue(
                elapsedNanos <= INPUT_LIMIT_NS,
                () -> action + " over the limit: "
                        + elapsedMillis + "ms"
        );
    }
}

    
