package com.team.tetris.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.team.tetris.domain.Difficulty;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsView {

    private static final double LABEL_W = 90;
    private static final double BUTTON_W = 100;

    private final VBox root = new VBox(8);
    private final ComboBox<String> screenSizeComboBox = new ComboBox<>();
    private final ComboBox<String> themeComboBox = new ComboBox<>();
    private final ComboBox<Difficulty> difficultyComboBox = new ComboBox<>();
    private final Map<String, Button> keyButtons = new LinkedHashMap<>();

    private final Button colorBlindButton = new Button("OFF");
    private final Button saveButton = new Button("저장");
    private final Button resetDefaultButton = new Button("기본 설정 복구");
    private final Button resetScoreboardButton = new Button("스코어보드 초기화");
    private final Button backButton = new Button("메인 메뉴");
    private final Label messageLabel = new Label();

    public SettingsView() {
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(12));
        root.getStyleClass().add("settings-root");

        Label title = new Label("SETTINGS");
        title.getStyleClass().add("settings-title");

        screenSizeComboBox.getItems().addAll("SMALL", "MEDIUM", "LARGE");
        screenSizeComboBox.setValue("MEDIUM");

        themeComboBox.getItems().addAll("LIGHT", "DARK");
        themeComboBox.setValue("LIGHT");

        difficultyComboBox.getItems().addAll(
            Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD
        );
        difficultyComboBox.setValue(Difficulty.NORMAL);

        screenSizeComboBox.setPrefWidth(150);
        themeComboBox.setPrefWidth(150);
        difficultyComboBox.setPrefWidth(150);

        Label keyTitle = new Label("키 설정");
        keyTitle.getStyleClass().add("settings-section-title");

        GridPane keys = new GridPane();
        keys.setAlignment(Pos.CENTER);
        keys.setHgap(20);
        keys.setVgap(8);

        addKey(keys, 0, "MOVE_LEFT", "A");
        addKey(keys, 1, "MOVE_RIGHT", "D");
        addKey(keys, 2, "SOFT_DROP", "S");
        addKey(keys, 3, "HARD_DROP", "ENTER");
        addKey(keys, 4, "ROTATE", "P");
        addKey(keys, 5, "SKIP", "-");

        colorBlindButton.setPrefWidth(BUTTON_W);

        Label fixed = new Label(
            "고정키 (변경 불가)\n" +
            "SHIFT : 일시정지 / 계속하기\n" +
            "TAB : 메인 메뉴로 돌아가기\n" +
            "ESC : 전체 게임 종료"
        );
        fixed.setAlignment(Pos.CENTER);
        fixed.getStyleClass().add("fixed-key-notice");

        for (Button b : List.of(
            saveButton, resetDefaultButton, resetScoreboardButton, backButton
        )) {
            b.setPrefWidth(180);
        }

        messageLabel.getStyleClass().add("settings-message");

        root.getChildren().addAll(
            title,
            row("화면 크기", screenSizeComboBox),
            row("화면 테마", themeComboBox),
            row("난이도", difficultyComboBox),
            keyTitle,
            keys,
            row("색맹 모드", colorBlindButton),
            fixed,
            saveButton,
            resetDefaultButton,
            resetScoreboardButton,
            backButton,
            messageLabel
        );
    }

    private HBox row(String text, javafx.scene.Node control) {
        Label label = new Label(text);
        label.setMinWidth(LABEL_W);
        label.setPrefWidth(LABEL_W);
        label.getStyleClass().add("settings-label");

        HBox row = new HBox(20, label, control);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private void addKey(GridPane grid, int row, String action, String key) {
        Label label = new Label(action);
        label.setMinWidth(LABEL_W);
        label.setPrefWidth(LABEL_W);

        Button button = new Button(key);
        button.setMinWidth(BUTTON_W);
        button.setPrefWidth(BUTTON_W);

        keyButtons.put(action, button);
        grid.add(label, 0, row);
        grid.add(button, 1, row);
    }

    public Parent getRoot() { return root; }

    public ComboBox<String> getScreenSizeComboBox() {
        return screenSizeComboBox;
    }

    public ComboBox<String> getThemeComboBox() {
        return themeComboBox;
    }

    public ComboBox<Difficulty> getDifficultyComboBox() {
        return difficultyComboBox;
    }

    public void setOnDifficultyChanged(Consumer<Difficulty> handler) {
        difficultyComboBox.setOnAction(e -> {
            Difficulty value = difficultyComboBox.getValue();
            if (handler != null && value != null) handler.accept(value);
        });
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty != null) difficultyComboBox.setValue(difficulty);
    }

    public Button getColorBlindButton() { return colorBlindButton; }

    public void setColorBlindMode(boolean enabled) {
        colorBlindButton.setText(enabled ? "ON" : "OFF");
    }

    public Button getKeyButton(String action) {
        return keyButtons.get(action);
    }

    public List<String> getKeyActionNames() {
        return List.copyOf(keyButtons.keySet());
    }

    public void setKeyText(String action, String key) {
        Button button = keyButtons.get(action);
        if (button != null) button.setText(key);
    }

    public Button getResetScoreboardButton() { return resetScoreboardButton; }
    public Button getResetDefaultButton() { return resetDefaultButton; }
    public Button getSaveButton() { return saveButton; }
    public Button getBackButton() { return backButton; }

    public void showMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }
}