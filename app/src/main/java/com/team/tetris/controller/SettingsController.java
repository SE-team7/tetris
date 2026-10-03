package com.team.tetris.controller;

import com.team.tetris.util.SceneManager;
import com.team.tetris.view.SettingsView;

public class SettingsController {

    private final SettingsView settingsView;
    private final SceneManager sceneManager;

    private final Runnable resetScoreboardAction;
    private final Runnable saveSettingsAction;
    private final Runnable resetDefaultAction;

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

    public void show() {
        sceneManager.showSettings();
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
        sceneManager.showMainMenu();
    }
}