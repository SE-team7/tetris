package com.team.tetris.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.PlayerId;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.control.ScrollPane;

public class SettingsView {

    private static final double LABEL_WIDTH = 90;
    private static final double BUTTON_WIDTH = 100;
    private static final double COLUMN_GAP = 20;

    private final VBox root;
    private final ScrollPane scroll = new ScrollPane();

    private final ComboBox<String> screenSizeComboBox;
    private final ComboBox<String> themeComboBox = new ComboBox<>();

    /*
     * 2차 추가:
     * 일반 모드 난이도 선택
     */
    private final ComboBox<Difficulty> difficultyComboBox;

    private final Map<String, Button> keyButtons;
    private final Map<PlayerId, Map<String, Button>> localButtons = new LinkedHashMap<>();
    private final Button singleTab = new Button("일반 / P2P");
    private final Button localTab = new Button("LOCAL BATTLE");
    private final GridPane localGrid = new GridPane();
    private final GridPane singleGrid = new GridPane();

    private final Button colorBlindButton;

    private final Button resetScoreboardButton;
    private final Button resetDefaultButton;
    private final Button saveButton;
    private final Button backButton;

    private final Label messageLabel;

    public SettingsView() {

        root = new VBox(8);
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("settings-root");
        scroll.getStyleClass().add("settings-scroll");
        root.setPadding(new Insets(12));

        /*
         * TITLE
         */
        Label titleLabel =
            new Label("SETTINGS");

        titleLabel.getStyleClass().add("settings-title");
        titleLabel.setStyle(
            "-fx-font-size: 36px;"
            + "-fx-font-weight: bold;"
        );

        VBox.setMargin(
            titleLabel,
            new Insets(0, 0, 12, 0)
        );

        /*
         * SCREEN SIZE
         */
        Label screenSizeTitle =
            createSettingLabel("화면 크기");

        screenSizeComboBox =
            new ComboBox<>();

        screenSizeComboBox
            .getItems()
            .addAll(
                "SMALL",
                "MEDIUM",
                "LARGE"
            );

        screenSizeComboBox.setValue("MEDIUM");
        screenSizeComboBox.setPrefWidth(150);

        HBox screenSizeRow =
            new HBox(
                COLUMN_GAP,
                screenSizeTitle,
                screenSizeComboBox
            );

        screenSizeRow.setAlignment(
            Pos.CENTER
        );

        themeComboBox.getItems().addAll("DARK", "LIGHT");
        themeComboBox.setValue("DARK");
        themeComboBox.setPrefWidth(150);
        HBox themeRow = new HBox(COLUMN_GAP, createSettingLabel("테마"), themeComboBox);
        themeRow.setAlignment(Pos.CENTER);

        /*
         * DIFFICULTY
         *
         * 일반 모드에서 사용할 난이도
         */
        Label difficultyTitle =
            createSettingLabel("난이도");

        difficultyComboBox =
            new ComboBox<>();

        difficultyComboBox
            .getItems()
            .addAll(
                Difficulty.EASY,
                Difficulty.NORMAL,
                Difficulty.HARD
            );

        difficultyComboBox.setValue(
            Difficulty.NORMAL
        );

        difficultyComboBox.setPrefWidth(
            150
        );

        HBox difficultyRow =
            new HBox(
                COLUMN_GAP,
                difficultyTitle,
                difficultyComboBox
            );

        difficultyRow.setAlignment(
            Pos.CENTER
        );

        /*
         * KEY SETTINGS
         */
        Label keySettingsTitle =
            new Label("키 설정");

        keySettingsTitle.getStyleClass().add("settings-section-title");
        keySettingsTitle.setStyle(
            "-fx-font-size: 16px;"
            + "-fx-font-weight: bold;"
        );

        GridPane keyGrid = singleGrid;

        keyGrid.setHgap(COLUMN_GAP);
        keyGrid.setVgap(8);
        keyGrid.setAlignment(Pos.CENTER);

        keyButtons =
            new LinkedHashMap<>();

        int row = 0;

        row = addKeyRow(
            keyGrid,
            row,
            "MOVE_LEFT",
            "A"
        );

        row = addKeyRow(
            keyGrid,
            row,
            "MOVE_RIGHT",
            "D"
        );

        row = addKeyRow(
            keyGrid,
            row,
            "SOFT_DROP",
            "S"
        );

        row = addKeyRow(
            keyGrid,
            row,
            "HARD_DROP",
            "ENTER"
        );

        row = addKeyRow(
            keyGrid,
            row,
            "ROTATE",
            "P"
        );

        addKeyRow(
            keyGrid,
            row,
            "SKIP",
            "-"
        );

        HBox tabs = new HBox(8, singleTab, localTab);
        tabs.setAlignment(Pos.CENTER);
        singleTab.setPrefWidth(120);
        localTab.setPrefWidth(120);
        localGrid.setHgap(10);
        localGrid.setVgap(8);
        localGrid.setAlignment(Pos.CENTER);
        localGrid.add(new Label("PLAYER 1"), 1, 0);
        localGrid.add(new Label("PLAYER 2"), 2, 0);
        localButtons.put(PlayerId.PLAYER1, new LinkedHashMap<>());
        localButtons.put(PlayerId.PLAYER2, new LinkedHashMap<>());
        int localRow = 1;
        for (String action : keyButtons.keySet()) {
            localGrid.add(new Label(action), 0, localRow);
            for (PlayerId player : new PlayerId[]{PlayerId.PLAYER1, PlayerId.PLAYER2}) {
                Button button = new Button("-");
                button.setPrefWidth(100);
                localButtons.get(player).put(action, button);
                localGrid.add(button, player == PlayerId.PLAYER1 ? 1 : 2, localRow);
            }
            localRow++;
        }
        setLocalTab(false);

        /*
         * COLOR BLIND MODE
         *
         * 키 설정 버튼들과 정확히 같은 열에 배치
         */
        Label colorBlindTitle =
            new Label("색맹 모드");

        colorBlindTitle.setMinWidth(
            LABEL_WIDTH
        );

        colorBlindTitle.setPrefWidth(
            LABEL_WIDTH
        );

        colorBlindButton =
            new Button("OFF");

        colorBlindButton.setMinWidth(
            BUTTON_WIDTH
        );

        colorBlindButton.setPrefWidth(
            BUTTON_WIDTH
        );

        GridPane colorBlindGrid =
            new GridPane();

        colorBlindGrid.setHgap(
            COLUMN_GAP
        );

        colorBlindGrid.setAlignment(
            Pos.CENTER
        );

        colorBlindGrid.add(
            colorBlindTitle,
            0,
            0
        );

        colorBlindGrid.add(
            colorBlindButton,
            1,
            0
        );

        /*
         * FIXED KEYS
         */
        Label fixedKeyNotice =
            new Label(
                "고정키 (변경 불가)\n"
                + "SHIFT : 일시정지 / 계속하기\n"
                + "TAB : 메인 메뉴로 돌아가기\n"
                + "ESC : 전체 게임 종료"
            );

        fixedKeyNotice.setAlignment(
            Pos.CENTER
        );

        fixedKeyNotice.getStyleClass().add("fixed-key-notice");
        fixedKeyNotice.setWrapText(true);
        fixedKeyNotice.setMinHeight(Region.USE_PREF_SIZE);
        fixedKeyNotice.setMaxWidth(Double.MAX_VALUE);
        fixedKeyNotice.setPrefWidth(300);

        /*
         * BUTTONS
         */
        saveButton =
            new Button("저장");

        resetDefaultButton =
            new Button("기본 설정 복구");

        resetScoreboardButton =
            new Button("스코어보드 초기화");

        backButton =
            new Button("메인 메뉴");

        saveButton.setPrefWidth(180);
        resetDefaultButton.setPrefWidth(180);
        resetScoreboardButton.setPrefWidth(180);
        backButton.setPrefWidth(180);

        /*
         * MESSAGE
         */
        messageLabel =
            new Label();

        messageLabel.getStyleClass().add("settings-message");
        messageLabel.setStyle("-fx-font-size: 12px;");
        messageLabel.setWrapText(true);

        /*
         * ORDER
         */
        root.getChildren().addAll(
            titleLabel,
            screenSizeRow,
            themeRow,
            difficultyRow,
            keySettingsTitle,
            tabs,
            keyGrid,
            localGrid,
            colorBlindGrid,
            fixedKeyNotice,
            saveButton,
            resetDefaultButton,
            resetScoreboardButton,
            backButton,
            messageLabel
        );

        scroll.setContent(root);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: #F7F7F7;");
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setMinHeight(Region.USE_PREF_SIZE);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setColorBlindMode(false);
    }

    private Label createSettingLabel(
        String text
    ) {

        Label label =
            new Label(text);

        label.setMinWidth(
            LABEL_WIDTH
        );

        label.setPrefWidth(
            LABEL_WIDTH
        );

        label.getStyleClass().add("settings-label");
        label.setStyle(
            "-fx-font-size: 15px;"
            + "-fx-font-weight: bold;"
        );

        return label;
    }

    private int addKeyRow(
        GridPane grid,
        int row,
        String action,
        String defaultKey
    ) {

        Label actionLabel =
            new Label(action);

        /*
         * 모든 왼쪽 Label의 폭을 동일하게 해서
         * 오른쪽 버튼들이 정확히 한 줄로 정렬됨
         */
        actionLabel.setMinWidth(
            LABEL_WIDTH
        );

        actionLabel.setPrefWidth(
            LABEL_WIDTH
        );

        Button keyButton =
            new Button(defaultKey);

        keyButton.setMinWidth(
            BUTTON_WIDTH
        );

        keyButton.setPrefWidth(
            BUTTON_WIDTH
        );

        keyButtons.put(
            action,
            keyButton
        );

        grid.add(
            actionLabel,
            0,
            row
        );

        grid.add(
            keyButton,
            1,
            row
        );

        return row + 1;
    }

    public Button getSingleTab() { return singleTab; }
    public Button getLocalTab() { return localTab; }
    public void setLocalTab(boolean local) {
        singleGrid.setVisible(!local);
        singleGrid.setManaged(!local);
        localGrid.setVisible(local);
        localGrid.setManaged(local);
        // The active tab is not a focused keyboard control.
        singleTab.setStyle("");
        localTab.setStyle("");
    }
    public Button getBattleKeyButton(PlayerId player, String action) {
        return localButtons.get(player).get(action);
    }
    public void setBattleKeyText(PlayerId player, String action, String text) {
        Button button = getBattleKeyButton(player, action);
        if (button != null) button.setText(text);
    }

    public Parent getRoot() { return scroll; }

    public ComboBox<String>
        getScreenSizeComboBox() {

        return screenSizeComboBox;
    }
    
    public ComboBox<Difficulty> 
    getDifficultyComboBox() {
        
        return difficultyComboBox;
    }

    /*
     * 2차 API:
     * 난이도 선택 이벤트 연결
     */
    public void setOnDifficultyChanged(
        Consumer<Difficulty> handler
    ) {

        difficultyComboBox.setOnAction(
            event -> {

                Difficulty difficulty =
                    difficultyComboBox.getValue();

                if (
                    handler != null
                    && difficulty != null
                ) {
                    handler.accept(
                        difficulty
                    );
                }
            }
        );
    }

    /*
     * 2차 API:
     * 현재 난이도를 화면에 표시
     */
    public void setDifficulty(
        Difficulty difficulty
    ) {

        if (difficulty != null) {
            difficultyComboBox.setValue(
                difficulty
            );
        }
    }

    public Button getColorBlindButton() {
        return colorBlindButton;
    }

    public void setColorBlindMode(
        boolean enabled
    ) {

        colorBlindButton.setText(
            enabled
                ? "ON"
                : "OFF"
        );
    }

    public Button getKeyButton(
        String action
    ) {

        return keyButtons.get(action);
    }

    public List<String>
        getKeyActionNames() {

        return List.copyOf(
            keyButtons.keySet()
        );
    }

    public void setKeyText(
        String action,
        String keyName
    ) {

        Button button =
            keyButtons.get(action);

        if (button != null) {
            button.setText(keyName);
        }
    }

    public Button
        getResetScoreboardButton() {

        return resetScoreboardButton;
    }

    public Button
        getResetDefaultButton() {

        return resetDefaultButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public void showMessage(
        String message
    ) {

        messageLabel.setText(
            message
        );
    }
    public ComboBox<String> getThemeComboBox() {
    return themeComboBox;
}
}
