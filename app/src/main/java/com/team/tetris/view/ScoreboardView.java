package com.team.tetris.view;

import com.team.tetris.controller.ScoreboardMenu;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Score;
import com.team.tetris.service.ScoreboardService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 스코어보드 화면. SceneManager는 getRoot()로 화면을 가져가 표시한다.
 * 상단 버튼으로 일반 모드 / 아이템 모드 기록을 골라 본다.
 */
public class ScoreboardView {

    private static final String HIGHLIGHT_STYLE = "-fx-text-fill: #f5c542; -fx-font-weight: bold;";

    private final VBox root = new VBox(16);
    private final HBox modeBox = new HBox(8);
    private final Map<GameMode, ToggleButton> modeButtons = new EnumMap<>(GameMode.class);
    private final GridPane table = new GridPane();
    private final ScrollPane tableScroll = new ScrollPane(table);
    private final VBox menuBox = new VBox(6);
    private final Label keyGuide = new Label();

    private Consumer<GameMode> onModeSelected = mode -> { };

    public ScoreboardView() {
        Label title = new Label("SCOREBOARD");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        modeBox.setAlignment(Pos.CENTER);
        for (GameMode mode : GameMode.values()) {
            ToggleButton button = new ToggleButton(modeLabel(mode));
            // 키 입력이 root로 계속 전달되도록 버튼이 포커스를 가져가지 않게 함
            button.setFocusTraversable(false);
            button.setOnAction(event -> onModeSelected.accept(mode));
            modeButtons.put(mode, button);
            modeBox.getChildren().add(button);
        }

        table.setHgap(24);
        table.setVgap(4);
        table.setAlignment(Pos.CENTER);

        // 동점자 때문에 10줄을 넘으면 스크롤
        tableScroll.setFitToWidth(true);
        tableScroll.setFocusTraversable(false);
        tableScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        tableScroll.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (focused) {
                root.requestFocus();
            }
        });
        VBox.setVgrow(tableScroll, Priority.ALWAYS);

        menuBox.setAlignment(Pos.CENTER);

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("scoreboard");
        root.getChildren().addAll(title, modeBox, tableScroll, menuBox, keyGuide);
    }

    public Parent getRoot() {
        return root;
    }

    /** 모드 버튼을 눌렀을 때 실행할 동작 (ScoreboardController가 등록). */
    public void setOnModeSelected(Consumer<GameMode> onModeSelected) {
        this.onModeSelected = onModeSelected;
    }

    /** 일반 모드 기록 표시. */
    public void render(List<Score> scores, int highlightPosition, ScoreboardMenu selected) {
        render(scores, highlightPosition, selected, GameMode.NORMAL);
    }

    /**
     * @param scores            해당 모드의 상위 기록 (점수 내림차순)
     * @param highlightPosition 강조할 줄(1부터). 없으면 -1
     * @param selected          현재 선택된 메뉴
     * @param gameMode          현재 보고 있는 게임 모드
     */
    public void render(List<Score> scores, int highlightPosition, ScoreboardMenu selected, GameMode gameMode) {
        for (Map.Entry<GameMode, ToggleButton> entry : modeButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == gameMode);
        }

        table.getChildren().clear();
        String[] headers = {"순위", "이름", "점수", "난이도", "레벨", "줄"};
        for (int col = 0; col < headers.length; col++) {
            Label header = new Label(headers[col]);
            header.setStyle("-fx-font-weight: bold;");
            table.add(header, col, 0);
        }

        // 동점자는 같은 등수, 기록이 MAX_SCORES개보다 적으면 빈 줄로 채움
        int[] ranks = ScoreboardService.calculateRanks(scores);
        int rows = Math.max(ScoreboardService.MAX_SCORES, scores.size());
        for (int i = 0; i < rows; i++) {
            int position = i + 1;
            String[] cells;
            if (i < scores.size()) {
                Score s = scores.get(i);
                cells = new String[] {
                    String.valueOf(ranks[i]),
                    s.getName(),
                    String.valueOf(s.getScore()),
                    s.getDifficulty().name(),
                    String.valueOf(s.getLevel()),
                    String.valueOf(s.getClearedLines())};
            } else {
                cells = new String[] {String.valueOf(position), "-", "-", "-", "-", "-"};
            }
            for (int col = 0; col < cells.length; col++) {
                Label cell = new Label(cells[col]);
                if (position == highlightPosition) {
                    cell.setStyle(HIGHLIGHT_STYLE);
                }
                table.add(cell, col, position);
            }
        }

        menuBox.getChildren().clear();
        for (ScoreboardMenu menu : ScoreboardMenu.values()) {
            boolean isSelected = menu == selected;
            Label item = new Label((isSelected ? "> " : "  ") + menu.getLabel());
            if (isSelected) {
                item.setStyle(HIGHLIGHT_STYLE);
            }
            menuBox.getChildren().add(item);
        }
    }

    /** 하단 조작키 안내. 예: showKeyGuide("UP / DOWN", "ENTER") */
    public void showKeyGuide(String moveKeys, String selectKey) {
        keyGuide.setText("이동: " + moveKeys + "   선택: " + selectKey + "   모드 전환: LEFT / RIGHT");
    }

    private static String modeLabel(GameMode mode) {
        return switch (mode) {
            case NORMAL -> "일반 모드";
            case ITEM -> "아이템 모드";
            default -> mode.name();
        };
    }
}
