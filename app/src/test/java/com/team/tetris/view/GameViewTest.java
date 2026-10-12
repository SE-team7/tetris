package com.team.tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;

import javafx.application.Platform;
import javafx.scene.control.Label;

class GameViewTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX가 이미 시작된 경우
        }
    }

    /*
     * 일반 블록이 게임 보드에 정상적으로 렌더링되는지 확인
     */
    @Test
    void rendersFixedBlockOnBoard() throws Exception {

        String renderedText = runOnFxThread(() -> {

            GameView view = new GameView();

            BlockType[][] board =
                new BlockType[25][10];

            /*
             * 마지막 행의 첫 번째 칸에
             * I 블록을 배치
             */
            board[24][0] = BlockType.I;

            view.render(
                board,
                null,
                null,
                0,
                1,
                false
            );

            Label[][] boardCells =
                getBoardCells(view);

            /*
             * 25행 중 화면에는 마지막 20행이 보이므로
             * board[24][0] -> boardCells[19][0]
             */
            return boardCells[19][0].getText();
        });

        assertEquals(
            "O",
            renderedText
        );
    }

    /*
     * 색맹 모드에서 일반 L 블록이
     * 아이템 L과 구분되도록 G로 표시되는지 확인
     */
    @Test
    void rendersLBlockAsGInColorBlindMode()
        throws Exception {

        String renderedText = runOnFxThread(() -> {

            GameView view = new GameView();

            view.applyColorBlindMode(true);

            BlockType[][] board =
                new BlockType[25][10];

            board[24][0] = BlockType.L;

            view.render(
                board,
                null,
                null,
                0,
                1,
                false
            );

            Label[][] boardCells =
                getBoardCells(view);

            return boardCells[19][0].getText();
        });

        assertEquals(
            "G",
            renderedText
        );
    }

    /*
     * GameView 자체가
     * MODE / DIFFICULTY 값을 정상적으로
     * 표시할 수 있는지 확인
     */
    @Test
    void displaysModeAndDifficulty()
        throws Exception {

        String[] texts = runOnFxThread(() -> {

            GameView view = new GameView();

            view.setGameInfo(
                GameMode.ITEM,
                Difficulty.NORMAL
            );

            Label modeLabel =
                getLabel(
                    view,
                    "modeLabel"
                );

            Label difficultyLabel =
                getLabel(
                    view,
                    "difficultyLabel"
                );

            return new String[] {
                modeLabel.getText(),
                difficultyLabel.getText()
            };
        });

        assertEquals(
            "MODE: ITEM",
            texts[0]
        );

        assertEquals(
            "DIFFICULTY: NORMAL",
            texts[1]
        );
    }

    /*
     * private boardCells 확인용
     */
    private static Label[][] getBoardCells(
        GameView view
    ) throws Exception {

        Field field =
            GameView.class
                .getDeclaredField(
                    "boardCells"
                );

        field.setAccessible(true);

        return (Label[][])
            field.get(view);
    }

    /*
     * private Label 확인용
     */
    private static Label getLabel(
        GameView view,
        String fieldName
    ) throws Exception {

        Field field =
            GameView.class
                .getDeclaredField(
                    fieldName
                );

        field.setAccessible(true);

        return (Label)
            field.get(view);
    }

    /*
     * JavaFX UI 작업은
     * JavaFX Application Thread에서 실행
     */
    private static <T> T runOnFxThread(
        java.util.concurrent.Callable<T> action
    ) throws Exception {

        if (
            Platform.isFxApplicationThread()
        ) {
            return action.call();
        }

        FutureTask<T> task =
            new FutureTask<>(action);

        Platform.runLater(task);

        return task.get(
            5,
            TimeUnit.SECONDS
        );
    }
}
