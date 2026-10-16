package com.team.tetris.controller;

import java.util.ArrayList;
import java.util.List;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.service.SettingsService;
import com.team.tetris.util.SceneManager;
import com.team.tetris.view.SettingsView;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SettingsController {

    private enum NavigationType {
        SCREEN_SIZE,
        DIFFICULTY,
        KEY,
        COLOR_BLIND,
        SAVE,
        RESET_DEFAULT,
        RESET_SCOREBOARD,
        BACK
    }

    private static class NavigationItem {

        private final NavigationType type;
        private final String keyActionName;

        private NavigationItem(
            NavigationType type
        ) {
            this(type, null);
        }

        private NavigationItem(
            NavigationType type,
            String keyActionName
        ) {
            this.type = type;
            this.keyActionName = keyActionName;
        }
    }

    private final SettingsView settingsView;
    private final SettingsService settingsService;
    private final ScoreboardService scoreboardService;
    private final SceneManager sceneManager;
    private final Settings settings;

    private final List<NavigationItem> navigationItems =
        new ArrayList<>();

    /*
     * 키 변경 중인 Action
     */
    private KeyAction waitingForKeyAction;

    /*
     * Enter로 확정하기 전의 새 키
     */
    private KeyCode pendingKeyCode;

    private int selectedIndex;

    /*
     * 저장된 난이도를 View에 표시할 때
     * changeDifficulty가 다시 호출되는 것을 방지
     */
    private boolean updatingDifficultyView;

    public SettingsController(
        SettingsView settingsView,
        SettingsService settingsService,
        ScoreboardService scoreboardService,
        SceneManager sceneManager,
        Settings settings
    ) {

        this.settingsView = settingsView;
        this.settingsService = settingsService;
        this.scoreboardService = scoreboardService;
        this.sceneManager = sceneManager;
        this.settings = settings;

        createNavigationItems();

        connectActions();
        connectKeySettings();
        connectKeyboardNavigation();
        connectFocusTracking();

        renderSettings();

        sceneManager.applyScreenSize(
            settings.getScreenSize()
        );
    }

    /*
     * ↑ / ↓ 이동 순서
     */
    private void createNavigationItems() {

        navigationItems.add(
            new NavigationItem(
                NavigationType.SCREEN_SIZE
            )
        );

        navigationItems.add(
            new NavigationItem(
                NavigationType.DIFFICULTY
            )
        );

        for (
            String actionName :
            settingsView.getKeyActionNames()
        ) {

            navigationItems.add(
                new NavigationItem(
                    NavigationType.KEY,
                    actionName
                )
            );
        }

        navigationItems.add(
            new NavigationItem(
                NavigationType.COLOR_BLIND
            )
        );

        navigationItems.add(
            new NavigationItem(
                NavigationType.SAVE
            )
        );

        navigationItems.add(
            new NavigationItem(
                NavigationType.RESET_DEFAULT
            )
        );

        navigationItems.add(
            new NavigationItem(
                NavigationType.RESET_SCOREBOARD
            )
        );

        navigationItems.add(
            new NavigationItem(
                NavigationType.BACK
            )
        );
    }

    /*
     * MOUSE / COMBOBOX
     */
    private void connectActions() {

        settingsView
            .getScreenSizeComboBox()
            .setOnAction(event -> {

                String value =
                    settingsView
                        .getScreenSizeComboBox()
                        .getValue();

                if (value != null) {

                    changeScreenSize(
                        ScreenSize.valueOf(
                            value
                        )
                    );
                }
            });

        settingsView
            .setOnDifficultyChanged(
                difficulty -> {

                    if (
                        !updatingDifficultyView
                    ) {

                        changeDifficulty(
                            difficulty
                        );
                    }
                }
            );

        settingsView
            .getColorBlindButton()
            .setOnAction(
                event ->
                    toggleColorBlindMode()
            );

        settingsView
            .getSaveButton()
            .setOnAction(
                event -> save()
            );

        settingsView
            .getResetDefaultButton()
            .setOnAction(
                event -> resetToDefault()
            );

        settingsView
            .getResetScoreboardButton()
            .setOnAction(
                event -> resetScoreboard()
            );

        settingsView
            .getBackButton()
            .setOnAction(
                event -> backToMainMenu()
            );
    }

    /*
     * KEY SETTING BUTTONS
     */
    private void connectKeySettings() {

        for (
            String actionName :
            settingsView.getKeyActionNames()
        ) {

            Button button =
                settingsView.getKeyButton(
                    actionName
                );

            if (button == null) {
                continue;
            }

            KeyAction action =
                findKeyAction(
                    actionName
                );

            if (action == null) {

                button.setOnAction(
                    event ->
                        settingsView.showMessage(
                            actionName
                            + " : 아직 설정에 연결되지 않았습니다."
                        )
                );

                continue;
            }

            button.setOnAction(
                event ->
                    beginKeyCapture(
                        action
                    )
            );
        }
    }

    private KeyAction findKeyAction(
        String actionName
    ) {

        for (
            KeyAction action :
            KeyAction.values()
        ) {

            if (
                action.name()
                    .equals(actionName)
            ) {

                return action;
            }
        }

        return null;
    }

    /*
     * 키 변경 시작
     */
    private void beginKeyCapture(
        KeyAction action
    ) {

        if (
            waitingForKeyAction
                != null
        ) {

            if (
                pendingKeyCode
                    != null
            ) {

                confirmPendingKey();

                if (
                    waitingForKeyAction
                        != null
                ) {
                    return;
                }

            } else {

                KeyCode oldKey =
                    settings.getKey(
                        waitingForKeyAction
                    );

                if (oldKey != null) {

                    settingsView.setKeyText(
                        waitingForKeyAction.name(),
                        oldKey.getName()
                    );
                }
            }
        }

        waitingForKeyAction =
            action;

        pendingKeyCode =
            null;

        settingsView.setKeyText(
            action.name(),
            ""
        );

        settingsView.showMessage(
            action.name()
            + " : 새 키를 누른 뒤 저장해주세요."
        );
    }

    /*
     * SETTINGS KEYBOARD CONTROL
     */
    private void connectKeyboardNavigation() {

        settingsView
            .getRoot()
            .addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {

                    /*
                     * 키 변경 모드
                     */
                    if (
                        waitingForKeyAction
                            != null
                    ) {

                        handleKeyCapture(
                            event.getCode()
                        );

                        event.consume();
                        return;
                    }

                    /*
                     * ComboBox가 열려 있으면
                     * ComboBox 자체가 키를 처리
                     */
                    if (
                        settingsView
                            .getScreenSizeComboBox()
                            .isShowing()
                        ||
                        settingsView
                            .getDifficultyComboBox()
                            .isShowing()
                    ) {

                        return;
                    }

                    KeyCode key =
                        event.getCode();

                    if (
                        key == KeyCode.UP
                    ) {

                        moveSelection(-1);
                        event.consume();

                    } else if (
                        key == KeyCode.DOWN
                    ) {

                        moveSelection(1);
                        event.consume();

                    } else if (
                        key == KeyCode.ENTER
                    ) {

                        activateCurrentItem();
                        event.consume();
                    }
                }
            );
    }

    /*
     * 키 변경 과정
     */
    private void handleKeyCapture(
        KeyCode key
    ) {

        if (
            key == KeyCode.ENTER
            && pendingKeyCode != null
        ) {

            confirmPendingKey();
            return;
        }

        pendingKeyCode =
            key;

        settingsView.setKeyText(
            waitingForKeyAction.name(),
            key.getName()
        );

        settingsView.showMessage(
            waitingForKeyAction.name()
            + " : ENTER를 눌러 확정"
        );
    }

    private void confirmPendingKey() {

        if (
            waitingForKeyAction == null
            || pendingKeyCode == null
        ) {
            return;
        }

        KeyAction action =
            waitingForKeyAction;

        KeyCode key =
            pendingKeyCode;

        boolean changed =
            settings.setKey(
                action,
                key
            );

        if (!changed) {

            pendingKeyCode =
                null;

            settingsView.setKeyText(
                action.name(),
                ""
            );

            settingsView.showMessage(
                "이미 사용 중인 키입니다."
            );

            return;
        }

        settingsView.setKeyText(
            action.name(),
            key.getName()
        );

        settingsView.showMessage(
            action.name()
            + " → "
            + key.getName()
        );

        waitingForKeyAction =
            null;

        pendingKeyCode =
            null;
    }

    /*
     * FOCUS / SELECTION
     */
    private void connectFocusTracking() {

        int index = 0;

        addFocusTracking(
            settingsView
                .getScreenSizeComboBox(),
            index++
        );

        addFocusTracking(
            settingsView
                .getDifficultyComboBox(),
            index++
        );

        for (
            String actionName :
            settingsView.getKeyActionNames()
        ) {

            Button button =
                settingsView.getKeyButton(
                    actionName
                );

            if (button != null) {

                addFocusTracking(
                    button,
                    index
                );
            }

            index++;
        }

        addFocusTracking(
            settingsView
                .getColorBlindButton(),
            index++
        );

        addFocusTracking(
            settingsView
                .getSaveButton(),
            index++
        );

        addFocusTracking(
            settingsView
                .getResetDefaultButton(),
            index++
        );

        addFocusTracking(
            settingsView
                .getResetScoreboardButton(),
            index++
        );

        addFocusTracking(
            settingsView
                .getBackButton(),
            index
        );
    }

    private void addFocusTracking(
        Node node,
        int index
    ) {

        node.focusedProperty()
            .addListener(
                (
                    observable,
                    oldValue,
                    newValue
                ) -> {

                    if (newValue) {

                        selectedIndex =
                            index;

                        updateSelectionStyle();
                    }
                }
            );
    }

    /*
     * 선택된 항목을 직접 표시
     */
    private void updateSelectionStyle() {

        for (
            int i = 0;
            i < navigationItems.size();
            i++
        ) {

            Node node =
                getNavigationNode(
                    navigationItems.get(i)
                );

            if (node == null) {
                continue;
            }

            if (
                i == selectedIndex
            ) {

                node.setStyle(
                    "-fx-background-color: linear-gradient(#f4f4f4, #dddddd);"
                    + "-fx-border-color: #4A90E2;"
                    + "-fx-border-width: 2px;"
                    + "-fx-border-radius: 3px;"
                    + "-fx-background-radius: 3px;"
                    + "-fx-text-fill: black;"
                    + "-fx-focus-color: transparent;"
                    + "-fx-faint-focus-color: transparent;"
                );

            } else {

                node.setStyle(
                    "-fx-background-color: linear-gradient(#f4f4f4, #dddddd);"
                    + "-fx-border-color: #b5b5b5;"
                    + "-fx-border-width: 1px;"
                    + "-fx-border-radius: 3px;"
                    + "-fx-background-radius: 3px;"
                    + "-fx-text-fill: black;"
                    + "-fx-focus-color: transparent;"
                    + "-fx-faint-focus-color: transparent;"
                );
            }
        }
    }

    private Node getNavigationNode(
        NavigationItem item
    ) {

        return switch (
            item.type
        ) {

            case SCREEN_SIZE ->
                settingsView
                    .getScreenSizeComboBox();

            case DIFFICULTY ->
                settingsView
                    .getDifficultyComboBox();

            case KEY ->
                settingsView
                    .getKeyButton(
                        item.keyActionName
                    );

            case COLOR_BLIND ->
                settingsView
                    .getColorBlindButton();

            case SAVE ->
                settingsView
                    .getSaveButton();

            case RESET_DEFAULT ->
                settingsView
                    .getResetDefaultButton();

            case RESET_SCOREBOARD ->
                settingsView
                    .getResetScoreboardButton();

            case BACK ->
                settingsView
                    .getBackButton();
        };
    }

    /*
     * ↑ / ↓
     */
    private void moveSelection(
        int direction
    ) {

        selectedIndex +=
            direction;

        if (
            selectedIndex < 0
        ) {

            selectedIndex =
                navigationItems.size()
                - 1;

        } else if (
            selectedIndex
                >= navigationItems.size()
        ) {

            selectedIndex =
                0;
        }

        focusCurrentItem();
    }

    private void focusCurrentItem() {

        /*
         * JavaFX 기본 focus 스타일에 의존하지 않고
         * 선택 상태를 직접 표시
         */
        updateSelectionStyle();
    }

    /*
     * ENTER
     */
    private void activateCurrentItem() {

        NavigationItem item =
            navigationItems.get(
                selectedIndex
            );

        switch (
            item.type
        ) {

            case SCREEN_SIZE ->

                settingsView
                    .getScreenSizeComboBox()
                    .show();

            case DIFFICULTY ->

                settingsView
                    .getDifficultyComboBox()
                    .show();

            case KEY -> {

                KeyAction action =
                    findKeyAction(
                        item.keyActionName
                    );

                if (
                    action == null
                ) {

                    settingsView.showMessage(
                        item.keyActionName
                        + " : 아직 설정에 연결되지 않았습니다."
                    );

                } else {

                    beginKeyCapture(
                        action
                    );
                }
            }

            case COLOR_BLIND ->

                toggleColorBlindMode();

            case SAVE ->

                save();

            case RESET_DEFAULT ->

                resetToDefault();

            case RESET_SCOREBOARD ->

                resetScoreboard();

            case BACK ->

                backToMainMenu();
        }
    }

    /*
     * COLOR BLIND
     */
    private void toggleColorBlindMode() {

        boolean enabled =
            !settings.isColorBlindMode();

        settings.setColorBlindMode(
            enabled
        );

        settingsView.setColorBlindMode(
            enabled
        );

        settingsView.showMessage(
            enabled
                ? "색맹 모드 ON"
                : "색맹 모드 OFF"
        );
    }

    /*
     * SHOW
     */
    public void show() {

        waitingForKeyAction =
            null;

        pendingKeyCode =
            null;

        selectedIndex =
            0;

        renderSettings();

        sceneManager.showSettings();

        settingsView
            .getRoot()
            .setFocusTraversable(true);

        settingsView
            .getRoot()
            .requestFocus();

        updateSelectionStyle();
    }

    /*
     * 기존 API
     */
    public void handleKey(
        KeyCode key
    ) {

        if (
            waitingForKeyAction
                != null
        ) {

            handleKeyCapture(
                key
            );
        }
    }

    public boolean changeKey(
        KeyAction action,
        KeyCode key
    ) {

        boolean changed =
            settings.setKey(
                action,
                key
            );

        if (!changed) {

            settingsView.showMessage(
                "이미 사용 중인 키입니다."
            );

            return false;
        }

        settingsView.setKeyText(
            action.name(),
            key.getName()
        );

        return true;
    }

    /*
     * SCREEN SIZE
     */
    public void changeScreenSize(
        ScreenSize screenSize
    ) {

        settings.setScreenSize(
            screenSize
        );

        sceneManager.applyScreenSize(
            screenSize
        );

        settingsView.showMessage(
            "화면 크기: "
            + screenSize.name()
        );
    }

    /*
     * DIFFICULTY
     */
    public void changeDifficulty(
        Difficulty difficulty
    ) {

        if (
            difficulty == null
        ) {
            return;
        }

        settings.setDifficulty(
            difficulty
        );

        settingsView.showMessage(
            "난이도: "
            + difficulty.name()
        );
    }

    /*
     * SCOREBOARD RESET
     */
    public void resetScoreboard() {

        scoreboardService
            .resetScoreboard();

        settingsView.showMessage(
            "스코어보드를 초기화했습니다."
        );
    }

    /*
     * DEFAULT
     */
    public void resetToDefault() {

        waitingForKeyAction =
            null;

        pendingKeyCode =
            null;

        Settings defaults =
            settingsService
                .resetToDefault();

        copySettings(
            defaults,
            settings
        );

        renderSettings();

        sceneManager.applyScreenSize(
            settings.getScreenSize()
        );

        updateSelectionStyle();

        settingsView.showMessage(
            "기본 설정으로 복구했습니다."
        );
    }

    /*
     * SAVE
     */
    public void save() {

        if (
            waitingForKeyAction
                != null
        ) {

            if (
                pendingKeyCode
                    == null
            ) {

                settingsView.showMessage(
                    "새 키를 먼저 입력해주세요."
                );

                return;
            }

            confirmPendingKey();

            if (
                waitingForKeyAction
                    != null
            ) {
                return;
            }
        }

        if (
            settingsService.save(
                settings
            )
        ) {

            renderSettings();
            updateSelectionStyle();

            settingsView.showMessage(
                "설정을 저장했습니다."
            );

        } else {

            settingsView.showMessage(
                "설정 저장에 실패했습니다."
            );
        }
    }

    /*
     * BACK
     */
    public void backToMainMenu() {

        waitingForKeyAction =
            null;

        pendingKeyCode =
            null;

        sceneManager.showMainMenu();
    }

    /*
     * SETTINGS -> VIEW
     */
    private void renderSettings() {

        settingsView
            .getScreenSizeComboBox()
            .setValue(
                settings
                    .getScreenSize()
                    .name()
            );

        Difficulty difficulty =
            settings.getDifficulty();

        if (
            difficulty != null
        ) {

            updatingDifficultyView =
                true;

            settingsView.setDifficulty(
                difficulty
            );

            updatingDifficultyView =
                false;
        }

        settingsView.setColorBlindMode(
            settings
                .isColorBlindMode()
        );

        for (
            KeyAction action :
            KeyAction.values()
        ) {

            KeyCode key =
                settings.getKey(
                    action
                );

            if (
                key != null
            ) {

                settingsView.setKeyText(
                    action.name(),
                    key.getName()
                );
            }
        }
    }

    private void copySettings(
        Settings source,
        Settings target
    ) {

        target.copyFrom(
            source
        );
    }
}