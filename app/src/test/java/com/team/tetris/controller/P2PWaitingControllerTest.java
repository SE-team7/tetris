package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import javafx.application.Platform;

import com.team.tetris.view.P2PWaitingView;
import javafx.scene.input.KeyCode;

class P2PWaitingControllerTest {
    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // JavaFX already initialized by another test.
        }
        Platform.setImplicitExit(false);
    }


    @Test
    void showResetsReadyAndDisablesEnter() {
        AtomicInteger changes = new AtomicInteger();
        P2PWaitingController controller = new P2PWaitingController(
            new P2PWaitingView(), () -> {}, () -> {}, ready -> changes.incrementAndGet());
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.ENTER);
        assertTrue(controller.isReady());

        controller.show();
        assertFalse(controller.isReady());
        controller.handleKey(KeyCode.ENTER);
        assertEquals(1, changes.get(), "Enter must be disabled after reset");
    }

    @Test
    void enterOnlySendsReadyOnce() {
        AtomicInteger changes = new AtomicInteger();
        AtomicBoolean lastReady = new AtomicBoolean();
        P2PWaitingController controller = new P2PWaitingController(
            new P2PWaitingView(), () -> {}, () -> {}, ready -> {
                changes.incrementAndGet();
                lastReady.set(ready);
            });
        controller.show();
        controller.handleKey(KeyCode.ENTER);
        assertEquals(0, changes.get());
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.ENTER);
        controller.handleKey(KeyCode.ENTER);
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.ENTER);
        assertTrue(controller.isReady());
        assertTrue(lastReady.get());
        assertEquals(1, changes.get());
    }

    @Test
    void tabReturnsToPreviousScreen() {
        AtomicInteger backs = new AtomicInteger();
        P2PWaitingController controller = new P2PWaitingController(
            new P2PWaitingView(), () -> {}, backs::incrementAndGet);
        controller.handleKey(KeyCode.TAB);
        assertEquals(1, backs.get());
        assertFalse(controller.isReady());
    }

    @Test
    void startBattleInvokesCallback() {
        AtomicInteger starts = new AtomicInteger();
        P2PWaitingController controller = new P2PWaitingController(
            new P2PWaitingView(), starts::incrementAndGet, () -> {});
        controller.startBattle();
        assertEquals(1, starts.get());
    }

    @Test
    void hostAndClientScreensResetReady() {
        P2PWaitingController controller = new P2PWaitingController(
            new P2PWaitingView(), () -> {}, () -> {});
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.ENTER);
        controller.showHost("127.0.0.1", "NORMAL");
        assertFalse(controller.isReady());
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.ENTER);
        controller.showClient("127.0.0.1");
        assertFalse(controller.isReady());
    }

    @Test
    void statusAndLagMethodsDoNotChangeReadyState() {
        P2PWaitingController controller = new P2PWaitingController(
            new P2PWaitingView(), () -> {}, () -> {});
        controller.showConnected();
        controller.showWaiting();
        controller.showOpponentReady();
        controller.showLag(250, true);
        controller.showLag(20, false);
        controller.showError("connection closed");
        assertFalse(controller.isReady());
    }
}
