package com.team.tetris.view;

import com.team.tetris.controller.ScoreboardMenu;
import com.team.tetris.domain.Score;
import com.team.tetris.service.ScoreboardService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 스코어보드 화면. SceneManager는 getRoot()로 화면을 가져가 표시한다.
 */
public class ScoreboardView {

    private static final String HIGHLIGHT_STYLE = "-fx-text-fill: #f5c542; -fx-font-weight: bold;";

    private final VBox root = new VBox(16);
    private final GridPane table = new GridPane();
    private final VBox menuBox = new VBox(6);
    private final Label keyGuide = new Label();

    public ScoreboardView() {
        Label title = new Label("SCOREBOARD");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        table.setHgap(24);
        table.setVgap(4);
        table.setAlignment(Pos.CENTER);
        menuBox.setAlignment(Pos.CENTER);

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("scoreboard");
        root.getChildren().addAll(title, table, menuBox, keyGuide);
    }

    public Parent getRoot() {
        return root;
    }

    /**
     * @param scores        상위 기록 목록
     * @param highlightRank 강조할 등수(1부터). 없으면 -1
     * @param selected      현재 선택된 메뉴
     */
    public void render(List<Score> scores, int highlightRank, ScoreboardMenu selected) {
        table.getChildren().clear();
        String[] headers = {"순위", "이름", "점수", "레벨", "줄"};
        for (int col = 0; col < headers.length; col++) {
            Label header = new Label(headers[col]);
            header.setStyle("-fx-font-weight: bold;");
            table.add(header, col, 0);
        }

        for (int i = 0; i < ScoreboardService.MAX_SCORES; i++) {
            int rank = i + 1;
            String[] cells = i < scores.size()
                    ? new String[] {
                        String.valueOf(rank),
                        scores.get(i).getName(),
                        String.valueOf(scores.get(i).getScore()),
                        String.valueOf(scores.get(i).getLevel()),
                        String.valueOf(scores.get(i).getClearedLines())}
                    : new String[] {String.valueOf(rank), "-", "-", "-", "-"};
            for (int col = 0; col < cells.length; col++) {
                Label cell = new Label(cells[col]);
                if (rank == highlightRank) {
                    cell.setStyle(HIGHLIGHT_STYLE);
                }
                table.add(cell, col, rank);
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
        keyGuide.setText("이동: " + moveKeys + "   선택: " + selectKey);
    }
}
