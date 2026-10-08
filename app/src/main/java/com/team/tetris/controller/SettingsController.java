package com.team.tetris.controller;

import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.service.SettingsService;
import com.team.tetris.util.SceneManager;
import com.team.tetris.view.SettingsView;

import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SettingsController {

    private final SettingsView settingsView;
    private final SettingsService settingsService;
    private final ScoreboardService scoreboardService;
    private final SceneManager sceneManager;
    private final Settings settings;

    private KeyAction waitingForKeyAction;

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

        connectActions();
        connectKeySettings();

        renderSettings();

        sceneManager.applyScreenSize(
            settings.getScreenSize()
        );
    }

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
                        ScreenSize.valueOf(value)
                    );
                }
            });

        settingsView
            .getColorBlindCheckBox()
            .setOnAction(event ->
                setColorBlindMode(
                    settingsView
                        .getColorBlindCheckBox()
                        .isSelected()
                )
            );

        settingsView
            .getResetScoreboardButton()
            .setOnAction(
                event -> resetScoreboard()
            );

        settingsView
            .getResetDefaultButton()
            .setOnAction(
                event -> resetToDefault()
            );

        settingsView
            .getSaveButton()
            .setOnAction(
                event -> save()
            );

        settingsView
            .getBackButton()
            .setOnAction(
                event -> backToMainMenu()
            );
    }

    private void connectKeySettings() {

        for (KeyAction action
            : KeyAction.values()) {

            Button button =
                settingsView.getKeyButton(
                    action.name()
                );

            if (button != null) {

                button.setOnAction(event -> {

                    waitingForKeyAction =
                        action;

                    settingsView.showMessage(
                        action.name()
                        + " : 새 키를 누르세요"
                    );
                });
            }
        }

        settingsView
            .getRoot()
            .addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {

                    if (waitingForKeyAction
                        != null) {

                        handleKey(
                            event.getCode()
                        );

                        event.consume();
                    }
                }
            );
    }

    public void show() {

        waitingForKeyAction = null;

        renderSettings();

        sceneManager.showSettings();
    }

    public void handleKey(KeyCode key) {

        if (waitingForKeyAction == null) {
            return;
        }

        KeyAction action =
            waitingForKeyAction;

        waitingForKeyAction = null;

        boolean changed =
            changeKey(action, key);

        if (!changed) {

            settingsView.showMessage(
                action.name()
                + " : 이미 사용 중인 키입니다."
            );
        }
    }

    public boolean changeKey(
        KeyAction action,
        KeyCode key
    ) {

        boolean changed =
            settings.setKey(action, key);

        if (!changed) {
            return false;
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

        return true;
    }

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

    public void setColorBlindMode(
        boolean enabled
    ) {

        settings.setColorBlindMode(
            enabled
        );

        settingsView.showMessage(
            enabled
                ? "색맹 모드 ON"
                : "색맹 모드 OFF"
        );
    }

    public void resetScoreboard() {

        scoreboardService
            .resetScoreboard();

        settingsView.showMessage(
            "스코어보드를 초기화했습니다."
        );
    }

    public void resetToDefault() {

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

    public void save() {

        settingsService.save(
            settings
        );

        settingsView.showMessage(
            "설정을 저장했습니다."
        );
    }

    public void backToMainMenu() {

        waitingForKeyAction = null;

        sceneManager.showMainMenu();
    }

    private void renderSettings() {

        settingsView
            .getScreenSizeComboBox()
            .setValue(
                settings
                    .getScreenSize()
                    .name()
            );

        settingsView
            .getColorBlindCheckBox()
            .setSelected(
                settings
                    .isColorBlindMode()
            );

        for (KeyAction action
            : KeyAction.values()) {

            KeyCode key =
                settings.getKey(action);

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

        target.setScreenSize(
            source.getScreenSize()
        );

        target.setColorBlindMode(
            source.isColorBlindMode()
        );

        for (KeyAction action
            : KeyAction.values()) {

            target.setKey(
                action,
                source.getKey(action)
            );
        }
    }
}