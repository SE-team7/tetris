package com.team.tetris.service;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;

//모드별 100,000회 게임 진행·조작·재시작을 반복하여 예외 및 상태 이상 확인
public class GameEngineStabilityNonFunctionalTest {
    @Test
    void normalModeRemainsStable() {
        assertTimeout(
                Duration.ofMinutes(1),
                () -> runStabilityScenario(false)
        );
    }

    @Test
    void itemModeRemainsStable() {
        assertTimeout(
                Duration.ofMinutes(1),
                () -> runStabilityScenario(true)
        );
    }

    private void runStabilityScenario(boolean itemMode) {
        GameEngineService engine = new GameEngineService();
        engine.startGame(Difficulty.NORMAL, itemMode);

        int restartCount = 0;

        for (int i = 0; i < 100_000; i++) {
            if (engine.isGameOver()) {
                engine.startGame(Difficulty.NORMAL, itemMode);
                restartCount++;

                assertFalse(engine.isGameOver());
                assertFalse(engine.isPaused());
            }

            // 블록 생성, 하강, 고정 및 줄 삭제 처리
            engine.tick();

            if (!engine.isGameOver()
                    && engine.getCurrentBlock() != null) {

                int moves = i % 5;

                for (int j = 0; j < moves; j++) {
                    if (i % 2 == 0) {
                        engine.moveLeft();
                    } else {
                        engine.moveRight();
                    }
                }

                engine.rotate();
                engine.hardDrop();
            }

            // 화면에서 처리하는 삭제 이벤트 소비
            while (engine.pollClearEvent() != null) {
                // 이벤트 큐 비우기
            }

            assertNotNull(engine.getNextBlock());
            assertTrue(engine.getClearedLines() >= 0);
            assertTrue(engine.getDropIntervalMillis() > 0);
            assertTrue(engine.getSkipsRemaining() >= 0);
            assertTrue(engine.getSkipsRemaining() <= 7);
        }

        // 반복 실행 후 새 게임과 조작 정상 여부 확인
        engine.startGame(Difficulty.NORMAL, itemMode);
        engine.tick();

        assertFalse(engine.isGameOver());
        assertFalse(engine.isPaused());
        assertNotNull(engine.getCurrentBlock());
        assertTrue(engine.moveLeft());

        System.out.printf(
            "%s mode: 100,000 iterations completed, restarts: %d%n",
            itemMode ? "Item" : "Normal",
            restartCount
        );
    }
}