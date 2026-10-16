package com.team.tetris.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.input.KeyCode;

/**
 * 사용자 설정 (화면 크기, 조작키, 색맹 모드, 난이도).
 * 저장·불러오기는 SettingsService가 담당한다.
 */
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

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    /** 게임 조작. PAUSE·QUIT·EXIT는 고정키라 변경할 수 없다. */
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
    private boolean colorBlindMode = false;
    private Difficulty difficulty = Difficulty.NORMAL;
    private final Map<KeyAction, KeyCode> keys = new EnumMap<>(KeyAction.class);

    public Settings() {
        for (KeyAction action : KeyAction.values()) {
            keys.put(action, action.getDefaultKey());
        }
    }

    public ScreenSize getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(ScreenSize screenSize) {
        if (screenSize != null) {
            this.screenSize = screenSize;
        }
    }

    public boolean isColorBlindMode() {
        return colorBlindMode;
    }

    public void setColorBlindMode(boolean colorBlindMode) {
        this.colorBlindMode = colorBlindMode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty != null) {
            this.difficulty = difficulty;
        }
    }

    public KeyCode getKey(KeyAction action) {
        return keys.get(action);
    }

    /** 고정키(Shift·Tab·Esc)인지 여부. 고정키는 다른 동작에 지정할 수 없다. */
    public static boolean isFixedKey(KeyCode key) {
        for (KeyAction action : KeyAction.values()) {
            if (action.isFixed() && action.getDefaultKey() == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * 조작키 하나를 변경한다.
     * 고정 동작은 변경할 수 없고, 고정키나 다른 동작이 이미 쓰는 키로도 바꿀 수 없다.
     * @return 변경 성공 여부
     */
    public boolean setKey(KeyAction action, KeyCode key) {
        if (action == null || key == null) {
            return false;
        }
        Map<KeyAction, KeyCode> changed = new EnumMap<>(keys);
        changed.put(action, key);
        return setKeys(changed);
    }

    /**
     * 여러 조작키를 한 번에 변경한다. 전체를 한꺼번에 검사하므로
     * 두 동작의 키를 서로 바꾼 설정(예: ROTATE=K, SKIP=P)도 적용된다.
     * 하나라도 규칙에 어긋나면 아무것도 바꾸지 않는다.
     * @return 변경 성공 여부
     */
    public boolean setKeys(Map<KeyAction, KeyCode> newKeys) {
        Map<KeyAction, KeyCode> result = new EnumMap<>(keys);
        for (Map.Entry<KeyAction, KeyCode> entry : newKeys.entrySet()) {
            KeyAction action = entry.getKey();
            if (action.isFixed()) {
                // 고정 동작은 기본 키만 허용
                if (entry.getValue() != action.getDefaultKey()) {
                    return false;
                }
                continue;
            }
            result.put(action, entry.getValue());
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

    /** 다른 설정의 값을 모두 복사한다. (기본 설정 복구 등에 사용) */
    public void copyFrom(Settings source) {
        this.screenSize = source.screenSize;
        this.colorBlindMode = source.colorBlindMode;
        this.difficulty = source.difficulty;
        this.keys.putAll(source.keys);
    }

    /** 입력된 키에 해당하는 게임 조작. 없으면 null */
    public KeyAction findGameAction(KeyCode key) {
        for (Map.Entry<KeyAction, KeyCode> entry : keys.entrySet()) {
            if (entry.getValue() == key) {
                return entry.getKey();
            }
        }
        return null;
    }
}
