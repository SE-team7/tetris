package com.team.tetris.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// [대전 모드] 대전 설정 값 검증 테스트
class BattleConfigTest {

    @Test
    void normalModeHasDifficultyAndNoTimeLimit() {
        BattleConfig config = BattleConfig.normal(Difficulty.HARD);

        assertEquals(BattleMode.NORMAL, config.getMode());
        assertEquals(Difficulty.HARD, config.getDifficulty());
        assertEquals(0, config.getTimeLimitSeconds());
        assertFalse(config.isItemMode());
        assertFalse(config.isTimeLimited());
    }

    @Test
    void itemModeHasNoTimeLimit() {
        BattleConfig config = BattleConfig.item();

        assertEquals(BattleMode.ITEM, config.getMode());
        assertEquals(0, config.getTimeLimitSeconds());
        assertTrue(config.isItemMode());
    }

    @Test
    void timeLimitModeAcceptsOnlyAllowedSeconds() {
        for (int seconds : new int[] {180, 300, 600}) {
            BattleConfig config = BattleConfig.timeLimit(Difficulty.EASY, seconds);
            assertEquals(seconds, config.getTimeLimitSeconds());
            assertTrue(config.isTimeLimited());
        }
        assertThrows(IllegalArgumentException.class,
                () -> BattleConfig.timeLimit(Difficulty.EASY, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleConfig.timeLimit(Difficulty.EASY, 120));
    }

    @Test
    void nonTimeLimitModeRejectsTimeLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new BattleConfig(BattleMode.NORMAL, Difficulty.EASY, 180));
    }

    @Test
    void nullValuesAreRejected() {
        assertThrows(NullPointerException.class,
                () -> new BattleConfig(null, Difficulty.EASY, 0));
        assertThrows(NullPointerException.class,
                () -> new BattleConfig(BattleMode.NORMAL, null, 0));
    }

    @Test
    void sameSettingsAreEqual() {
        assertEquals(BattleConfig.timeLimit(Difficulty.NORMAL, 300),
                BattleConfig.timeLimit(Difficulty.NORMAL, 300));
        assertEquals(BattleConfig.normal(Difficulty.EASY).hashCode(),
                BattleConfig.normal(Difficulty.EASY).hashCode());
        assertNotEquals(BattleConfig.normal(Difficulty.EASY), BattleConfig.normal(Difficulty.HARD));
        assertNotEquals(BattleConfig.normal(Difficulty.EASY), "config");
    }
}
