package com.team.tetris.view;

import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BattleViewTest {

    @BeforeAll
    static void startJavaFx() {
        try { Platform.startup(() -> {}); }
        catch (IllegalStateException ignored) { }
    }

    @Test
    void rootAndInitialPlayerScores() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            assertInstanceOf(BorderPane.class, view.getRoot());
            assertEquals(0, view.getDisplayedScore(PlayerId.PLAYER1));
            assertEquals(0, view.getDisplayedScore(PlayerId.PLAYER2));
            return null;
        });
    }

    @Test
    void timerChangesWarningAndUrgentStyles() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            Label timer = field(view, "timer", Label.class);
            view.setTimer("TIME 02:00");
            assertEquals("TIME 02:00", timer.getText());
            assertFalse(timer.getStyleClass().contains("battle-timer-warning"));
            view.setTimer("TIME 01:00");
            assertTrue(timer.getStyleClass().contains("battle-timer-warning"));
            view.setTimer("TIME 00:30");
            assertTrue(timer.getStyleClass().contains("battle-timer-urgent"));
            assertFalse(timer.getStyleClass().contains("battle-timer-warning"));
            view.setTimer("TIME 00:31");
            assertTrue(timer.getStyleClass().contains("battle-timer-warning"));
            return null;
        });
    }

    @Test
    void timerHandlesInvalidAndEmptyValues() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            Label timer = field(view, "timer", Label.class);
            view.setTimer("not a time");
            assertEquals("not a time", timer.getText());
            assertFalse(timer.getStyleClass().contains("battle-timer-urgent"));
            view.setTimer(null);
            assertEquals("", timer.getText());
            assertFalse(timer.isVisible());
            assertFalse(timer.isManaged());
            view.setTimer("00:10");
            assertTrue(timer.isVisible());
            assertTrue(timer.isManaged());
            return null;
        });
    }

    @Test
    void statusAndResultAcceptNullAndClear() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            view.setStatus("CONNECTED");
            assertEquals("CONNECTED", field(view, "status", Label.class).getText());
            view.setStatus(null);
            assertEquals("", field(view, "status", Label.class).getText());
            view.showResult("WIN", "REMATCH?");
            assertEquals("WIN", field(view, "result", Label.class).getText());
            assertEquals("REMATCH?", field(view, "rematch", Label.class).getText());
            view.clearResult();
            assertEquals("", field(view, "result", Label.class).getText());
            assertEquals("", field(view, "rematch", Label.class).getText());
            return null;
        });
    }

    @Test
    void finishScreenShowsLocalScoresAndCanBeCleared() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            view.showFinish("PLAYER 1 WINS", "TIME UP", 450, 200, true);
            VBox screen = field(view, "finishScreen", VBox.class);
            assertTrue(screen.isVisible());
            assertTrue(screen.isManaged());
            assertEquals("PLAYER 1 WINS", field(view, "finishTitle", Label.class).getText());
            assertEquals("TIME UP", field(view, "finishReason", Label.class).getText());
            assertTrue(field(view, "finishScores", Label.class).getText().contains("PLAYER 1 SCORE : 450"));
            assertTrue(field(view, "finishGuide", Label.class).isManaged());
            view.clearResult();
            assertFalse(screen.isVisible());
            assertFalse(screen.isManaged());
            return null;
        });
    }

    @Test
    void finishScreenUsesServerAndClientNamesInP2p() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            view.setPlayerNames(true);
            view.showFinish("YOU LOSE", "DISCONNECTED", 10, 20, false);
            String scores = field(view, "finishScores", Label.class).getText();
            assertTrue(scores.contains("SERVER SCORE : 10"));
            assertTrue(scores.contains("CLIENT SCORE : 20"));
            assertTrue(field(view, "finishTitle", Label.class)
                .getStyleClass().contains("battle-finish-lose"));
            assertFalse(field(view, "finishGuide", Label.class).isManaged());
            view.setPlayerNames(false);
            view.showFinish("DRAW", "TIE", 1, 1, true);
            assertTrue(field(view, "finishScores", Label.class)
                .getText().contains("PLAYER 2 SCORE : 1"));
            return null;
        });
    }

    @Test
    void choiceSupportsArrowNavigationAndEnter() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            AtomicInteger left = new AtomicInteger();
            AtomicInteger right = new AtomicInteger();
            view.showChoice("REMATCH?", "Choose", "YES", left::incrementAndGet,
                "NO", right::incrementAndGet);
            HBox actions = field(view, "finishActions", HBox.class);
            assertEquals(2, actions.getChildren().size());
            assertTrue(actions.getChildren().get(0).getStyleClass()
                .contains("battle-choice-selected"));
            assertTrue(view.handleFinishKey(KeyCode.RIGHT));
            assertTrue(actions.getChildren().get(1).getStyleClass()
                .contains("battle-choice-selected"));
            assertTrue(view.handleFinishKey(KeyCode.ENTER));
            assertEquals(1, right.get());
            assertTrue(view.handleFinishKey(KeyCode.LEFT));
            assertTrue(view.handleFinishKey(KeyCode.ENTER));
            assertEquals(1, left.get());
            assertFalse(view.handleFinishKey(KeyCode.ESCAPE));
            return null;
        });
    }

    @Test
    void choiceSupportsUpAndDownAndNullCallbacks() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            view.showChoice("TEST", null, "A", null, "B", null);
            assertEquals("", field(view, "finishReason", Label.class).getText());
            assertTrue(view.handleFinishKey(KeyCode.DOWN));
            assertTrue(view.handleFinishKey(KeyCode.ENTER));
            assertTrue(view.handleFinishKey(KeyCode.UP));
            assertTrue(view.handleFinishKey(KeyCode.ENTER));
            return null;
        });
    }

    @Test
    void resultChoicesPreserveExistingFinishInformation() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            AtomicInteger accepted = new AtomicInteger();
            view.showFinish("YOU WIN!", "GOOD GAME", 300, 250, false);
            view.showResultChoices("REMATCH", accepted::incrementAndGet,
                "MENU", () -> {});
            assertEquals("YOU WIN!", field(view, "finishTitle", Label.class).getText());
            assertEquals("GOOD GAME", field(view, "finishReason", Label.class).getText());
            assertTrue(field(view, "finishScores", Label.class).getText().contains("300"));
            assertTrue(view.handleFinishKey(KeyCode.ENTER));
            assertEquals(1, accepted.get());
            return null;
        });
    }

    @Test
    void waitingDoesNotAcceptFinishKeys() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            view.showWaiting("WAIT", "OTHER PLAYER");
            assertEquals("WAIT", field(view, "finishTitle", Label.class).getText());
            assertEquals("WAITING FOR OPPONENT...",
                field(view, "finishGuide", Label.class).getText());
            assertEquals(0, field(view, "finishActions", HBox.class).getChildren().size());
            assertFalse(view.handleFinishKey(KeyCode.ENTER));
            assertFalse(view.handleFinishKey(KeyCode.RIGHT));
            return null;
        });
    }

    @Test
    void postMatchChatReturnsToWaitingRoomWithTheSameDraft() throws Exception {
        fx(() -> {
            BattleView battle = new BattleView();
            P2PWaitingView waiting = new P2PWaitingView();
            var input = (TextField) waiting.getRoot().lookup(".p2p-chat-input");
            input.setText("one more round?");
            battle.showWaiting("REMATCH REQUESTED", "Waiting for an answer");
            battle.setPostMatchChat(waiting.getChatPanel());
            assertTrue(battle.hasPostMatchChat());
            assertSame(input, battle.getRoot().lookup(".p2p-chat-input"));

            battle.clearResult();
            waiting.restoreChatPanel();
            assertFalse(battle.hasPostMatchChat());
            assertSame(input, waiting.getRoot().lookup(".p2p-chat-input"));
            assertEquals("one more round?", input.getText());
            return null;
        });
    }

    @Test
    void backActionCanBeSetAndRun() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            AtomicInteger count = new AtomicInteger();
            assertDoesNotThrow(view::runBackAction);
            view.setOnBackAction(count::incrementAndGet);
            view.runBackAction();
            assertEquals(1, count.get());
            view.setOnBackAction(null);
            assertDoesNotThrow(view::runBackAction);
            return null;
        });
    }

    @Test
    void itemModeChangesRootStyleAndCanBeDisabled() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            BorderPane root = (BorderPane) view.getRoot();
            view.setItemMode(true);
            assertTrue(root.getStyleClass().contains("battle-item-mode"));
            view.setItemMode(false);
            assertFalse(root.getStyleClass().contains("battle-item-mode"));
            assertDoesNotThrow(() -> view.applyColorBlindMode(true));
            assertDoesNotThrow(() -> view.applyColorBlindMode(false));
            return null;
        });
    }

    @Test
    void renderPlayersUpdatesDisplayedScoresAndNullIsIgnored() throws Exception {
        fx(() -> {
            BattleView view = new BattleView();
            BlockType[][] board = new BlockType[20][10];
            boolean[][] attacks = new boolean[20][10];
            GameSnapshot first = new GameSnapshot(board, attacks, null, null,
                120, 3, 5, null, false);
            GameSnapshot second = new GameSnapshot(board, attacks, null, null,
                240, 4, 2, null, false);
            view.renderPlayer(PlayerId.PLAYER1, first);
            view.renderPlayer(PlayerId.PLAYER2, second);
            assertEquals(120, view.getDisplayedScore(PlayerId.PLAYER1));
            assertEquals(240, view.getDisplayedScore(PlayerId.PLAYER2));
            view.renderPlayer(PlayerId.PLAYER1, null);
            assertEquals(120, view.getDisplayedScore(PlayerId.PLAYER1));
            return null;
        });
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) return action.call();
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get();
    }
}
