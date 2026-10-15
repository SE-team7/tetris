package com.team.tetris.view;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.GameMode;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

class ScoreboardViewTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX가 이미 시작된 경우
        }
    }

    @Test
    void rootExistsAndDefaultModeIsNormal() throws Exception {
        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            Parent root =
                view.getRoot();

            assertNotNull(root);

            assertEquals(
                GameMode.NORMAL,
                view.getSelectedMode()
            );

            return null;
        });
    }

    @Test
    void selectedModeCanChangeToItem() throws Exception {
        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.setSelectedMode(
                GameMode.ITEM
            );

            assertEquals(
                GameMode.ITEM,
                view.getSelectedMode()
            );

            Button normalButton =
                getField(
                    view,
                    "normalModeButton",
                    Button.class
                );

            Button itemButton =
                getField(
                    view,
                    "itemModeButton",
                    Button.class
                );

            assertTrue(
                itemButton
                    .getStyle()
                    .contains(
                        "#444444"
                    )
            );

            assertTrue(
                normalButton
                    .getStyle()
                    .contains(
                        "#E8E8E8"
                    )
            );

            return null;
        });
    }

    @Test
    void selectedModeCanChangeBackToNormal() throws Exception {
        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.setSelectedMode(
                GameMode.ITEM
            );

            view.setSelectedMode(
                GameMode.NORMAL
            );

            assertEquals(
                GameMode.NORMAL,
                view.getSelectedMode()
            );

            Button normalButton =
                getField(
                    view,
                    "normalModeButton",
                    Button.class
                );

            Button itemButton =
                getField(
                    view,
                    "itemModeButton",
                    Button.class
                );

            assertTrue(
                normalButton
                    .getStyle()
                    .contains(
                        "#444444"
                    )
            );

            assertTrue(
                itemButton
                    .getStyle()
                    .contains(
                        "#E8E8E8"
                    )
            );

            return null;
        });
    }

    @Test
    void nullModeFallsBackToNormal() throws Exception {
        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.setSelectedMode(
                GameMode.ITEM
            );

            view.setSelectedMode(
                null
            );

            assertEquals(
                GameMode.NORMAL,
                view.getSelectedMode()
            );

            return null;
        });
    }

    @Test
    void itemModeButtonCallsHandler() throws Exception {
        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            AtomicReference<GameMode>
                receivedMode =
                    new AtomicReference<>();

            view.setOnModeSelected(
                receivedMode::set
            );

            Button itemButton =
                getField(
                    view,
                    "itemModeButton",
                    Button.class
                );

            itemButton.fire();

            assertEquals(
                GameMode.ITEM,
                receivedMode.get()
            );

            assertEquals(
                GameMode.ITEM,
                view.getSelectedMode()
            );

            return null;
        });
    }

    @Test
    void normalModeButtonCallsHandler() throws Exception {
        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.setSelectedMode(
                GameMode.ITEM
            );

            AtomicReference<GameMode>
                receivedMode =
                    new AtomicReference<>();

            view.setOnModeSelected(
                receivedMode::set
            );

            Button normalButton =
                getField(
                    view,
                    "normalModeButton",
                    Button.class
                );

            normalButton.fire();

            assertEquals(
                GameMode.NORMAL,
                receivedMode.get()
            );

            assertEquals(
                GameMode.NORMAL,
                view.getSelectedMode()
            );

            return null;
        });
    }

    @Test
    void buttonsWorkEvenWithoutHandler() throws Exception {
        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            Button itemButton =
                getField(
                    view,
                    "itemModeButton",
                    Button.class
                );

            Button normalButton =
                getField(
                    view,
                    "normalModeButton",
                    Button.class
                );

            itemButton.fire();

            assertEquals(
                GameMode.ITEM,
                view.getSelectedMode()
            );

            normalButton.fire();

            assertEquals(
                GameMode.NORMAL,
                view.getSelectedMode()
            );

            return null;
        });
    }

    @Test
    void emptyScoreboardCreatesHeaderAndTenRows()
        throws Exception {

        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.render(
                List.of()
            );

            GridPane scoreGrid =
                getField(
                    view,
                    "scoreGrid",
                    GridPane.class
                );

            /*
             * HEADER 6개
             * +
             * 10줄 × 6칸
             * =
             * 66개
             */
            assertEquals(
                66,
                scoreGrid
                    .getChildren()
                    .size()
            );

            assertEquals(
                "순위",
                getGridLabel(
                    scoreGrid,
                    0,
                    0
                ).getText()
            );

            assertEquals(
                "이름",
                getGridLabel(
                    scoreGrid,
                    1,
                    0
                ).getText()
            );

            assertEquals(
                "점수",
                getGridLabel(
                    scoreGrid,
                    2,
                    0
                ).getText()
            );

            assertEquals(
                "난이도",
                getGridLabel(
                    scoreGrid,
                    3,
                    0
                ).getText()
            );

            assertEquals(
                "레벨",
                getGridLabel(
                    scoreGrid,
                    4,
                    0
                ).getText()
            );

            assertEquals(
                "줄",
                getGridLabel(
                    scoreGrid,
                    5,
                    0
                ).getText()
            );

            return null;
        });
    }

    @Test
    void emptyRowsContainRanksAndDashValues()
        throws Exception {

        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.render(
                List.of()
            );

            GridPane scoreGrid =
                getField(
                    view,
                    "scoreGrid",
                    GridPane.class
                );

            assertEquals(
                "1",
                getGridLabel(
                    scoreGrid,
                    0,
                    1
                ).getText()
            );

            assertEquals(
                "-",
                getGridLabel(
                    scoreGrid,
                    1,
                    1
                ).getText()
            );

            assertEquals(
                "10",
                getGridLabel(
                    scoreGrid,
                    0,
                    10
                ).getText()
            );

            assertEquals(
                "-",
                getGridLabel(
                    scoreGrid,
                    5,
                    10
                ).getText()
            );

            return null;
        });
    }

    @Test
    void nullScoreListDoesNotCrash()
        throws Exception {

        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.render(
                null,
                -1
            );

            GridPane scoreGrid =
                getField(
                    view,
                    "scoreGrid",
                    GridPane.class
                );

            assertEquals(
                66,
                scoreGrid
                    .getChildren()
                    .size()
            );

            return null;
        });
    }

    @Test
    void renderWithModeChangesSelectedMode()
        throws Exception {

        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.render(
                List.of(),
                -1,
                null,
                GameMode.ITEM
            );

            assertEquals(
                GameMode.ITEM,
                view.getSelectedMode()
            );

            view.render(
                List.of(),
                -1,
                null,
                GameMode.NORMAL
            );

            assertEquals(
                GameMode.NORMAL,
                view.getSelectedMode()
            );

            return null;
        });
    }

    @Test
    void allRenderOverloadsCanBeCalled()
        throws Exception {

        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            assertDoesNotThrow(
                () -> view.render(
                    List.of()
                )
            );

            assertDoesNotThrow(
                () -> view.render(
                    List.of(),
                    -1
                )
            );

            assertDoesNotThrow(
                () -> view.render(
                    List.of(),
                    -1,
                    null
                )
            );

            assertDoesNotThrow(
                () -> view.render(
                    List.of(),
                    -1,
                    null,
                    GameMode.ITEM
                )
            );

            return null;
        });
    }

    @Test
    void keyGuideShowsFixedControls()
        throws Exception {

        runFx(() -> {

            ScoreboardView view =
                new ScoreboardView();

            view.showKeyGuide(
                "TEST",
                "TEST"
            );

            Label keyGuideLabel =
                getField(
                    view,
                    "keyGuideLabel",
                    Label.class
                );

            assertEquals(
                "← / →: 모드 전환   TAB: 메인메뉴   ESC: 전체 종료",
                keyGuideLabel.getText()
            );

            return null;
        });
    }

    @Test
    void safeTextHandlesNullBlankAndNormalText()
        throws Exception {

        ScoreboardView view =
            runFx(
                ScoreboardView::new
            );

        Method safeText =
            ScoreboardView.class
                .getDeclaredMethod(
                    "safeText",
                    String.class
                );

        safeText.setAccessible(true);

        assertEquals(
            "-",
            safeText.invoke(
                view,
                new Object[] { null }
            )
        );

        assertEquals(
            "-",
            safeText.invoke(
                view,
                ""
            )
        );

        assertEquals(
            "-",
            safeText.invoke(
                view,
                "   "
            )
        );

        assertEquals(
            "PLAYER",
            safeText.invoke(
                view,
                "PLAYER"
            )
        );
    }

    @Test
    void allColumnWidthsAreCorrect()
        throws Exception {

        ScoreboardView view =
            runFx(
                ScoreboardView::new
            );

        Method method =
            ScoreboardView.class
                .getDeclaredMethod(
                    "getColumnWidth",
                    int.class
                );

        method.setAccessible(true);

        assertEquals(
            45.0,
            (double) method.invoke(
                view,
                0
            )
        );

        assertEquals(
            95.0,
            (double) method.invoke(
                view,
                1
            )
        );

        assertEquals(
            75.0,
            (double) method.invoke(
                view,
                2
            )
        );

        assertEquals(
            95.0,
            (double) method.invoke(
                view,
                3
            )
        );

        assertEquals(
            65.0,
            (double) method.invoke(
                view,
                4
            )
        );

        assertEquals(
            55.0,
            (double) method.invoke(
                view,
                5
            )
        );

        /*
         * default branch
         */
        assertEquals(
            70.0,
            (double) method.invoke(
                view,
                99
            )
        );
    }

    private static Label getGridLabel(
        GridPane grid,
        int column,
        int row
    ) {

        for (
            javafx.scene.Node node :
                grid.getChildren()
        ) {

            int nodeColumn =
                GridPane
                    .getColumnIndex(node)
                    == null
                        ? 0
                        : GridPane
                            .getColumnIndex(
                                node
                            );

            int nodeRow =
                GridPane
                    .getRowIndex(node)
                    == null
                        ? 0
                        : GridPane
                            .getRowIndex(
                                node
                            );

            if (
                nodeColumn == column
                && nodeRow == row
                && node
                    instanceof Label
            ) {

                return (Label) node;
            }
        }

        fail(
            "Label not found: column="
            + column
            + ", row="
            + row
        );

        return null;
    }

    private static <T> T getField(
        Object target,
        String fieldName,
        Class<T> type
    ) throws Exception {

        Field field =
            target
                .getClass()
                .getDeclaredField(
                    fieldName
                );

        field.setAccessible(
            true
        );

        return type.cast(
            field.get(
                target
            )
        );
    }

    private static <T> T runFx(
        Callable<T> action
    ) throws Exception {

        if (
            Platform
                .isFxApplicationThread()
        ) {

            return action.call();
        }

        FutureTask<T> task =
            new FutureTask<>(
                action
            );

        Platform.runLater(
            task
        );

        return task.get();
    }
}
