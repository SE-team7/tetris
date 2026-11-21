package com.team.tetris.controller;

import java.util.ArrayList;
import java.util.List;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.PlayerId;
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
        KEY_TAB,
        BATTLE_KEY,
        COLOR_BLIND,
        SAVE,
        RESET_DEFAULT,
        RESET_SCOREBOARD,
        BACK
    }

    private record Item(Type type, String keyName, PlayerId player) {
        Item(Type type) { this(type, null, null); }
        Item(Type type, String keyName) { this(type, keyName, null); }
    }

    private final SettingsView view;
    private final SettingsService service;
    private final ScoreboardService scoreboard;
    private final SceneManager scenes;
    private final Settings settings;

    private final List<Item> items = new ArrayList<>();

    private KeyAction waitingKey;
    private PlayerId waitingPlayer;
    private boolean localTabSelected;
    private boolean pendingLocalTab;
    private KeyCode pendingKey;
    private int selected = -1;
    private int playerColumn = 0;
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
        items.clear();
        items.add(new Item(Type.SCREEN_SIZE));
        items.add(new Item(Type.THEME));
        items.add(new Item(Type.DIFFICULTY));
        items.add(new Item(Type.KEY_TAB));
        if (localTabSelected) {
            for (String name : view.getKeyActionNames()) {
                items.add(new Item(Type.BATTLE_KEY, name, PlayerId.PLAYER1));
            }
        } else {
            for (String name : view.getKeyActionNames()) items.add(new Item(Type.KEY, name));
        }
        items.add(new Item(Type.COLOR_BLIND));
        items.add(new Item(Type.SAVE));
        items.add(new Item(Type.RESET_DEFAULT));
        items.add(new Item(Type.RESET_SCOREBOARD));
        items.add(new Item(Type.BACK));
    }

    private void previewTab(boolean local) {
        pendingLocalTab = local;
        updateSelection();
    }

    private void selectTab(boolean local) {
        pendingLocalTab = local;
        localTabSelected = local;
        view.setLocalTab(local);
        createItems();
        selected = 3;
        playerColumn = 0;
        updateSelection();
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
        view.getSingleTab().setOnAction(e -> selectTab(false));
        view.getLocalTab().setOnAction(e -> selectTab(true));
        for (PlayerId player : new PlayerId[]{PlayerId.PLAYER1, PlayerId.PLAYER2}) {
            for (String name : view.getKeyActionNames()) {
                Button button = view.getBattleKeyButton(player, name);
                KeyAction action = findAction(name);
                if (button != null && action != null) {
                    button.setOnAction(e -> beginBattleCapture(player, action));
                }
            }
        }

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
                case LEFT -> {
                    if (selected < 0) return;
                    if (items.get(selected).type() == Type.KEY_TAB) previewTab(false);
                    else if (items.get(selected).type() == Type.BATTLE_KEY) {
                        playerColumn = 0;
                        updateSelection();
                    } else return;
                }
                case RIGHT -> {
                    if (selected < 0) return;
                    if (items.get(selected).type() == Type.KEY_TAB) previewTab(true);
                    else if (items.get(selected).type() == Type.BATTLE_KEY) {
                        playerColumn = 1;
                        updateSelection();
                    } else return;
                }
                case ENTER -> activate();
                default -> { return; }
            }

            e.consume();
        });
    }

    private void connectFocus() {
        track(view.getScreenSizeComboBox(), new Item(Type.SCREEN_SIZE));
        track(view.getThemeComboBox(), new Item(Type.THEME));
        track(view.getDifficultyComboBox(), new Item(Type.DIFFICULTY));
        track(view.getSingleTab(), new Item(Type.KEY_TAB));
        track(view.getLocalTab(), new Item(Type.KEY_TAB));
        for (String name : view.getKeyActionNames()) {
            track(view.getKeyButton(name), new Item(Type.KEY, name));
            for (PlayerId player : new PlayerId[]{PlayerId.PLAYER1, PlayerId.PLAYER2}) {
                track(view.getBattleKeyButton(player, name), new Item(Type.BATTLE_KEY, name, player));
            }
        }
        track(view.getColorBlindButton(), new Item(Type.COLOR_BLIND));
        track(view.getSaveButton(), new Item(Type.SAVE));
        track(view.getResetDefaultButton(), new Item(Type.RESET_DEFAULT));
        track(view.getResetScoreboardButton(), new Item(Type.RESET_SCOREBOARD));
        track(view.getBackButton(), new Item(Type.BACK));
    }

    private void track(Node node, Item item) {
        if (node == null) return;

        node.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused && scenes != null && selected >= 0) {
                int index = items.indexOf(item);
                if (index >= 0) {
                    selected = index;
                    if (item.type() == Type.BATTLE_KEY)
                        playerColumn = item.player() == PlayerId.PLAYER2 ? 1 : 0;
                    updateSelection();
                }
            }
        });
    }

    private void move(int direction) {
        selected = selected < 0
            ? (direction > 0 ? 0 : items.size() - 1)
            : Math.floorMod(selected + direction, items.size());
        updateSelection();
    }

    private void activate() {
        if (selected < 0) return;
        Item item = items.get(selected);

        switch (item.type()) {
            case SCREEN_SIZE -> view.getScreenSizeComboBox().show();
            case THEME -> view.getThemeComboBox().show();
            case DIFFICULTY -> view.getDifficultyComboBox().show();
            case KEY -> {
                KeyAction action = findAction(item.keyName());
                if (action != null) beginKeyCapture(action);
            }
            case KEY_TAB -> selectTab(pendingLocalTab);
            case BATTLE_KEY -> beginBattleCapture(
                playerColumn == 0 ? PlayerId.PLAYER1 : PlayerId.PLAYER2,
                findAction(item.keyName()));
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
            case KEY_TAB -> pendingLocalTab ? view.getLocalTab() : view.getSingleTab();
            case BATTLE_KEY -> view.getBattleKeyButton(
                playerColumn == 0 ? PlayerId.PLAYER1 : PlayerId.PLAYER2,
                item.keyName());
            case COLOR_BLIND -> view.getColorBlindButton();
            case SAVE -> view.getSaveButton();
            case RESET_DEFAULT -> view.getResetDefaultButton();
            case RESET_SCOREBOARD -> view.getResetScoreboardButton();
            case BACK -> view.getBackButton();
        };
    }

    private void updateSelection() {
        // A blue border indicates keyboard focus only, never the active tab.
        view.getSingleTab().setStyle("-fx-border-color: " +
            (selected == 3 && !pendingLocalTab ? "#4A90E2" : "#B5B5B5") +
            ";-fx-border-width:" + (selected == 3 && !pendingLocalTab ? "2px;" : "1px;"));
        view.getLocalTab().setStyle("-fx-border-color: " +
            (selected == 3 && pendingLocalTab ? "#4A90E2" : "#B5B5B5") +
            ";-fx-border-width:" + (selected == 3 && pendingLocalTab ? "2px;" : "1px;"));

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.type() == Type.KEY_TAB) continue;
            if (item.type() == Type.BATTLE_KEY) {
                for (PlayerId player : new PlayerId[]{PlayerId.PLAYER1, PlayerId.PLAYER2}) {
                    Node button = view.getBattleKeyButton(player, item.keyName());
                    if (button != null) {
                        boolean focused = i == selected &&
                            (player == PlayerId.PLAYER1 ? playerColumn == 0 : playerColumn == 1);
                        button.setStyle("-fx-border-color:" +
                            (focused ? "#4A90E2" : "#B5B5B5") +
                            ";-fx-border-width:" + (focused ? "2px;" : "1px;"));
                    }
                }
            } else {
                Node control = node(item);
                if (control != null) {
                    boolean focused = i == selected;
                    control.setStyle("-fx-border-color:" +
                        (focused ? "#4A90E2" : "#B5B5B5") +
                        ";-fx-border-width:" + (focused ? "2px;" : "1px;"));
                }
            }
        }
    }

    private void beginBattleCapture(PlayerId player, KeyAction action) {
        if (action == null) return;
        waitingPlayer = player;
        waitingKey = action;
        pendingKey = null;
        view.setBattleKeyText(player, action.name(), "");
        view.showMessage(player.name() + " / " + action.name() + " : 새 키를 입력하세요.");
    }

    private void beginKeyCapture(KeyAction action) {
        waitingPlayer = null;
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
        if (waitingPlayer == null) view.setKeyText(waitingKey.name(), key.getName());
        else view.setBattleKeyText(waitingPlayer, waitingKey.name(), key.getName());
        view.showMessage(waitingKey.name() + " : ENTER를 눌러 확정");
    }

    private void confirmKey() {
        if (waitingKey == null || pendingKey == null) return;

        boolean accepted = waitingPlayer == null
            ? settings.setKey(waitingKey, pendingKey)
            : setLocalKey(waitingPlayer, waitingKey, pendingKey);
        if (!accepted) {
            pendingKey = null;
            if (waitingPlayer == null) view.setKeyText(waitingKey.name(), "");
            else view.setBattleKeyText(waitingPlayer, waitingKey.name(), "");
            view.showMessage("이미 사용 중이거나 사용할 수 없는 키입니다.");
            return;
        }

        if (waitingPlayer == null) view.setKeyText(waitingKey.name(), pendingKey.getName());
        else view.setBattleKeyText(waitingPlayer, waitingKey.name(), pendingKey.getName());
        waitingPlayer = null;
        waitingKey = null;
        pendingKey = null;
    }

    private boolean setLocalKey(PlayerId player, KeyAction action, KeyCode key) {
        if (Settings.isFixedKey(key)) return false;
        for (PlayerId other : new PlayerId[]{PlayerId.PLAYER1, PlayerId.PLAYER2}) {
            for (KeyAction candidate : KeyAction.values()) {
                if (candidate.isFixed()) continue;
                if (other == player && candidate == action) continue;
                if (settings.getBattleKey(other, candidate) == key) return false;
            }
        }
        return settings.setBattleKey(player, action, key);
    }

    private void toggleColorBlindMode() {
        boolean value = !settings.isColorBlindMode();
        settings.setColorBlindMode(value);
        view.setColorBlindMode(value);
        view.showMessage(value ? "색맹 모드 ON" : "색맹 모드 OFF");
    }

    public void show() {
        waitingKey = null;
        waitingPlayer = null;
        pendingKey = null;
        selected = -1;
        playerColumn = 0;
        pendingLocalTab = localTabSelected;

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
        waitingPlayer = null;
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
        waitingPlayer = null;
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

        for (PlayerId player : new PlayerId[]{PlayerId.PLAYER1, PlayerId.PLAYER2}) {
            for (String name : view.getKeyActionNames()) {
                KeyAction action = findAction(name);
                KeyCode key = settings.getBattleKey(player, action);
                if (key != null) view.setBattleKeyText(player, name, key.getName());
            }
        }
        for (KeyAction action : KeyAction.values()) {
            KeyCode key = settings.getKey(action);

            if (key != null) {
                view.setKeyText(action.name(), key.getName());
            }
        }
    }
}