package com.team.tetris.view;

import java.util.List;
import java.util.function.Consumer;

import com.team.tetris.controller.ScoreboardMenu;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Score;
import com.team.tetris.service.ScoreboardService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ScoreboardView {

    /*
     * 밝은 화면 디자인
     */
    private static final String BACKGROUND_COLOR = "#F4F4F4";
    private static final String TEXT_COLOR = "#222222";
    private static final String HEADER_COLOR = "#333333";
    private static final String DIM_TEXT_COLOR = "#AAAAAA";
    private static final String HIGHLIGHT_COLOR = "#E89B00";

    private final VBox root;

    private final Button normalModeButton;
    private final Button itemModeButton;

    private final GridPane scoreGrid;
    private final Label keyGuideLabel;

    private Consumer<GameMode> onModeSelected;

    private GameMode selectedMode = GameMode.NORMAL;


    public ScoreboardView() {

        root = new VBox(12);

        root.setAlignment(
                Pos.TOP_CENTER
        );

        /*
         * 위쪽 여백을 충분히 둬서
         * SCOREBOARD 제목이 잘리지 않도록 함
         */
        root.setPadding(
                new Insets(
                        32,
                        30,
                        24,
                        30
                )
        );

        /*
         * Settings 화면과 맞춘 밝은 배경
         */
        root.setStyle(
                "-fx-background-color: "
                        + BACKGROUND_COLOR
                        + ";"
        );

        /*
         * TITLE
         */
        Label titleLabel =
                new Label("SCOREBOARD");

        titleLabel.setStyle(
                "-fx-font-size: 36px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + TEXT_COLOR
                        + ";"
        );

        /*
         * 제목 아래 공간
         */
        VBox.setMargin(
                titleLabel,
                new Insets(
                        0,
                        0,
                        20,
                        0
                )
        );

        /*
         * MODE BUTTONS
         */
        normalModeButton =
                new Button("일반 모드");

        itemModeButton =
                new Button("아이템 모드");

        normalModeButton.setFocusTraversable(false);
        itemModeButton.setFocusTraversable(false);

        normalModeButton.setOnAction(
                event -> {

                    setSelectedMode(
                            GameMode.NORMAL
                    );

                    if (onModeSelected != null) {

                        onModeSelected.accept(
                                GameMode.NORMAL
                        );
                    }
                }
        );

        itemModeButton.setOnAction(
                event -> {

                    setSelectedMode(
                            GameMode.ITEM
                    );

                    if (onModeSelected != null) {

                        onModeSelected.accept(
                                GameMode.ITEM
                        );
                    }
                }
        );

        HBox modeButtons =
                new HBox(
                        10,
                        normalModeButton,
                        itemModeButton
                );

        modeButtons.setAlignment(
                Pos.CENTER
        );

        /*
         * 버튼과 스코어 표 사이 공간
         */
        VBox.setMargin(
                modeButtons,
                new Insets(
                        0,
                        0,
                        22,
                        0
                )
        );

        /*
         * SCORE TABLE
         */
        scoreGrid =
                new GridPane();

        scoreGrid.setAlignment(
                Pos.TOP_CENTER
        );

        scoreGrid.setHgap(22);
        scoreGrid.setVgap(9);

        scoreGrid.setPadding(
                new Insets(
                        4,
                        0,
                        10,
                        0
                )
        );

        /*
         * KEY GUIDE
         */
        keyGuideLabel =
                new Label();

        keyGuideLabel.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: "
                        + DIM_TEXT_COLOR
                        + ";"
        );

        VBox.setVgrow(
                scoreGrid,
                Priority.ALWAYS
        );

        root.getChildren().addAll(
                titleLabel,
                modeButtons,
                scoreGrid,
                keyGuideLabel
        );

        setSelectedMode(
                GameMode.NORMAL
        );

        render(
                List.of(),
                -1,
                null,
                GameMode.NORMAL
        );
    }

    public Parent getRoot() {

        return root;
    }

    /*
     * 모드 선택 이벤트
     */
    public void setOnModeSelected(
            Consumer<GameMode> handler) {

        this.onModeSelected =
                handler;
    }

    /*
     * 현재 선택된 모드 표시
     */
    public void setSelectedMode(
            GameMode mode) {

        if (mode == null) {

            mode =
                    GameMode.NORMAL;
        }

        selectedMode =
                mode;

        if (
                selectedMode
                == GameMode.NORMAL
        ) {

            normalModeButton.setStyle(
                    selectedButtonStyle()
            );

            itemModeButton.setStyle(
                    normalButtonStyle()
            );

        } else {

            normalModeButton.setStyle(
                    normalButtonStyle()
            );

            itemModeButton.setStyle(
                    selectedButtonStyle()
            );
        }
    }

    public GameMode getSelectedMode() {

        return selectedMode;
    }

    /*
     * 현재 ScoreboardController가 사용하는 render
     */
    public void render(
            List<Score> scores,
            int highlightIndex,
            ScoreboardMenu selectedMenu,
            GameMode currentMode) {

        setSelectedMode(
                currentMode
        );

        renderTable(
                scores,
                highlightIndex
        );
    }

    /*
     * 기존 코드 호환용
     */
    public void render(
            List<Score> scores,
            int highlightIndex,
            ScoreboardMenu selectedMenu) {

        renderTable(
                scores,
                highlightIndex
        );
    }

    public void render(
            List<Score> scores,
            int highlightIndex) {

        renderTable(
                scores,
                highlightIndex
        );
    }

    public void render(
            List<Score> scores) {

        renderTable(
                scores,
                -1
        );
    }

    /*
     * 실제 스코어 표
     */
    private void renderTable(
            List<Score> scores,
            int highlightIndex) {

        scoreGrid
                .getChildren()
                .clear();

        /*
         * HEADER
         */
        addHeader(
                "순위",
                0
        );

        addHeader(
                "이름",
                1
        );

        addHeader(
                "점수",
                2
        );

        addHeader(
                "난이도",
                3
        );

        addHeader(
                "레벨",
                4
        );

        addHeader(
                "줄",
                5
        );

        /*
         * SCORE ROWS
         */
        int[] ranks = ScoreboardService.calculateRanks(
                scores == null ? List.of() : scores
        );

        for (
                int row = 0;
                row < 10;
                row++
        ) {

            int gridRow =
                    row + 1;

            boolean hasScore =
                    scores != null
                            && row
                            < scores.size();

            boolean highlighted = hasScore && gridRow == highlightIndex;

            if (hasScore) {

                Score score =
                        scores.get(row);

                String color =
                        highlighted
                                ? HIGHLIGHT_COLOR
                                : TEXT_COLOR;

                addCell(
                        String.valueOf(ranks[row]),
                        0,
                        gridRow,
                        color
                );

                addCell(
                        safeText(
                                score.getName()
                        ),
                        1,
                        gridRow,
                        color
                );

                addCell(
                        String.valueOf(
                                score.getScore()
                        ),
                        2,
                        gridRow,
                        color
                );

                addCell(
                        score.getDifficulty()
                                == null
                                ? "-"
                                : score
                                .getDifficulty()
                                .name(),
                        3,
                        gridRow,
                        color
                );

                addCell(
                        String.valueOf(
                                score.getLevel()
                        ),
                        4,
                        gridRow,
                        color
                );

                addCell(
                        String.valueOf(
                                score.getClearedLines()
                        ),
                        5,
                        gridRow,
                        color
                );

            } else {

                addCell(
                        String.valueOf(
                                row + 1
                        ),
                        0,
                        gridRow,
                        DIM_TEXT_COLOR
                );

                addCell(
                        "-",
                        1,
                        gridRow,
                        DIM_TEXT_COLOR
                );

                addCell(
                        "-",
                        2,
                        gridRow,
                        DIM_TEXT_COLOR
                );

                addCell(
                        "-",
                        3,
                        gridRow,
                        DIM_TEXT_COLOR
                );

                addCell(
                        "-",
                        4,
                        gridRow,
                        DIM_TEXT_COLOR
                );

                addCell(
                        "-",
                        5,
                        gridRow,
                        DIM_TEXT_COLOR
                );
            }
        }
    }

    /*
     * KEY GUIDE
     */
    public void showKeyGuide(
            String moveKeys,
            String selectKey) {

        keyGuideLabel.setText(
                moveKeys
                        + ": 이동   "
                        + selectKey
                        + ": 메인 메뉴   LEFT / RIGHT: 모드 전환   ESC: 메인 메뉴"
        );
    }

    /*
     * HEADER
     */
    private void addHeader(
            String text,
            int column) {

        Label label =
                new Label(text);

        label.setAlignment(
                Pos.CENTER
        );

        label.setMinWidth(
                getColumnWidth(
                        column
                )
        );

        label.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + HEADER_COLOR
                        + ";"
        );

        scoreGrid.add(
                label,
                column,
                0
        );
    }

    /*
     * SCORE CELL
     */
    private void addCell(
            String text,
            int column,
            int row,
            String color) {

        Label label =
                new Label(text);

        label.setAlignment(
                Pos.CENTER
        );

        label.setMinWidth(
                getColumnWidth(
                        column
                )
        );

        boolean highlighted = HIGHLIGHT_COLOR.equals(color);

        label.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-text-fill: "
                        + color
                        + ";"
                        + "-fx-font-weight: "
                        + (
                        highlighted
                                ? "bold"
                                : "normal"
                )
                        + ";"
        );

        scoreGrid.add(
                label,
                column,
                row
        );
    }

    /*
     * COLUMN WIDTH
     */
    private double getColumnWidth(
            int column) {

        return switch (column) {

            case 0 -> 45;
            case 1 -> 95;
            case 2 -> 75;
            case 3 -> 95;
            case 4 -> 65;
            case 5 -> 55;

            default -> 70;
        };
    }

    private String safeText(
            String text) {

        if (
                text == null
                        || text.isBlank()
        ) {

            return "-";
        }

        return text;
    }

    /*
     * 선택된 모드 버튼
     */
    private String selectedButtonStyle() {

        return
                "-fx-background-color: #444444;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 7 14 7 14;"
                        + "-fx-background-radius: 3;"
                        + "-fx-border-color: #444444;"
                        + "-fx-border-radius: 3;";
    }

    /*
     * 선택되지 않은 모드 버튼
     */
    private String normalButtonStyle() {

        return
                "-fx-background-color: #E8E8E8;"
                        + "-fx-text-fill: #222222;"
                        + "-fx-font-size: 14px;"
                        + "-fx-padding: 7 14 7 14;"
                        + "-fx-background-radius: 3;"
                        + "-fx-border-color: #BBBBBB;"
                        + "-fx-border-radius: 3;";
    }
}