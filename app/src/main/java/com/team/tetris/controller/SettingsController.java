package com.team.tetris.controller;

import com.team.tetris.util.SceneManager;
import com.team.tetris.view.SettingsView;

import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SettingsController {

    private final SettingsView settingsView;
    private final SceneManager sceneManager;

    private final Runnable resetScoreboardAction;
    private final Runnable saveSettingsAction;
    private final Runnable resetDefaultAction;

    private String waitingForKeyAction;

    public SettingsController(
        SettingsView settingsView,
        SceneManager sceneManager,
        Runnable resetScoreboardAction,
        Runnable saveSettingsAction,
        Runnable resetDefaultAction
    ) {
        this.settingsView = settingsView;
        this.sceneManager = sceneManager;
        this.resetScoreboardAction = resetScoreboardAction;
        this.saveSettingsAction = saveSettingsAction;
        this.resetDefaultAction = resetDefaultAction;

        connectActions();
        connectKeySettings();
    }

    private void connectActions() {

        settingsView.getScreenSizeComboBox()
            .setOnAction(event ->
                changeScreenSize(
                    settingsView.getScreenSizeComboBox().getValue()
                )
            );

        settingsView.getColorBlindCheckBox()
            .setOnAction(event ->
                setColorBlindMode(
                    settingsView.getColorBlindCheckBox().isSelected()
                )
            );

        settingsView.getResetScoreboardButton()
           
        .setOnAction(event -> resetScoreboard());

        settingsView.getResetDefaultButton()
            .setOnAction(event -> resetToDefault());

        settingsView.getSaveButton()
            .setOnAction(event -> save());

        settingsView.getBackButton()
            .setOnAction(event -> backToMainMenu());
    }

    private void connectKeySettings() {

        String[] actions = {
            "MOVE_LEFT",       
            "MOVE_RIGHT",
            "SOFT_DROP",
            "HARD_DROP",
            "ROTATE",
            "PAUSE",
            "MENU_UP",
            "MENU_DOWN",
            "SELECT"
        };

        for (String action : actions) {

            Button button =
                settingsView.getKeyButton(action);

            if (button != null) {
                button.setOnAction(event -> {
                    waitingForKeyAction = action;
                    settingsView.showMessage(
                        action + " : 새 키를 누르세요"
                    );
                });
            }
        }

        settingsView.getRoot().addEventFilter(
    KeyEvent.KEY_PRESSED,
    event -> {
        if (waitingForKeyAction != null) {
            handleKey(event.getCode());
            event.consume();
        }
    }
);
    }

    public void show() {
        waitingForKeyAction = null;
        sceneManager.showSettings();
    }

    public void handleKey(KeyCode key) {

        if (waitingForKeyAction == null) {
            return;
        }

        settingsView.setKeyText(
            waitingForKeyAction,
            key.getName()
        );

        settingsView.showMessage(
            waitingForKeyAction
            + " → "
            + key.getName()
        );

        waitingForKeyAction = null;
    }

    public void changeScreenSize(String screenSize) {

        switch (screenSize) {

            case "SMALL":
                sceneManager.getStage().setWidth(640);
                sceneManager.getStage().setHeight(720);
                break;

            case "LARGE":
                sceneManager.getStage().setWidth(1000);
                sceneManager.getStage().setHeight(900);
                break;

            case "MEDIUM":
            default:
                sceneManager.getStage().setWidth(800);
                sceneManager.getStage().setHeight(800);
                break;
        }

        settingsView.showMessage(
            "화면 크기: " + screenSize
        );
    }

    public void setColorBlindMode(boolean enabled) {

        if (enabled) {
            settingsView.showMessage("색맹 모드 ON");
        } else {
            settingsView.showMessage("색맹 모드 OFF");
        }
    }

    public void resetScoreboard() {

        if (resetScoreboardAction != null) {
            resetScoreboardAction.run();
        }

        settingsView.showMessage(
            "스코어보드를 초기화했습니다."
        );
    }

    public void resetToDefault() {

        settingsView.getScreenSizeComboBox()
            .setValue("MEDIUM");

        settingsView.getColorBlindCheckBox()
            .setSelected(false);

        settingsView.setKeyText("MOVE_LEFT", "A");
        settingsView.setKeyText("MOVE_RIGHT", "D");
        settingsView.setKeyText("SOFT_DROP", "S");
        settingsView.setKeyText("HARD_DROP", "ENTER");
        settingsView.setKeyText("ROTATE", "P");
        settingsView.setKeyText("PAUSE", "ESC");

        settingsView.setKeyText("MENU_UP", "W");
        settingsView.setKeyText("MENU_DOWN", "S");
        settingsView.setKeyText("SELECT", "ENTER");

        changeScreenSize("MEDIUM");

        if (resetDefaultAction != null) {
            resetDefaultAction.run();
        }

        settingsView.showMessage(
            "기본 설정으로 복구했습니다."
        );
    }

    public void save() {

        if (saveSettingsAction != null) {
            saveSettingsAction.run();
        }

        settingsView.showMessage(
            "설정을 저장했습니다."
        );
    }

    public void backToMainMenu() {
        waitingForKeyAction = null;
        sceneManager.showMainMenu();
    }
}