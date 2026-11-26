package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import com.team.tetris.view.BattleModeView;
import com.team.tetris.view.BattleModeView.BattleModeItem;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

class BattleModeControllerTest {
    @BeforeAll static void fxStart() { try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {} Platform.setImplicitExit(false); }
    private static void fx(Runnable action) throws Exception { FutureTask<Void> t = new FutureTask<>(() -> { action.run(); return null; }); Platform.runLater(t); t.get(10, TimeUnit.SECONDS); }
    @Test void threeModesWrapAndEnterSelectsWithoutInvisibleBack() throws Exception { fx(() -> {
        AtomicReference<BattleModeItem> chosen = new AtomicReference<>(); AtomicInteger back = new AtomicInteger();
        BattleModeController c = new BattleModeController(new BattleModeView(), chosen::set, back::incrementAndGet);
        c.show(); assertEquals(BattleModeItem.NORMAL, c.getSelected());
        c.handleKey(KeyCode.DOWN); assertEquals(BattleModeItem.ITEM, c.getSelected());
        c.handleKey(KeyCode.DOWN); assertEquals(BattleModeItem.TIME_LIMIT, c.getSelected());
        c.handleKey(KeyCode.DOWN); assertEquals(BattleModeItem.NORMAL, c.getSelected());
        c.handleKey(KeyCode.UP); assertEquals(BattleModeItem.TIME_LIMIT, c.getSelected());
        c.handleKey(KeyCode.ENTER); assertEquals(BattleModeItem.TIME_LIMIT, chosen.get()); assertEquals(0, back.get());
        c.handleKey(KeyCode.ESCAPE); assertEquals(1, back.get());
        c.show(); assertEquals(BattleModeItem.NORMAL, c.getSelected());
    }); }
}
