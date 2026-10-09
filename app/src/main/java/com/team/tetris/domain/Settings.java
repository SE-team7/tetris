package com.team.tetris.domain;

import javafx.scene.input.KeyCode;

import java.util.EnumMap;
import java.util.Map;

/**
 * 사용자 설정 (화면 크기, 조작키, 색맹 모드).
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

    /** 설정 가능한 조작. 게임 조작과 메뉴 조작은 서로 다른 그룹이다. */
    public enum KeyAction {
        MOVE_LEFT(true, KeyCode.A),
        MOVE_RIGHT(true, KeyCode.D),
        SOFT_DROP(true, KeyCode.S),
        HARD_DROP(true, KeyCode.ENTER),
        ROTATE(true, KeyCode.P),
        SKIP(true, KeyCode.K),
        EXIT(true, KeyCode.ESCAPE),
        PAUSE(true, KeyCode.SHIFT),
        MENU_UP(false, KeyCode.W),
        MENU_DOWN(false, KeyCode.S),
        SELECT(false, KeyCode.ENTER);

        private final boolean gameAction;
        private final KeyCode defaultKey;

        KeyAction(boolean gameAction, KeyCode defaultKey) {
            this.gameAction = gameAction;
            this.defaultKey = defaultKey;
        }

        public boolean isGameAction() {
            return gameAction;
        }

        public KeyCode getDefaultKey() {
            return defaultKey;
        }
    }

    private ScreenSize screenSize = ScreenSize.MEDIUM;
    private boolean colorBlindMode = false;
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

    public KeyCode getKey(KeyAction action) {
        return keys.get(action);
    }

    /**
     * 조작키를 변경한다. 같은 그룹(게임/메뉴) 안에서 이미 쓰는 키면 변경하지 않는다.
     * @return 변경 성공 여부 (false면 중복 키)
     */
    public boolean setKey(KeyAction action, KeyCode key) {
        if (action == null || key == null) {
            return false;
        }
        for (Map.Entry<KeyAction, KeyCode> entry : keys.entrySet()) {
            KeyAction other = entry.getKey();
            if (other != action
                    && other.isGameAction() == action.isGameAction()
                    && entry.getValue() == key) {
                return false;
            }
        }
        keys.put(action, key);
        return true;
    }

    /** 입력된 키에 해당하는 게임 조작. 없으면 null */
    public KeyAction findGameAction(KeyCode key) {
        return findAction(key, true);
    }

    /** 입력된 키에 해당하는 메뉴 조작. 없으면 null */
    public KeyAction findMenuAction(KeyCode key) {
        return findAction(key, false);
    }

    private KeyAction findAction(KeyCode key, boolean gameAction) {
        for (Map.Entry<KeyAction, KeyCode> entry : keys.entrySet()) {
            if (entry.getKey().isGameAction() == gameAction && entry.getValue() == key) {
                return entry.getKey();
            }
        }
        return null;
    }
}
