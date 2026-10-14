package com.team.tetris.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;

import javafx.scene.input.KeyCode;

class SettingsTest {

    @Test
    void defaults() {
        Settings settings = new Settings();

        assertEquals(ScreenSize.MEDIUM, settings.getScreenSize());
        assertFalse(settings.isColorBlindMode());
        assertEquals(Difficulty.NORMAL, settings.getDifficulty());
        for (KeyAction action : KeyAction.values()) {
            assertEquals(action.getDefaultKey(), settings.getKey(action));
        }
    }

    @Test
    void screenSize_hasWidthAndHeight() {
        assertEquals(640, ScreenSize.SMALL.getWidth());
        assertEquals(720, ScreenSize.SMALL.getHeight());
        assertEquals(800, ScreenSize.MEDIUM.getWidth());
        assertEquals(800, ScreenSize.MEDIUM.getHeight());
        assertEquals(1000, ScreenSize.LARGE.getWidth());
        assertEquals(900, ScreenSize.LARGE.getHeight());
    }

    @Test
    void setters_ignoreNull() {
        Settings settings = new Settings();
        settings.setScreenSize(ScreenSize.LARGE);
        settings.setDifficulty(Difficulty.HARD);

        settings.setScreenSize(null);
        settings.setDifficulty(null);

        assertEquals(ScreenSize.LARGE, settings.getScreenSize());
        assertEquals(Difficulty.HARD, settings.getDifficulty());
    }

    @Test
    void colorBlindMode_canBeToggled() {
        Settings settings = new Settings();
        settings.setColorBlindMode(true);
        assertTrue(settings.isColorBlindMode());
        settings.setColorBlindMode(false);
        assertFalse(settings.isColorBlindMode());
    }

    @Test
    void setKey_rejectsNull() {
        Settings settings = new Settings();

        assertFalse(settings.setKey(null, KeyCode.Q));
        assertFalse(settings.setKey(KeyAction.SKIP, null));
        assertEquals(KeyCode.K, settings.getKey(KeyAction.SKIP));
    }

    @Test
    void fixedAction_acceptsOnlyItsDefaultKey() {
        Settings settings = new Settings();

        assertTrue(settings.setKey(KeyAction.PAUSE, KeyCode.SHIFT));
        assertFalse(settings.setKey(KeyAction.PAUSE, KeyCode.P));
        assertTrue(KeyAction.PAUSE.isFixed());
        assertFalse(KeyAction.SKIP.isFixed());
    }

    @Test
    void setKeys_appliesSwapAtOnce_orNothing() {
        Settings settings = new Settings();

        // ROTATE(P) ↔ SKIP(K) 동시에 교환
        Map<KeyAction, KeyCode> swap = new EnumMap<>(KeyAction.class);
        swap.put(KeyAction.ROTATE, KeyCode.K);
        swap.put(KeyAction.SKIP, KeyCode.P);
        assertTrue(settings.setKeys(swap));
        assertEquals(KeyCode.K, settings.getKey(KeyAction.ROTATE));
        assertEquals(KeyCode.P, settings.getKey(KeyAction.SKIP));

        // 하나라도 중복이면 아무것도 바뀌지 않음
        Map<KeyAction, KeyCode> bad = new EnumMap<>(KeyAction.class);
        bad.put(KeyAction.MOVE_LEFT, KeyCode.Q);
        bad.put(KeyAction.MOVE_RIGHT, KeyCode.Q);
        assertFalse(settings.setKeys(bad));
        assertEquals(KeyCode.A, settings.getKey(KeyAction.MOVE_LEFT));
        assertEquals(KeyCode.D, settings.getKey(KeyAction.MOVE_RIGHT));

        // 고정 동작을 다른 키로 바꾸려 하면 실패
        Map<KeyAction, KeyCode> fixed = new EnumMap<>(KeyAction.class);
        fixed.put(KeyAction.EXIT, KeyCode.Q);
        assertFalse(settings.setKeys(fixed));
    }

    @Test
    void copyFrom_copiesAllValues_andStaysIndependent() {
        Settings source = new Settings();
        source.setScreenSize(ScreenSize.SMALL);
        source.setColorBlindMode(true);
        source.setDifficulty(Difficulty.EASY);
        source.setKey(KeyAction.SKIP, KeyCode.Q);

        Settings target = new Settings();
        target.copyFrom(source);

        assertEquals(ScreenSize.SMALL, target.getScreenSize());
        assertTrue(target.isColorBlindMode());
        assertEquals(Difficulty.EASY, target.getDifficulty());
        assertEquals(KeyCode.Q, target.getKey(KeyAction.SKIP));

        // 복사 후 원본을 바꿔도 대상은 그대로
        source.setKey(KeyAction.SKIP, KeyCode.Z);
        assertEquals(KeyCode.Q, target.getKey(KeyAction.SKIP));
    }

    @Test
    void findGameAction_returnsNullForUnknownOrNull() {
        Settings settings = new Settings();

        assertEquals(KeyAction.MOVE_LEFT, settings.findGameAction(KeyCode.A));
        assertNull(settings.findGameAction(KeyCode.F1));
        assertNull(settings.findGameAction(null));
    }

    @Test
    void isFixedKey() {
        assertTrue(Settings.isFixedKey(KeyCode.SHIFT));
        assertTrue(Settings.isFixedKey(KeyCode.TAB));
        assertTrue(Settings.isFixedKey(KeyCode.ESCAPE));
        assertFalse(Settings.isFixedKey(KeyCode.K));
        assertFalse(Settings.isFixedKey(null));
    }
}
