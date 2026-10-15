package com.team.tetris.view;

import static com.team.tetris.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.FxTestSupport;
import com.team.tetris.controller.ScoreboardMenu;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Score;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

class ScoreboardViewTest {

    private static final String HIGHLIGHT_COLOR = "#E89B00";

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    // 화면 구성: [제목, 모드 버튼(일반, 아이템), 점수 표, 키 안내]
    private static Button modeButton(ScoreboardView view, int index) {
        HBox buttons = (HBox) ((VBox) view.getRoot()).getChildren().get(1);
        return (Button) buttons.getChildren().get(index);
    }

    private static GridPane grid(ScoreboardView view) {
        return (GridPane) ((VBox) view.getRoot()).getChildren().get(2);
    }

    private static Label keyGuide(ScoreboardView view) {
        return (Label) ((VBox) view.getRoot()).getChildren().get(3);
    }

    /** 표에서 (열, 행) 칸의 Label. 0행은 머리글. */
    private static Label cell(ScoreboardView view, int column, int row) {
        for (Node node : grid(view).getChildren()) {
            Integer c = GridPane.getColumnIndex(node);
            Integer r = GridPane.getRowIndex(node);
            if (c != null && r != null && c == column && r == row) {
                return (Label) node;
            }
        }
        return null;
    }

    private static int rowCount(ScoreboardView view) {
        int max = -1;
        for (Node node : grid(view).getChildren()) {
            max = Math.max(max, GridPane.getRowIndex(node));
        }
        return max; // 머리글(0행) 제외한 줄 수
    }

    private static List<Score> scores(int... values) {
        List<Score> list = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            list.add(new Score("P" + i, values[i], 1, 0, Difficulty.NORMAL, GameMode.NORMAL));
        }
        return list;
    }

    @Test
    void emptyScoreboard_showsHeaderAndTenEmptyRows() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.render(List.of(), -1, ScoreboardMenu.MAIN_MENU, GameMode.NORMAL);

            assertEquals("순위", cell(view, 0, 0).getText());
            assertEquals("난이도", cell(view, 3, 0).getText());
            assertEquals(10, rowCount(view));
            assertEquals("1", cell(view, 0, 1).getText());
            assertEquals("-", cell(view, 1, 1).getText());
            assertEquals("10", cell(view, 0, 10).getText());
        });
    }

    @Test
    void scoreRow_showsAllColumnsIncludingDifficulty() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.render(List.of(new Score("KIM", 500, 3, 7, Difficulty.HARD, GameMode.NORMAL)),
                    -1, ScoreboardMenu.MAIN_MENU, GameMode.NORMAL);

            assertEquals("1", cell(view, 0, 1).getText());
            assertEquals("KIM", cell(view, 1, 1).getText());
            assertEquals("500", cell(view, 2, 1).getText());
            assertEquals("HARD", cell(view, 3, 1).getText());
            assertEquals("3", cell(view, 4, 1).getText());
            assertEquals("7", cell(view, 5, 1).getText());
            assertEquals("-", cell(view, 1, 2).getText()); // 다음 줄은 빈 줄
        });
    }

    @Test
    void tiedScores_shareSameRank() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.render(scores(300, 200, 200, 100), -1, ScoreboardMenu.MAIN_MENU, GameMode.NORMAL);

            assertEquals("1", cell(view, 0, 1).getText());
            assertEquals("2", cell(view, 0, 2).getText());
            assertEquals("2", cell(view, 0, 3).getText());
            assertEquals("4", cell(view, 0, 4).getText());
        });
    }

    @Test
    void highlightedRow_isStyledOnlyThatRow() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.render(scores(300, 200, 100), 2, ScoreboardMenu.MAIN_MENU, GameMode.NORMAL);

            assertTrue(cell(view, 1, 2).getStyle().contains(HIGHLIGHT_COLOR));
            assertTrue(cell(view, 1, 2).getStyle().contains("bold"));
            assertFalse(cell(view, 1, 1).getStyle().contains(HIGHLIGHT_COLOR));
            assertFalse(cell(view, 1, 3).getStyle().contains(HIGHLIGHT_COLOR));
        });
    }

    @Test
    void highlightOnEmptyRow_isIgnored() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.render(scores(300), 5, ScoreboardMenu.MAIN_MENU, GameMode.NORMAL);

            assertFalse(cell(view, 1, 5).getStyle().contains(HIGHLIGHT_COLOR));
        });
    }

    @Test
    void blankOrNullName_isShownAsDash() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.render(List.of(new Score(" ", 100, 1, 0), new Score(null, 90, 1, 0)));

            assertEquals("-", cell(view, 1, 1).getText());
            assertEquals("-", cell(view, 1, 2).getText());
        });
    }

    @Test
    void nullScoreList_showsEmptyRows() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.render(null, -1);

            assertEquals(10, rowCount(view));
            assertEquals("-", cell(view, 2, 1).getText());
        });
    }

    @Test
    void render_withMode_updatesSelectedModeButton() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            assertEquals(GameMode.NORMAL, view.getSelectedMode());

            view.render(List.of(), -1, ScoreboardMenu.MAIN_MENU, GameMode.ITEM);
            assertEquals(GameMode.ITEM, view.getSelectedMode());
            assertTrue(modeButton(view, 1).getStyle().contains("#444444")); // 선택된 버튼 색
            assertFalse(modeButton(view, 0).getStyle().contains("#444444"));

            // 모드 없는 render는 선택된 모드를 바꾸지 않음
            view.render(List.of(), -1, ScoreboardMenu.MAIN_MENU);
            assertEquals(GameMode.ITEM, view.getSelectedMode());
        });
    }

    @Test
    void setSelectedMode_null_meansNormal() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.setSelectedMode(GameMode.ITEM);
            view.setSelectedMode(null);

            assertEquals(GameMode.NORMAL, view.getSelectedMode());
        });
    }

    @Test
    void clickingModeButtons_notifiesHandler() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            List<GameMode> selected = new ArrayList<>();
            view.setOnModeSelected(selected::add);

            modeButton(view, 1).fire();
            assertEquals(GameMode.ITEM, view.getSelectedMode());
            modeButton(view, 0).fire();
            assertEquals(GameMode.NORMAL, view.getSelectedMode());

            assertEquals(List.of(GameMode.ITEM, GameMode.NORMAL), selected);
        });
    }

    @Test
    void clickingModeButtons_withoutHandler_doesNotFail() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            modeButton(view, 1).fire();

            assertEquals(GameMode.ITEM, view.getSelectedMode());
        });
    }

    @Test
    void showKeyGuide_showsModeAndMenuKeys() throws Exception {
        onFx(() -> {
            ScoreboardView view = new ScoreboardView();
            view.showKeyGuide("UP / DOWN", "ENTER");

            assertNotNull(keyGuide(view).getText());
            assertTrue(keyGuide(view).getText().contains("TAB"));
            assertTrue(keyGuide(view).getText().contains("모드 전환"));
        });
    }
}
