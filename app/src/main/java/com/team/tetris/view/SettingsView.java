package com.team.tetris.view;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class SettingsView {

    private final VBox root;

    private final ComboBox<String> screenSizeComboBox;
    private final CheckBox colorBlindCheckBox;

    private final Map<String, Button> keyButtons;

    private final Button resetScoreboardButton;
    private final Button resetDefaultButton;
    private final Button saveButton;
    private final Button backButton;

    private final Label messageLabel;

    public SettingsView() {

        root = new VBox(18);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titleLabel = new Label("SETTINGS");
        titleLabel.getStyleClass().add("title");

        /*
         * SCREEN SIZE
         */
        Label screenSizeTitle = new Label("화면 크기");

        screenSizeComboBox = new ComboBox<>();
        screenSizeComboBox.getItems().addAll(
            "SMALL",
            "MEDIUM",
            "LARGE"
        );
        screenSizeComboBox.setValue("MEDIUM");

        /*
         * KEY SETTINGS
         */
        Label keySettingsTitle = new Label("키 설정");

        GridPane keyGrid = new GridPane();
        keyGrid.setHgap(20);
        keyGrid.setVgap(8);
        keyGrid.setAlignment(Pos.CENTER);

        keyButtons = new LinkedHashMap<>();

        int row = 0;

        row = addKeyRow(keyGrid, row, "MOVE_LEFT", "A");
        row = addKeyRow(keyGrid, row, "MOVE_RIGHT", "D");
        row = addKeyRow(keyGrid, row, "SOFT_DROP", "S");
        row = addKeyRow(keyGrid, row, "HARD_DROP", "ENTER");
        row = addKeyRow(keyGrid, row, "ROTATE", "P");
        row = addKeyRow(keyGrid, row, "PAUSE", "ESC");

        row++;

        row = addKeyRow(keyGrid, row, "MENU_UP", "W");
        row = addKeyRow(keyGrid, row, "MENU_DOWN", "S");
        addKeyRow(keyGrid, row, "SELECT", "ENTER");

        /*
         * COLOR BLIND MODE
         */
        colorBlindCheckBox =
            new CheckBox("색맹 모드");

        /*
         * OTHER SETTINGS
         */
        resetScoreboardButton =
            new Button("스코어보드 초기화");

        resetDefaultButton =
            new Button("기본 설정 복구");

        saveButton =
            new Button("저장");

        backButton =
            new Button("메인 메뉴");

        messageLabel = new Label();

        root.getChildren().addAll(
            titleLabel,
            screenSizeTitle,
            screenSizeComboBox,
            keySettingsTitle,
            keyGrid,
            colorBlindCheckBox,
            resetScoreboardButton,
            resetDefaultButton,
            saveButton,
            backButton,
            messageLabel
        );
    }

    private int addKeyRow(
        GridPane grid,
        int row,
        String action,
        String defaultKey
    ) {

        Label actionLabel = new Label(action);

        Button keyButton = new Button(defaultKey);
        keyButton.setPrefWidth(100);

        keyButtons.put(action, keyButton);

        grid.add(actionLabel, 0, row);
        grid.add(keyButton, 1, row);

        return row + 1;
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

    public Button getKeyButton(String action) {
        return keyButtons.get(action);
    }

    public void setKeyText(
        String action,
        String keyName
    ) {
        Button button = keyButtons.get(action);

        if (button != null) {
            button.setText(keyName);
        }
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