package com.team.tetris.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.input.KeyCode;

public class Settings {

    public enum ScreenSize {
        SMALL(640, 720), MEDIUM(800, 800), LARGE(1000, 900);

        private final int width;
        private final int height;

        ScreenSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
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

        public KeyCode getDefaultKey() { return defaultKey; }

        public boolean isFixed() {
            return this == PAUSE || this == QUIT || this == EXIT;
        }
    }

    private ScreenSize screenSize = ScreenSize.MEDIUM;
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

        Map<KeyAction, KeyCode> p1 = new EnumMap<>(KeyAction.class);
        p1.put(KeyAction.MOVE_LEFT, KeyCode.A);
        p1.put(KeyAction.MOVE_RIGHT, KeyCode.D);
        p1.put(KeyAction.SOFT_DROP, KeyCode.S);
        p1.put(KeyAction.ROTATE, KeyCode.W);
        p1.put(KeyAction.HARD_DROP, KeyCode.SPACE);
        p1.put(KeyAction.SKIP, KeyCode.CONTROL);

        Map<KeyAction, KeyCode> p2 = new EnumMap<>(KeyAction.class);
        p2.put(KeyAction.MOVE_LEFT, KeyCode.LEFT);
        p2.put(KeyAction.MOVE_RIGHT, KeyCode.RIGHT);
        p2.put(KeyAction.SOFT_DROP, KeyCode.DOWN);
        p2.put(KeyAction.ROTATE, KeyCode.UP);
        p2.put(KeyAction.HARD_DROP, KeyCode.ENTER);
        p2.put(KeyAction.SKIP, KeyCode.P);

        battleKeys.put(PlayerId.PLAYER1, p1);
        battleKeys.put(PlayerId.PLAYER2, p2);
    }

    public ScreenSize getScreenSize() { return screenSize; }

    public void setScreenSize(ScreenSize screenSize) {
        if (screenSize != null) this.screenSize = screenSize;
    }

    public boolean isColorBlindMode() { return colorBlindMode; }

    public void setColorBlindMode(boolean enabled) {
        colorBlindMode = enabled;
    }

    public Difficulty getDifficulty() { return difficulty; }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty != null) this.difficulty = difficulty;
    }

    public KeyCode getKey(KeyAction action) {
        return keys.get(action);
    }

    public static boolean isFixedKey(KeyCode key) {
        for (KeyAction action : KeyAction.values()) {
            if (action.isFixed() && action.getDefaultKey() == key) return true;
        }
        return false;
    }

    public boolean setKey(KeyAction action, KeyCode key) {
        if (action == null || key == null) return false;

        Map<KeyAction, KeyCode> changed = new EnumMap<>(keys);
        changed.put(action, key);
        return setKeys(changed);
    }

    public boolean setKeys(Map<KeyAction, KeyCode> newKeys) {
        Map<KeyAction, KeyCode> result = new EnumMap<>(keys);

        for (Map.Entry<KeyAction, KeyCode> entry : newKeys.entrySet()) {
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

        keys.clear();
        keys.putAll(result);
        return true;
    }

    public KeyCode getBattleKey(PlayerId player, KeyAction action) {
        Map<KeyAction, KeyCode> map = battleKeys.get(player);
        return map == null ? null : map.get(action);
    }

    public boolean setBattleKey(
        PlayerId player,
        KeyAction action,
        KeyCode key
    ) {
        if (!isBattleAction(action) || player == null || key == null) {
            return false;
        }

        Map<KeyAction, KeyCode> changed =
            new EnumMap<>(battleKeys.get(player));

        changed.put(action, key);
        return setBattleKeys(player, changed);
    }

    public boolean setBattleKeys(
        PlayerId player,
        Map<KeyAction, KeyCode> newKeys
    ) {
        if (player == null || newKeys == null) return false;

        Map<KeyAction, KeyCode> result =
            new EnumMap<>(battleKeys.get(player));

        for (Map.Entry<KeyAction, KeyCode> entry : newKeys.entrySet()) {
            if (!isBattleAction(entry.getKey()) || entry.getValue() == null) {
                return false;
            }
            result.put(entry.getKey(), entry.getValue());
        }

        Set<KeyCode> used = EnumSet.noneOf(KeyCode.class);

        for (KeyAction action : battleActions()) {
            KeyCode key = result.get(action);

            if (key == null || isFixedKey(key) || !used.add(key)) {
                return false;
            }
        }

        battleKeys.put(player, result);
        return true;
    }

    public boolean isBattleKeyAvailable(
        PlayerId player,
        KeyAction action,
        KeyCode key
    ) {
        if (player == null || !isBattleAction(action) || key == null) {
            return false;
        }

        for (KeyAction other : battleActions()) {
            if (other != action && getBattleKey(player, other) == key) {
                return false;
            }
        }

        return !isFixedKey(key);
    }

    public KeyAction findBattleAction(PlayerId player, KeyCode key) {
        if (player == null || key == null) return null;

        Map<KeyAction, KeyCode> map = battleKeys.get(player);
        if (map == null) return null;

        for (Map.Entry<KeyAction, KeyCode> entry : map.entrySet()) {
            if (entry.getValue() == key) return entry.getKey();
        }

        return null;
    }

    public KeyAction findGameAction(KeyCode key) {
        for (Map.Entry<KeyAction, KeyCode> entry : keys.entrySet()) {
            if (entry.getValue() == key) return entry.getKey();
        }
        return null;
    }

    public void copyFrom(Settings source) {
        if (source == null) return;

        screenSize = source.screenSize;
        colorBlindMode = source.colorBlindMode;
        difficulty = source.difficulty;

        keys.clear();
        keys.putAll(source.keys);

        battleKeys.clear();

        for (PlayerId player : PlayerId.values()) {
            Map<KeyAction, KeyCode> sourceMap =
                source.battleKeys.get(player);

            if (sourceMap != null) {
                battleKeys.put(player, new EnumMap<>(sourceMap));
            }
        }
    }

    private static boolean isBattleAction(KeyAction action) {
        return action == KeyAction.MOVE_LEFT
            || action == KeyAction.MOVE_RIGHT
            || action == KeyAction.SOFT_DROP
            || action == KeyAction.ROTATE
            || action == KeyAction.HARD_DROP
            || action == KeyAction.SKIP;
    }

    private static KeyAction[] battleActions() {
        return new KeyAction[] {
            KeyAction.MOVE_LEFT,
            KeyAction.MOVE_RIGHT,
            KeyAction.SOFT_DROP,
            KeyAction.ROTATE,
            KeyAction.HARD_DROP,
            KeyAction.SKIP
        };
    }
}