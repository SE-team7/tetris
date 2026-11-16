package com.team.tetris.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.input.KeyCode;

public class Settings {

    public enum ScreenSize {
        SMALL(640, 720),
        MEDIUM(800, 800),
        LARGE(1000, 900);

        private final int width;
        private final int height;

        ScreenSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    public enum Theme {
        LIGHT,
        DARK
    }

    public enum KeyAction {
        MOVE_LEFT(KeyCode.A),
        MOVE_RIGHT(KeyCode.D),
        SOFT_DROP(KeyCode.S),
        HARD_DROP(KeyCode.ENTER),
        ROTATE(KeyCode.P),
        SKIP(KeyCode.K),
        PAUSE(KeyCode.SHIFT),
        QUIT(KeyCode.TAB),
        EXIT(KeyCode.ESCAPE);

        private final KeyCode defaultKey;

        KeyAction(KeyCode defaultKey) {
            this.defaultKey = defaultKey;
        }

        public KeyCode getDefaultKey() {
            return defaultKey;
        }

        public boolean isFixed() {
            return this == PAUSE || this == QUIT || this == EXIT;
        }
    }

    private ScreenSize screenSize = ScreenSize.MEDIUM;
    private Theme theme = Theme.LIGHT;
    private boolean colorBlindMode;
    private Difficulty difficulty = Difficulty.NORMAL;

    private final Map<KeyAction, KeyCode> keys =
        new EnumMap<>(KeyAction.class);

    private final Map<PlayerId, Map<KeyAction, KeyCode>> battleKeys =
        new EnumMap<>(PlayerId.class);

    public Settings() {
        for (KeyAction action : KeyAction.values()) {
            keys.put(action, action.getDefaultKey());
        }

        battleKeys.put(PlayerId.PLAYER1, createP1Keys());
        battleKeys.put(PlayerId.PLAYER2, createP2Keys());
    }

    private Map<KeyAction, KeyCode> createP1Keys() {
        Map<KeyAction, KeyCode> map = new EnumMap<>(KeyAction.class);
        map.put(KeyAction.MOVE_LEFT, KeyCode.A);
        map.put(KeyAction.MOVE_RIGHT, KeyCode.D);
        map.put(KeyAction.SOFT_DROP, KeyCode.S);
        map.put(KeyAction.ROTATE, KeyCode.W);
        map.put(KeyAction.HARD_DROP, KeyCode.SPACE);
        map.put(KeyAction.SKIP, KeyCode.CONTROL);
        map.put(KeyAction.PAUSE, KeyCode.SHIFT);
        map.put(KeyAction.QUIT, KeyCode.TAB);
        map.put(KeyAction.EXIT, KeyCode.ESCAPE);
        return map;
    }

    private Map<KeyAction, KeyCode> createP2Keys() {
        Map<KeyAction, KeyCode> map = new EnumMap<>(KeyAction.class);
        map.put(KeyAction.MOVE_LEFT, KeyCode.LEFT);
        map.put(KeyAction.MOVE_RIGHT, KeyCode.RIGHT);
        map.put(KeyAction.SOFT_DROP, KeyCode.DOWN);
        map.put(KeyAction.ROTATE, KeyCode.UP);
        map.put(KeyAction.HARD_DROP, KeyCode.ENTER);
        map.put(KeyAction.SKIP, KeyCode.P);
        map.put(KeyAction.PAUSE, KeyCode.SHIFT);
        map.put(KeyAction.QUIT, KeyCode.TAB);
        map.put(KeyAction.EXIT, KeyCode.ESCAPE);
        return map;
    }

    public ScreenSize getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(ScreenSize value) {
        if (value != null) screenSize = value;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme value) {
        if (value != null) theme = value;
    }

    public boolean isColorBlindMode() {
        return colorBlindMode;
    }

    public void setColorBlindMode(boolean value) {
        colorBlindMode = value;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty value) {
        if (value != null) difficulty = value;
    }

    public KeyCode getKey(KeyAction action) {
        return keys.get(action);
    }

    public boolean setKey(KeyAction action, KeyCode key) {
        if (action == null || key == null) return false;

        Map<KeyAction, KeyCode> changed = new EnumMap<>(keys);
        changed.put(action, key);
        return setKeys(changed);
    }

    public boolean setKeys(Map<KeyAction, KeyCode> newKeys) {
        if (newKeys == null) return false;

        Map<KeyAction, KeyCode> result = new EnumMap<>(keys);

        for (var entry : newKeys.entrySet()) {
            KeyAction action = entry.getKey();
            KeyCode key = entry.getValue();

            if (action == null || key == null) return false;

            if (action.isFixed()) {
                if (key != action.getDefaultKey()) return false;
            } else {
                result.put(action, key);
            }
        }

        Set<KeyCode> used = EnumSet.noneOf(KeyCode.class);

        for (KeyAction action : KeyAction.values()) {
            KeyCode key = result.get(action);

            if (key == null
                || (!action.isFixed() && isFixedKey(key))
                || !used.add(key)) {
                return false;
            }
        }

        keys.putAll(result);
        return true;
    }

    public static boolean isFixedKey(KeyCode key) {
        return key == KeyCode.SHIFT
            || key == KeyCode.TAB
            || key == KeyCode.ESCAPE;
    }

    public KeyAction findGameAction(KeyCode key) {
        if (key == null) return null;

        for (var entry : keys.entrySet()) {
            if (entry.getValue() == key) return entry.getKey();
        }

        return null;
    }

    public KeyCode getBattleKey(PlayerId player, KeyAction action) {
        Map<KeyAction, KeyCode> map = battleKeys.get(player);
        return map == null ? null : map.get(action);
    }

    public KeyAction findBattleAction(PlayerId player, KeyCode key) {
        Map<KeyAction, KeyCode> map = battleKeys.get(player);
        if (map == null || key == null) return null;

        for (var entry : map.entrySet()) {
            if (entry.getValue() == key) return entry.getKey();
        }

        return null;
    }

    public boolean setBattleKey(
        PlayerId player,
        KeyAction action,
        KeyCode key
    ) {
        if (player == null || action == null || key == null) return false;

        Map<KeyAction, KeyCode> map = battleKeys.get(player);
        if (map == null) return false;

        map.put(action, key);
        return true;
    }

    public void copyFrom(Settings source) {
        if (source == null) return;

        screenSize = source.screenSize;
        theme = source.theme;
        colorBlindMode = source.colorBlindMode;
        difficulty = source.difficulty;

        keys.clear();
        keys.putAll(source.keys);

        battleKeys.clear();

        for (var entry : source.battleKeys.entrySet()) {
            battleKeys.put(
                entry.getKey(),
                new EnumMap<>(entry.getValue())
            );
        }
    }
}