package com.team.tetris.controller;

import java.util.ArrayList;
import java.util.List;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.domain.Settings.Theme;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.service.SettingsService;
import com.team.tetris.util.SceneManager;
import com.team.tetris.view.SettingsView;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SettingsController {

    private enum Type {
        SCREEN_SIZE,
        THEME,
        DIFFICULTY,
        KEY,
        COLOR_BLIND,
        SAVE,
        RESET_DEFAULT,
        RESET_SCOREBOARD,
        BACK
    }

    private record Item(Type type, String keyName) {
        Item(Type type) {
            this(type, null);
        }
    }

    private final SettingsView view;
    private final SettingsService service;
    private final ScoreboardService scoreboard;
    private final SceneManager scenes;
    private final Settings settings;

    private final List<Item> items = new ArrayList<>();

    private KeyAction waitingKey;
    private KeyCode pendingKey;
    private int selected;
    private boolean updatingDifficulty;

    public SettingsController(
        SettingsView view,
        SettingsService service,
        ScoreboardService scoreboard,
        SceneManager scenes,
        Settings settings
    ) {
        this.view = view;
        this.service = service;
        this.scoreboard = scoreboard;
        this.scenes = scenes;
        this.settings = settings;

        createItems();
        connectActions();
        connectKeys();
        connectKeyboard();
        connectFocus();

        render();

        scenes.applyScreenSize(settings.getScreenSize());
        scenes.applyTheme(settings.getTheme());
    }

    private void createItems() {
        items.add(new Item(Type.SCREEN_SIZE));
        items.add(new Item(Type.THEME));
        items.add(new Item(Type.DIFFICULTY));

        for (String name : view.getKeyActionNames()) {
            items.add(new Item(Type.KEY, name));
        }

        items.add(new Item(Type.COLOR_BLIND));
        items.add(new Item(Type.SAVE));
        items.add(new Item(Type.RESET_DEFAULT));
        items.add(new Item(Type.RESET_SCOREBOARD));
        items.add(new Item(Type.BACK));
    }

    private void connectActions() {
        view.getScreenSizeComboBox().setOnAction(e -> {
            String value = view.getScreenSizeComboBox().getValue();
            if (value != null) changeScreenSize(ScreenSize.valueOf(value));
        });

        view.getThemeComboBox().setOnAction(e -> {
            String value = view.getThemeComboBox().getValue();

            if (value != null) {
                settings.setTheme(Theme.valueOf(value));
                scenes.applyTheme(settings.getTheme());
                view.showMessage("화면 테마: " + value);
            }
        });

        view.setOnDifficultyChanged(value -> {
            if (!updatingDifficulty) changeDifficulty(value);
        });

        view.getColorBlindButton().setOnAction(e -> toggleColorBlindMode());
        view.getSaveButton().setOnAction(e -> save());
        view.getResetDefaultButton().setOnAction(e -> resetToDefault());
        view.getResetScoreboardButton().setOnAction(e -> resetScoreboard());
        view.getBackButton().setOnAction(e -> backToMainMenu());
    }

    private void connectKeys() {
        for (String name : view.getKeyActionNames()) {
            Button button = view.getKeyButton(name);
            KeyAction action = findAction(name);

            if (button != null && action != null) {
                button.setOnAction(e -> beginKeyCapture(action));
            }
        }
    }

    private KeyAction findAction(String name) {
        try {
            return KeyAction.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void connectKeyboard() {
        view.getRoot().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (waitingKey != null) {
                capture(e.getCode());
                e.consume();
                return;
            }

            if (view.getScreenSizeComboBox().isShowing()
                || view.getThemeComboBox().isShowing()
                || view.getDifficultyComboBox().isShowing()) {
                return;
            }

            switch (e.getCode()) {
                case UP -> move(-1);
                case DOWN -> move(1);
                case ENTER -> activate();
                default -> { return; }
            }

            e.consume();
        });
    }

    private void connectFocus() {
        int i = 0;

        track(view.getScreenSizeComboBox(), i++);
        track(view.getThemeComboBox(), i++);
        track(view.getDifficultyComboBox(), i++);

        for (String name : view.getKeyActionNames()) {
            track(view.getKeyButton(name), i++);
        }

        track(view.getColorBlindButton(), i++);
        track(view.getSaveButton(), i++);
        track(view.getResetDefaultButton(), i++);
        track(view.getResetScoreboardButton(), i++);
        track(view.getBackButton(), i);
    }

    private void track(Node node, int index) {
        if (node == null) return;

        node.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                selected = index;
                updateSelection();
            }
        });
    }

    private void move(int direction) {
        selected = Math.floorMod(selected + direction, items.size());
        updateSelection();
    }

    private void activate() {
        Item item = items.get(selected);

        switch (item.type()) {
            case SCREEN_SIZE -> view.getScreenSizeComboBox().show();
            case THEME -> view.getThemeComboBox().show();
            case DIFFICULTY -> view.getDifficultyComboBox().show();
            case KEY -> {
                KeyAction action = findAction(item.keyName());
                if (action != null) beginKeyCapture(action);
            }
            case COLOR_BLIND -> toggleColorBlindMode();
            case SAVE -> save();
            case RESET_DEFAULT -> resetToDefault();
            case RESET_SCOREBOARD -> resetScoreboard();
            case BACK -> backToMainMenu();
        }
    }

    private Node node(Item item) {
        return switch (item.type()) {
            case SCREEN_SIZE -> view.getScreenSizeComboBox();
            case THEME -> view.getThemeComboBox();
            case DIFFICULTY -> view.getDifficultyComboBox();
            case KEY -> view.getKeyButton(item.keyName());
            case COLOR_BLIND -> view.getColorBlindButton();
            case SAVE -> view.getSaveButton();
            case RESET_DEFAULT -> view.getResetDefaultButton();
            case RESET_SCOREBOARD -> view.getResetScoreboardButton();
            case BACK -> view.getBackButton();
        };
    }

    private void updateSelection() {
        for (int i = 0; i < items.size(); i++) {
            Node node = node(items.get(i));
            if (node == null) continue;

            node.setStyle(
                "-fx-border-color:"
                + (i == selected ? "#4A90E2" : "#B5B5B5")
                + ";-fx-border-width:"
                + (i == selected ? "2px;" : "1px;")
            );
        }
    }

    private void beginKeyCapture(KeyAction action) {
        waitingKey = action;
        pendingKey = null;

        view.setKeyText(action.name(), "");
        view.showMessage(action.name() + " : 새 키를 입력하세요.");
    }

    private void capture(KeyCode key) {
        if (key == KeyCode.ENTER && pendingKey != null) {
            confirmKey();
            return;
        }

        pendingKey = key;
        view.setKeyText(waitingKey.name(), key.getName());
        view.showMessage(waitingKey.name() + " : ENTER를 눌러 확정");
    }

    private void confirmKey() {
        if (waitingKey == null || pendingKey == null) return;

        if (!settings.setKey(waitingKey, pendingKey)) {
            pendingKey = null;
            view.setKeyText(waitingKey.name(), "");
            view.showMessage("이미 사용 중인 키입니다.");
            return;
        }

        view.setKeyText(waitingKey.name(), pendingKey.getName());
        waitingKey = null;
        pendingKey = null;
    }

    private void toggleColorBlindMode() {
        boolean value = !settings.isColorBlindMode();
        settings.setColorBlindMode(value);
        view.setColorBlindMode(value);
        view.showMessage(value ? "색맹 모드 ON" : "색맹 모드 OFF");
    }

    public void show() {
        waitingKey = null;
        pendingKey = null;
        selected = 0;

        render();
        scenes.showSettings();
        updateSelection();
    }

    public void handleKey(KeyCode key) {
        if (waitingKey != null) capture(key);
    }

    public boolean changeKey(KeyAction action, KeyCode key) {
        boolean changed = settings.setKey(action, key);

        if (changed) {
            view.setKeyText(action.name(), key.getName());
        } else {
            view.showMessage("이미 사용 중인 키입니다.");
        }

        return changed;
    }

    public void changeScreenSize(ScreenSize value) {
        settings.setScreenSize(value);
        scenes.applyScreenSize(value);
        view.showMessage("화면 크기: " + value.name());
    }

    public void changeDifficulty(Difficulty value) {
        if (value == null) return;

        settings.setDifficulty(value);
        view.showMessage("난이도: " + value.name());
    }

    public void resetScoreboard() {
        scoreboard.resetScoreboard();
        view.showMessage("스코어보드를 초기화했습니다.");
    }

    public void resetToDefault() {
        Settings defaults = service.resetToDefault();
        settings.copyFrom(defaults);

        waitingKey = null;
        pendingKey = null;

        scenes.applyScreenSize(settings.getScreenSize());
        scenes.applyTheme(settings.getTheme());

        render();
        updateSelection();

        view.showMessage("기본 설정으로 복구했습니다.");
    }

    public void save() {
        if (waitingKey != null) {
            if (pendingKey == null) {
                view.showMessage("새 키를 먼저 입력해주세요.");
                return;
            }

            confirmKey();

            if (waitingKey != null) return;
        }

        view.showMessage(
            service.save(settings)
                ? "설정을 저장했습니다."
                : "설정 저장에 실패했습니다."
        );
    }

    public void backToMainMenu() {
        waitingKey = null;
        pendingKey = null;
        scenes.showMainMenu();
    }

    private void render() {
        view.getScreenSizeComboBox().setValue(
            settings.getScreenSize().name()
        );

        view.getThemeComboBox().setValue(
            settings.getTheme().name()
        );

        updatingDifficulty = true;
        view.setDifficulty(settings.getDifficulty());
        updatingDifficulty = false;

        view.setColorBlindMode(settings.isColorBlindMode());

        for (KeyAction action : KeyAction.values()) {
            KeyCode key = settings.getKey(action);

            if (key != null) {
                view.setKeyText(action.name(), key.getName());
            }
        }
    }
}