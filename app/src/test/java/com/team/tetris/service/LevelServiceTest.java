package com.team.tetris.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;

class LevelServiceTest {

    private static final long BASE_INTERVAL = 1000L;
    private static final long MIN_INTERVAL = 200L;
    private static final long NORMAL_STEP = 80L;
    private static final long EASY_STEP = (long) (NORMAL_STEP * 0.8);  // 64
    private static final long HARD_STEP = (long) (NORMAL_STEP * 1.2);  // 96
    private static final int LINE_SCORE_PER_LEVEL = 30;

    @Test
    void initialStateStartsAtLevelOneWithBaseInterval() {
        LevelService service = new LevelService();

        assertEquals(1, service.getLevel());
        assertEquals(BASE_INTERVAL, service.getDropIntervalMillis());
    }

    @Test
    void updateLevelAtExactMultipleAdvancesLevel() {
        LevelService service = new LevelService();

        assertEquals(2, service.updateLevel(LINE_SCORE_PER_LEVEL));
        assertEquals(2, service.getLevel());
    }

    @Test
    void updateLevelJustBelowThresholdStaysOnPreviousLevel() {
        LevelService service = new LevelService();

        assertEquals(1, service.updateLevel(LINE_SCORE_PER_LEVEL - 1));
    }

    @Test
    void updateLevelJustAboveThresholdAdvances() {
        LevelService service = new LevelService();

        assertEquals(2, service.updateLevel(LINE_SCORE_PER_LEVEL + 1));
    }

    @Test
    void updateLevelWithZeroStaysAtLevelOne() {
        LevelService service = new LevelService();

        assertEquals(1, service.updateLevel(0));
    }

    @Test
    void updateLevelClampsNegativeScoreToZero() {
        LevelService service = new LevelService();

        assertEquals(1, service.updateLevel(-9999));
    }

    @Test
    void updateLevelHandlesLargeScore() {
        LevelService service = new LevelService();

        int score = LINE_SCORE_PER_LEVEL * 100;
        assertEquals(101, service.updateLevel(score));
    }

    @Test
    void dropIntervalDecreasesByStepPerLevel() {
        LevelService service = new LevelService();

        service.updateLevel(LINE_SCORE_PER_LEVEL);  // level 2
        assertEquals(BASE_INTERVAL - NORMAL_STEP, service.getDropIntervalMillis());
    }

    @Test
    void dropIntervalFloorsAtMinimum() {
        LevelService service = new LevelService();

        // 매우 높은 레벨에서도 최소값 유지
        service.updateLevel(LINE_SCORE_PER_LEVEL * 1000);
        assertEquals(MIN_INTERVAL, service.getDropIntervalMillis());
    }

    @Test
    void easyDifficultyReducesStepSize() {
        LevelService service = new LevelService();
        service.reset(Difficulty.EASY);

        service.updateLevel(LINE_SCORE_PER_LEVEL);  // level 2
        assertEquals(BASE_INTERVAL - EASY_STEP, service.getDropIntervalMillis());
    }

    @Test
    void hardDifficultyIncreasesStepSize() {
        LevelService service = new LevelService();
        service.reset(Difficulty.HARD);

        service.updateLevel(LINE_SCORE_PER_LEVEL);  // level 2
        assertEquals(BASE_INTERVAL - HARD_STEP, service.getDropIntervalMillis());
    }

    @Test
    void normalDifficultyUsesDefaultStep() {
        LevelService service = new LevelService();
        service.reset(Difficulty.NORMAL);

        service.updateLevel(LINE_SCORE_PER_LEVEL);
        assertEquals(BASE_INTERVAL - NORMAL_STEP, service.getDropIntervalMillis());
    }

    @Test
    void resetRevertsLevelToOne() {
        LevelService service = new LevelService();
        service.updateLevel(LINE_SCORE_PER_LEVEL * 5);

        service.reset();

        assertEquals(1, service.getLevel());
        assertEquals(BASE_INTERVAL, service.getDropIntervalMillis());
    }

    @Test
    void resetWithDifficultyChangesStepGoingForward() {
        LevelService service = new LevelService();
        service.reset(Difficulty.NORMAL);
        service.updateLevel(LINE_SCORE_PER_LEVEL);
        long normalInterval = service.getDropIntervalMillis();

        service.reset(Difficulty.HARD);
        service.updateLevel(LINE_SCORE_PER_LEVEL);
        long hardInterval = service.getDropIntervalMillis();

        assertTrue(hardInterval < normalInterval,
                "HARD는 NORMAL보다 더 빠르게 가속되어야 함");
    }

    @Test
    void allDifficultiesStartAtSameBaseIntervalAtLevelOne() {
        for (Difficulty difficulty : Difficulty.values()) {
            LevelService service = new LevelService();
            service.reset(difficulty);

            assertEquals(BASE_INTERVAL, service.getDropIntervalMillis(),
                    "레벨1 기본 간격은 난이도에 상관없이 동일해야 함: " + difficulty);
        }
    }

    @Test
    void dropIntervalMonotonicallyDecreasesWithLevel() {
        LevelService service = new LevelService();
        long previous = service.getDropIntervalMillis();

        for (int score = LINE_SCORE_PER_LEVEL; score <= LINE_SCORE_PER_LEVEL * 20; score += LINE_SCORE_PER_LEVEL) {
            service.updateLevel(score);
            long current = service.getDropIntervalMillis();
            assertTrue(current <= previous,
                    "레벨이 오를수록 간격이 줄거나 같아야 함 (level=" + service.getLevel() + ")");
            previous = current;
        }
    }

    @Test
    void updateLevelReturnsSameValueAsGetLevel() {
        LevelService service = new LevelService();

        int returned = service.updateLevel(LINE_SCORE_PER_LEVEL * 3);
        assertEquals(service.getLevel(), returned);
    }
}
