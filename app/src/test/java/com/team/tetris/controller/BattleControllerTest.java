package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleState;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.service.BattleService;
import com.team.tetris.view.BattleView;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;

class BattleControllerTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // JavaFX toolkit is shared by the test suite.
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void nullAndUnmappedKeysDoNotInterruptController() throws Exception {
        onFx(() -> {
            BattleService service = new BattleService();
            BattleController controller = new BattleController(
                service, new BattleView(), new Settings(), () -> {});
            try {
                controller.handleKeyPressed(null);
                controller.handleKeyReleased(null);
                controller.handleKeyPressed(KeyCode.F12);
                controller.handleKeyReleased(KeyCode.F12);
                assertNotNull(controller);
            } finally {
                controller.stop();
            }
            return null;
        });
    }

    @Test
    void escapeStopsBattleAndReturnsToMenu() throws Exception {
        onFx(() -> {
            AtomicInteger backs = new AtomicInteger();
            BattleService service = new BattleService();
            BattleController controller = new BattleController(
                service, new BattleView(), new Settings(), backs::incrementAndGet);
            controller.handleKeyPressed(KeyCode.ESCAPE);
            assertEquals(1, backs.get());
            assertNotEquals(BattleState.RUNNING, service.getState());
            return null;
        });
    }

    @Test
    void localBattleStartsAndStops() throws Exception {
        onFx(() -> {
            BattleService service = new BattleService();
            BattleController controller = new BattleController(
                service, new BattleView(), new Settings(), () -> {});
            try {
                controller.startLocal(BattleConfig.normal(Difficulty.NORMAL));
                assertEquals(BattleState.RUNNING, service.getState());
                controller.handleKeyPressed(KeyCode.A);
                controller.handleKeyReleased(KeyCode.A);
                controller.handleKeyPressed(KeyCode.P);
                controller.handleKeyReleased(KeyCode.P);
            } finally {
                controller.stop();
            }
            assertNotEquals(BattleState.RUNNING, service.getState());
            return null;
        });
    }

    @Test
    void cpuBattleUsesSharedKeysAndShowsOnlyHumanControls() throws Exception {
        onFx(() -> {
            var service = new RecordingBattleService();
            var view = new BattleView();
            var settings = new Settings();
            assertTrue(settings.setKey(KeyAction.MOVE_LEFT, KeyCode.J));
            var controller = new BattleController(service, view, settings, () -> {});
            try {
                controller.setAgent(PlayerId.PLAYER2, snapshot -> List.of());
                controller.startLocal(BattleConfig.normal(Difficulty.NORMAL));
                controller.handleKeyPressed(KeyCode.J);
                controller.handleKeyPressed(KeyCode.A);
                controller.handleKeyPressed(KeyCode.LEFT);

                assertEquals(List.of(new Input(PlayerId.PLAYER1, KeyAction.MOVE_LEFT)), service.inputs);
                var visibleKeys = view.getRoot().lookupAll(".battle-control-key").stream()
                    .filter(node -> node.getParent().getParent().isVisible())
                    .map(node -> ((Label) node).getText())
                    .toList();
                assertEquals(5, visibleKeys.size());
                assertTrue(visibleKeys.contains("J / D"));

                controller.clearAgents();
                controller.startLocal(BattleConfig.normal(Difficulty.NORMAL));
                service.inputs.clear();
                controller.handleKeyPressed(KeyCode.J);
                controller.handleKeyPressed(KeyCode.A);
                controller.handleKeyPressed(KeyCode.LEFT);
                assertEquals(List.of(new Input(PlayerId.PLAYER1, KeyAction.MOVE_LEFT),
                    new Input(PlayerId.PLAYER2, KeyAction.MOVE_LEFT)), service.inputs);
            } finally {
                controller.stop();
            }
            return null;
        });
    }

    @Test
    void p2pGuestDoesNotInheritCpuInputs() throws Exception {
        onFx(() -> {
            var service = new RecordingBattleService();
            var controller = new BattleController(service, new BattleView(), new Settings(), () -> {});
            var config = BattleConfig.normal(Difficulty.NORMAL);
            try {
                controller.setAgent(PlayerId.PLAYER2, snapshot -> List.of(KeyAction.HARD_DROP));
                controller.startLocal(config);
                assertEquals(List.of(new Input(PlayerId.PLAYER2, KeyAction.HARD_DROP)), service.inputs);

                controller.stop();
                service.startNetworkBattle(config, PlayerId.PLAYER2);
                service.inputs.clear();
                controller.startRendering();
                assertTrue(service.inputs.isEmpty());
                controller.handleKeyPressed(KeyCode.A);
                assertEquals(List.of(new Input(PlayerId.PLAYER2, KeyAction.MOVE_LEFT)), service.inputs);
            } finally {
                controller.stop();
            }
            return null;
        });
    }

    private record Input(PlayerId player, KeyAction action) {}

    private static class RecordingBattleService extends BattleService {
        final List<Input> inputs = new ArrayList<>();
        private final GameSnapshot snapshot = new GameSnapshot(new BlockType[Board.ROWS][Board.COLS],
            new boolean[Board.ROWS][Board.COLS], null, null, 0, 1, 7, null, false);

        @Override
        public GameSnapshot getPlayerSnapshot(PlayerId player) {
            return snapshot;
        }

        @Override
        public void submitAction(PlayerId player, KeyAction action) {
            inputs.add(new Input(player, action));
        }
    }

    @Test
    void networkRenderingAndInputCleanupAreSafe() throws Exception {
        onFx(() -> {
            BattleService service = new BattleService();
            BattleController controller = new BattleController(
                service, new BattleView(), new Settings(), () -> {});
            try {
                controller.startRendering();
                controller.handleKeyPressed(KeyCode.A);
                controller.handleKeyReleased(KeyCode.A);
                controller.showP2PResult(null, true);
                controller.clearInputStates();
                controller.stopRenderingOnly();
            } finally {
                controller.stop();
            }
            return null;
        });
    }


    @Test
    void p2pResultVariantsDoNotCrashAndStopRendering() throws Exception {
        onFx(() -> {
            BattleService service = new BattleService();
            BattleView view = new BattleView();
            BattleController controller = new BattleController(
                service, view, new Settings(), () -> {});
            try {
                controller.showP2PResult(new MessagePayload.Result(
                    NetworkController.HOST_PLAYER, false, "GAME_OVER"), true);
                controller.showP2PResult(new MessagePayload.Result(
                    NetworkController.CLIENT_PLAYER, false, "TIME_UP"), true);
                controller.showP2PResult(new MessagePayload.Result(
                    NetworkController.CLIENT_PLAYER, false, "PLAYER_LEFT"), false);
                controller.showP2PResult(new MessagePayload.Result(
                    null, true, "TIME_UP"), false);
                controller.showP2PResult(new MessagePayload.Result(
                    null, false, "CONNECTION_LOST"), true);
                controller.showP2PResult(new MessagePayload.Result(
                    "unknown", false, "CUSTOM_REASON"), true);
                assertNotNull(view);
            } finally { controller.stop(); }
            return null;
        });
    }

    @Test
    void stoppedBattleIgnoresGameplayInput() throws Exception {
        onFx(() -> {
            BattleService service = new BattleService();
            AtomicInteger backs = new AtomicInteger();
            BattleController controller = new BattleController(
                service, new BattleView(), new Settings(), backs::incrementAndGet);
            try {
                controller.handleKeyPressed(KeyCode.A);
                controller.handleKeyPressed(KeyCode.ENTER);
                controller.handleKeyReleased(KeyCode.A);
                assertNotEquals(BattleState.RUNNING, service.getState());
                assertEquals(0, backs.get());
            } finally { controller.stop(); }
            return null;
        });
    }

    private static <T> T onFx(Callable<T> work) throws Exception {
        if (Platform.isFxApplicationThread()) return work.call();
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get();
    }
}
