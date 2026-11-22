package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import com.team.tetris.view.TimeLimitView;
import com.team.tetris.view.TimeLimitView.TimeLimitItem;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

class TimeLimitControllerTest {
    @BeforeAll static void fxStart() { try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {} Platform.setImplicitExit(false); }
    private static void fx(Runnable action) throws Exception { FutureTask<Void> t = new FutureTask<>(() -> { action.run(); return null; }); Platform.runLater(t); t.get(10, TimeUnit.SECONDS); }
    @Test void wrapsThreeDurationsAndReturnsCorrectMinutes() throws Exception { fx(() -> {
        AtomicInteger minutes = new AtomicInteger(-1); AtomicInteger back = new AtomicInteger();
        TimeLimitController c = new TimeLimitController(new TimeLimitView(), minutes::set, back::incrementAndGet);
        c.show(); assertEquals(TimeLimitItem.THREE_MIN, c.getSelected());
        c.handleKey(KeyCode.ENTER); assertEquals(3, minutes.get());
        c.handleKey(KeyCode.DOWN); assertEquals(TimeLimitItem.FIVE_MIN, c.getSelected());
        c.handleKey(KeyCode.ENTER); assertEquals(5, minutes.get());
        c.handleKey(KeyCode.DOWN); assertEquals(TimeLimitItem.TEN_MIN, c.getSelected());
        c.handleKey(KeyCode.ENTER); assertEquals(10, minutes.get());
        c.handleKey(KeyCode.DOWN); assertEquals(TimeLimitItem.THREE_MIN, c.getSelected());
        c.handleKey(KeyCode.UP); assertEquals(TimeLimitItem.TEN_MIN, c.getSelected());
        assertEquals(0, back.get()); c.handleKey(KeyCode.ESCAPE); assertEquals(1, back.get());
    }); }
}
