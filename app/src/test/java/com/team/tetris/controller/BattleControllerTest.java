package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleState;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.Settings;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.service.BattleService;
import com.team.tetris.view.BattleView;

import javafx.application.Platform;
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
