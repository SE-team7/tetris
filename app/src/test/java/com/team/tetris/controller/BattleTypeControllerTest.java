package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import com.team.tetris.view.BattleTypeView;
import com.team.tetris.view.BattleTypeView.BattleTypeItem;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

class BattleTypeControllerTest {
    @BeforeAll static void fxStart() { try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {} Platform.setImplicitExit(false); }
    private static void fx(Runnable action) throws Exception { FutureTask<Void> t = new FutureTask<>(() -> { action.run(); return null; }); Platform.runLater(t); t.get(10, TimeUnit.SECONDS); }
    @Test void arrowsWrapOnlyVisibleOptionsAndEnterSelects() throws Exception { fx(() -> {
        AtomicReference<BattleTypeItem> chosen = new AtomicReference<>(); AtomicInteger back = new AtomicInteger();
        BattleTypeController c = new BattleTypeController(new BattleTypeView(), chosen::set, back::incrementAndGet);
        c.show(); assertEquals(BattleTypeItem.LOCAL, c.getSelected());
        c.handleKey(KeyCode.DOWN); assertEquals(BattleTypeItem.P2P, c.getSelected());
        c.handleKey(KeyCode.DOWN); assertEquals(BattleTypeItem.LOCAL, c.getSelected());
        c.handleKey(KeyCode.UP); assertEquals(BattleTypeItem.P2P, c.getSelected());
        c.handleKey(KeyCode.ENTER); assertEquals(BattleTypeItem.P2P, chosen.get()); assertEquals(0, back.get());
        c.handleKey(KeyCode.ESCAPE); assertEquals(1, back.get());
        c.show(); assertEquals(BattleTypeItem.LOCAL, c.getSelected());
    }); }
}
