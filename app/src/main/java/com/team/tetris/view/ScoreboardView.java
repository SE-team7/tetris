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
import javafx.scene.layout.VBox;

/**
 * 스코어보드 화면.
 * SceneManager는 getRoot()로 화면을 가져가 표시한다.
 */
public class ScoreboardView {

    private static final String HIGHLIGHT_STYLE =
        "-fx-text-fill: #f5c542;"
        + "-fx-font-weight: bold;";

    private static final String MODE_SELECTED_STYLE =
        "-fx-font-weight: bold;"
        + "-fx-border-color: #f5c542;"
        + "-fx-border-width: 2px;";

    private final VBox root =
        new VBox(16);

    private final GridPane table =
        new GridPane();

    private final VBox menuBox =
        new VBox(6);

    private final Label keyGuide =
        new Label();

    /*
     * 2차:
     * 일반 / 아이템 스코어보드 선택
     */
    private final Button normalModeButton =
        new Button("일반모드");

    private final Button itemModeButton =
        new Button("아이템모드");

    private Consumer<GameMode>
        modeSelectedHandler;

    private GameMode selectedMode =
        GameMode.NORMAL;

    public ScoreboardView() {

        Label title =
            new Label("SCOREBOARD");

        title.setStyle(
            "-fx-font-size: 28px;"
            + "-fx-font-weight: bold;"
        );

        /*
         * MODE SELECT
         */
        normalModeButton.setPrefWidth(
            120
        );

        itemModeButton.setPrefWidth(
            120
        );

        HBox modeBox =
            new HBox(
                10,
                normalModeButton,
                itemModeButton
            );

        modeBox.setAlignment(
            Pos.CENTER
        );

        normalModeButton.setOnAction(
            event ->
                selectMode(
                    GameMode.NORMAL
                )
        );

        itemModeButton.setOnAction(
            event ->
                selectMode(
                    GameMode.ITEM
                )
        );

        updateModeButtonStyle();

        /*
         * TABLE
         */
        table.setHgap(24);
        table.setVgap(4);

        table.setAlignment(
            Pos.CENTER
        );

        menuBox.setAlignment(
            Pos.CENTER
        );

        root.setAlignment(
            Pos.CENTER
        );

        root.setPadding(
            new Insets(24)
        );

        root.getStyleClass()
            .add("scoreboard");

        root.getChildren().addAll(
            title,
            modeBox,
            table,
            menuBox,
            keyGuide
        );
    }

    public Parent getRoot() {

        return root;
    }

    /*
     * 2차 API
     *
     * 일반모드 / 아이템모드 버튼을
     * ScoreboardController와 연결한다.
     */
    public void setOnModeSelected(
        Consumer<GameMode> handler
    ) {

        this.modeSelectedHandler =
            handler;
    }

    /*
     * Controller에서 현재 선택 모드를
     * 화면에 맞춰줄 때 사용할 수 있다.
     */
    public void setSelectedMode(
        GameMode mode
    ) {

        if (mode == null) {
            return;
        }

        selectedMode =
            mode;

        updateModeButtonStyle();
    }

    private void selectMode(
        GameMode mode
    ) {

        selectedMode =
            mode;

        updateModeButtonStyle();

        if (
            modeSelectedHandler
                != null
        ) {

            modeSelectedHandler.accept(
                mode
            );
        }
    }

    private void updateModeButtonStyle() {

        if (
            selectedMode
                == GameMode.NORMAL
        ) {

            normalModeButton.setStyle(
                MODE_SELECTED_STYLE
            );

            itemModeButton.setStyle(
                ""
            );

        } else {

            normalModeButton.setStyle(
                ""
            );

            itemModeButton.setStyle(
                MODE_SELECTED_STYLE
            );
        }
    }

    /**
     * @param scores
     *     상위 기록 목록
     *
     * @param highlightRank
     *     강조할 등수(1부터).
     *     없으면 -1
     *
     * @param selected
     *     현재 선택된 메뉴
     */
    public void render(
        List<Score> scores,
        int highlightRank,
        ScoreboardMenu selected
    ) {

        table
            .getChildren()
            .clear();

        /*
         * 2차:
         * 난이도 열 추가
         */
        String[] headers = {
            "순위",
            "이름",
            "점수",
            "레벨",
            "줄",
            "난이도"
        };

        for (
            int col = 0;
            col < headers.length;
            col++
        ) {

            Label header =
                new Label(
                    headers[col]
                );

            header.setStyle(
                "-fx-font-weight: bold;"
            );

            table.add(
                header,
                col,
                0
            );
        }

        for (
            int i = 0;
            i < ScoreboardService.MAX_SCORES;
            i++
        ) {

            int rank =
                i + 1;

            String[] cells;

            if (
                scores != null
                && i < scores.size()
            ) {

                Score score =
                    scores.get(i);

                String difficulty =
                    score.getDifficulty()
                        == null
                        ? "-"
                        : score
                            .getDifficulty()
                            .name();

                cells =
                    new String[] {
                        String.valueOf(
                            rank
                        ),
                        score.getName(),
                        String.valueOf(
                            score.getScore()
                        ),
                        String.valueOf(
                            score.getLevel()
                        ),
                        String.valueOf(
                            score.getClearedLines()
                        ),
                        difficulty
                    };

            } else {

                cells =
                    new String[] {
                        String.valueOf(
                            rank
                        ),
                        "-",
                        "-",
                        "-",
                        "-",
                        "-"
                    };
            }

            for (
                int col = 0;
                col < cells.length;
                col++
            ) {

                Label cell =
                    new Label(
                        cells[col]
                    );

                if (
                    rank
                        == highlightRank
                ) {

                    cell.setStyle(
                        HIGHLIGHT_STYLE
                    );
                }

                table.add(
                    cell,
                    col,
                    rank
                );
            }
        }

        /*
         * 기존 메뉴
         */
        menuBox
            .getChildren()
            .clear();

        for (
            ScoreboardMenu menu :
            ScoreboardMenu.values()
        ) {

            boolean isSelected =
                menu == selected;

            Label item =
                new Label(
                    (
                        isSelected
                            ? "> "
                            : "  "
                    )
                    + menu.getLabel()
                );

            if (isSelected) {

                item.setStyle(
                    HIGHLIGHT_STYLE
                );
            }

            menuBox
                .getChildren()
                .add(
                    item
                );
        }
    }

    /*
     * 하단 조작키 안내
     */
    public void showKeyGuide(
        String moveKeys,
        String selectKey
    ) {

        keyGuide.setText(
            "이동: "
            + moveKeys
            + "   선택: "
            + selectKey
        );
    }
}