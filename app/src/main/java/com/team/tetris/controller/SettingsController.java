package com.team.tetris.controller;

import java.util.ArrayList;
import java.util.List;

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
            this.keyActionName =
                keyActionName;
        }
    }

    private final SettingsView settingsView;
    private final SettingsService settingsService;
    private final ScoreboardService scoreboardService;
    private final SceneManager sceneManager;
    private final Settings settings;

    private final List<NavigationItem>
        navigationItems =
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

    public SettingsController(
        SettingsView settingsView,
        SettingsService settingsService,
        ScoreboardService scoreboardService,
        SceneManager sceneManager,
        Settings settings
    ) {

        this.settingsView =
            settingsView;

        this.settingsService =
            settingsService;

        this.scoreboardService =
            scoreboardService;

        this.sceneManager =
            sceneManager;

        this.settings =
            settings;

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
     * MOUSE
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

        /*
         * 색맹 모드:
         * 버튼 하나를 누를 때마다
         * ON / OFF 전환
         */
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

            /*
             * SKIP이 아직 Settings enum에
             * 없는 경우에도 compile 가능.
             */
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
     *
     * Enter를 누르면 기존 글자 삭제.
     */
    private void beginKeyCapture(
        KeyAction action
    ) {

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
            + " : 새 키를 누르세요"
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
                     * Screen Size 목록이
                     * 열려 있을 때는
                     * ComboBox가 직접 처리
                     */
                    if (
                        settingsView
                            .getScreenSizeComboBox()
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
     *
     * ENTER
     * -> 빈 칸
     *
     * H
     * -> H 표시
     *
     * ENTER
     * -> 확정
     */
    private void handleKeyCapture(
        KeyCode key
    ) {

        pendingKeyCode =
            key;
        
        settingsView.setKeyText(
        waitingForKeyAction.name(),
        key.getName()
        );

        settingsView.showMessage(
            waitingForKeyAction.name()
            + " : 저장 버튼을 눌러 적용하세요."
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
                action.name()
                + " : 이미 사용 중인 키입니다."
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
     * FOCUS
     */
    private void connectFocusTracking() {

        int index = 0;

        addFocusTracking(
            settingsView
                .getScreenSizeComboBox(),
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
                    }
                }
            );
    }

    /*
     * ↑ / ↓
     */
    private void moveSelection(
        int direction
    ) {

        selectedIndex +=
            direction;

        if (selectedIndex < 0) {

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

        NavigationItem item =
            navigationItems.get(
                selectedIndex
            );

        switch (item.type) {

            case SCREEN_SIZE ->

                settingsView
                    .getScreenSizeComboBox()
                    .requestFocus();

            case KEY -> {

                Button button =
                    settingsView.getKeyButton(
                        item.keyActionName
                    );

                if (button != null) {
                    button.requestFocus();
                }
            }

            case COLOR_BLIND ->

                settingsView
                    .getColorBlindButton()
                    .requestFocus();

            case SAVE ->

                settingsView
                    .getSaveButton()
                    .requestFocus();

            case RESET_DEFAULT ->

                settingsView
                    .getResetDefaultButton()
                    .requestFocus();

            case RESET_SCOREBOARD ->

                settingsView
                    .getResetScoreboardButton()
                    .requestFocus();

            case BACK ->

                settingsView
                    .getBackButton()
                    .requestFocus();
        }
    }

    /*
     * ENTER
     */
    private void activateCurrentItem() {

        NavigationItem item =
            navigationItems.get(
                selectedIndex
            );

        switch (item.type) {

            case SCREEN_SIZE ->

                settingsView
                    .getScreenSizeComboBox()
                    .show();

            case KEY -> {

                KeyAction action =
                    findKeyAction(
                        item.keyActionName
                    );

                if (action == null) {

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
            .getScreenSizeComboBox()
            .requestFocus();
    }

    /*
     * 기존 API
     */
    public void handleKey(
        KeyCode key
    ) {

        if (
            waitingForKeyAction != null
        ) {

            handleKeyCapture(key);
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
                action.name()
                + " : 이미 사용 중인 키입니다."
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

        settingsView.showMessage(
            "기본 설정으로 복구했습니다."
        );
    }

    /*
     * SAVE
     *
     * 여기서 파일에 저장
     */
    public void save() {
        if (waitingForKeyAction != null) {
        if (pendingKeyCode == null) {
            settingsView.showMessage("새 키를 먼저 입력해주세요.");
            return;
        }

        confirmPendingKey();

        if (waitingForKeyAction != null) {
            return;
        }
    }

        settingsService.save(
            settings
        );

        settingsView.showMessage(
            "설정을 저장했습니다."
        );
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

        settingsView
            .setColorBlindMode(
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

            if (key != null) {

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

        /*
         * 키를 하나씩 setKey로 복사하면
         * 서로 바꾼 키가 중복 검사에 걸려 복구되지 않으므로 한 번에 복사
         */
        target.copyFrom(
            source
        );
    }
}