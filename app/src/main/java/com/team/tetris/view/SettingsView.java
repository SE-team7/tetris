package com.team.tetris.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SettingsView {

    private final VBox root;

    private final ComboBox<String> screenSizeComboBox;
    private final CheckBox colorBlindCheckBox;

    private final Button keySettingsButton;
    private final Button resetScoreboardButton;
    private final Button resetDefaultButton;
    private final Button saveButton;
    private final Button backButton;

    private final Label messageLabel;

    public SettingsView() {
        root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        Label titleLabel = new Label("SETTINGS");
        titleLabel.getStyleClass().add("title");

        Label screenSizeLabel = new Label("화면 크기");

        screenSizeComboBox = new ComboBox<>();
        screenSizeComboBox.getItems().addAll(
            "SMALL",
            "MEDIUM",
            "LARGE"
        );
        screenSizeComboBox.setValue("MEDIUM");

        colorBlindCheckBox = new CheckBox("색맹 모드");

        keySettingsButton = new Button("키 설정");
        resetScoreboardButton = new Button("스코어보드 초기화");
        resetDefaultButton = new Button("기본 설정 복구");
        saveButton = new Button("저장");
        backButton = new Button("메인 메뉴");

        messageLabel = new Label();

        root.getChildren().addAll(
            titleLabel,
            screenSizeLabel,
            screenSizeComboBox,
            keySettingsButton,
            colorBlindCheckBox,
            resetScoreboardButton,
            resetDefaultButton,
            saveButton,
            backButton,
            messageLabel
        );
    }

    public Parent getRoot() {
        return root;
    }

    public ComboBox<String> getScreenSizeComboBox() {
        return screenSizeComboBox;
    }

    public CheckBox getColorBlindCheckBox() {
        return colorBlindCheckBox;
    }

    public Button getKeySettingsButton() {
        return keySettingsButton;
    }

    public Button getResetScoreboardButton() {
        return resetScoreboardButton;
    }

    public Button getResetDefaultButton() {
        return resetDefaultButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public void showMessage(String message) {
        messageLabel.setText(message);
    }
}