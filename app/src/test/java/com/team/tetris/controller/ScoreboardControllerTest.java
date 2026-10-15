package com.team.tetris.controller;

import static com.team.tetris.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.team.tetris.FxTestSupport;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.util.FileManager;
import com.team.tetris.view.ScoreboardView;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/*
 * ESC는 Platform.exit()로 JavaFX 전체를 종료해 이후 테스트가 실행되지 않으므로 테스트하지 않음
 */
class ScoreboardControllerTest {

    private static final String HIGHLIGHT_COLOR = "#E89B00";

    @TempDir
    Path tempDir;

    private ScoreboardService service;
    private ScoreboardView view;
    private ScoreboardController controller;
    private int scoreboardShown;
    private int mainMenuShown;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new ScoreboardService(new FileManager(tempDir));
        service.addScore("N1", 300, 3, 10, Difficulty.EASY, GameMode.NORMAL);
        service.addScore("N2", 200, 2, 5, Difficulty.NORMAL, GameMode.NORMAL);
        service.addScore("I1", 900, 5, 20, Difficulty.HARD, GameMode.ITEM);
        scoreboardShown = 0;
        mainMenuShown = 0;
        onFx(() -> {
            view = new ScoreboardView();
            controller = new ScoreboardController(service, view,
                    () -> scoreboardShown++, () -> mainMenuShown++);
        });
    }

    private Label cell(int column, int row) {
        GridPane grid = (GridPane) ((VBox) view.getRoot()).getChildren().get(2);
        for (Node node : grid.getChildren()) {
            if (GridPane.getColumnIndex(node) == column && GridPane.getRowIndex(node) == row) {
                return (Label) node;
            }
        }
        return null;
    }

    private Button modeButton(int index) {
        HBox buttons = (HBox) ((VBox) view.getRoot()).getChildren().get(1);
        return (Button) buttons.getChildren().get(index);
    }

    private boolean highlighted(int row) {
        return cell(1, row).getStyle().contains(HIGHLIGHT_COLOR);
    }

    @Test
    void show_displaysNormalModeWithoutHighlight() throws Exception {
        onFx(() -> {
            controller.show();

            assertEquals(1, scoreboardShown);
            assertEquals(GameMode.NORMAL, view.getSelectedMode());
            assertEquals("N1", cell(1, 1).getText());
            assertEquals("N2", cell(1, 2).getText());
            assertFalse(highlighted(1));
        });
    }

    @Test
    void showWithPosition_highlightsRowInNormalMode() throws Exception {
        onFx(() -> {
            controller.show(2);

            assertEquals(GameMode.NORMAL, view.getSelectedMode());
            assertTrue(highlighted(2));
            assertFalse(highlighted(1));
        });
    }

    @Test
    void showWithMode_opensThatModeAndHighlights() throws Exception {
        onFx(() -> {
            controller.show(GameMode.ITEM, 1);

            assertEquals(GameMode.ITEM, view.getSelectedMode());
            assertEquals("I1", cell(1, 1).getText());
            assertEquals("HARD", cell(3, 1).getText());
            assertTrue(highlighted(1));
        });
    }

    @Test
    void showWithNullMode_meansNormal() throws Exception {
        onFx(() -> {
            controller.show(null, -1);

            assertEquals(GameMode.NORMAL, view.getSelectedMode());
            assertEquals("N1", cell(1, 1).getText());
        });
    }

    @Test
    void leftRightKeys_switchModeAndWrapAround() throws Exception {
        onFx(() -> {
            controller.show();

            controller.handleKey(KeyCode.RIGHT);
            assertEquals(GameMode.ITEM, view.getSelectedMode());
            assertEquals("I1", cell(1, 1).getText());

            controller.handleKey(KeyCode.RIGHT); // 마지막 모드에서 다시 처음으로
            assertEquals(GameMode.NORMAL, view.getSelectedMode());

            controller.handleKey(KeyCode.LEFT); // 처음 모드에서 마지막으로
            assertEquals(GameMode.ITEM, view.getSelectedMode());
        });
    }

    @Test
    void highlight_isShownOnlyInModeOfNewRecord() throws Exception {
        onFx(() -> {
            controller.show(GameMode.ITEM, 1);
            assertTrue(highlighted(1));

            controller.handleKey(KeyCode.LEFT); // 일반 모드로 이동
            assertFalse(highlighted(1));

            controller.handleKey(KeyCode.RIGHT); // 다시 아이템 모드
            assertTrue(highlighted(1));
        });
    }

    @Test
    void modeButtonClick_switchesRecordsThroughController() throws Exception {
        onFx(() -> {
            controller.show();

            modeButton(1).fire();
            assertEquals("I1", cell(1, 1).getText());

            modeButton(0).fire();
            assertEquals("N1", cell(1, 1).getText());
        });
    }

    @Test
    void selectModeNull_isIgnored() throws Exception {
        onFx(() -> {
            controller.show(GameMode.ITEM, -1);
            controller.selectMode(null);

            assertEquals(GameMode.ITEM, view.getSelectedMode());
        });
    }

    @Test
    void tabKey_goesToMainMenu() throws Exception {
        onFx(() -> {
            controller.show();
            controller.handleKey(KeyCode.TAB);

            assertEquals(1, mainMenuShown);
        });
    }

    @Test
    void selectMenu_mainMenu_goesToMainMenu() throws Exception {
        onFx(() -> {
            controller.selectMenu(ScoreboardMenu.MAIN_MENU);

            assertEquals(1, mainMenuShown);
        });
    }

    @Test
    void upDownAndOtherKeys_doNotLeaveScoreboard() throws Exception {
        onFx(() -> {
            controller.show();
            controller.handleKey(KeyCode.UP);
            controller.handleKey(KeyCode.DOWN);
            controller.handleKey(KeyCode.ENTER);
            controller.handleKey(KeyCode.A);
            controller.handleKey(null);

            assertEquals(0, mainMenuShown);
            assertEquals(GameMode.NORMAL, view.getSelectedMode());
            assertEquals("N1", cell(1, 1).getText());
        });
    }

    @Test
    void scoreboardMenu_hasLabel() {
        assertEquals("메인 메뉴", ScoreboardMenu.MAIN_MENU.getLabel());
    }
}
