package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import com.team.tetris.network.RecentHostStore.HostEntry;
import com.team.tetris.view.P2PConnectionView;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

class P2PConnectionControllerTest {
    @BeforeAll static void fxStart() { try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {} Platform.setImplicitExit(false); }
    private static void fx(Runnable action) throws Exception { FutureTask<Void> t = new FutureTask<>(() -> { action.run(); return null; }); Platform.runLater(t); t.get(10, TimeUnit.SECONDS); }
    @Test void menuWrapsAndHostCallbackRuns() throws Exception { fx(() -> {
        P2PConnectionView view = new P2PConnectionView(); AtomicInteger hosted = new AtomicInteger(); AtomicInteger back = new AtomicInteger();
        P2PConnectionController c = new P2PConnectionController(view, hosted::incrementAndGet, s -> {}, back::incrementAndGet);
        c.show(); c.handleKey(KeyCode.DOWN); c.handleKey(KeyCode.DOWN);
        c.handleKey(KeyCode.ENTER); assertEquals(1, hosted.get()); assertEquals(0, back.get());
        c.handleKey(KeyCode.ESCAPE); assertEquals(1, back.get());
    }); }
    @Test void recentIpCanBeSelectedAndJoined() throws Exception { fx(() -> {
        P2PConnectionView view = new P2PConnectionView(); AtomicReference<String> joined = new AtomicReference<>();
        P2PConnectionController c = new P2PConnectionController(view, () -> {}, joined::set, () -> {},
            () -> List.of(new HostEntry("127.0.0.1", 7777), new HostEntry("192.168.1.2", 7777)));
        c.show(); c.handleKey(KeyCode.DOWN); c.handleKey(KeyCode.ENTER);
        c.handleKey(KeyCode.DOWN); assertEquals("127.0.0.1", view.getIp());
        c.handleKey(KeyCode.DOWN); assertEquals("192.168.1.2", view.getIp());
        c.handleKey(KeyCode.ENTER); assertEquals("192.168.1.2", joined.get());
    }); }
    @Test void blankIpDoesNotJoinAndEscapeClosesInput() throws Exception { fx(() -> {
        P2PConnectionView view = new P2PConnectionView(); AtomicInteger joined = new AtomicInteger();
        P2PConnectionController c = new P2PConnectionController(view, () -> {}, s -> joined.incrementAndGet(), () -> {});
        c.show(); c.handleKey(KeyCode.DOWN); c.handleKey(KeyCode.ENTER);
        c.handleKey(KeyCode.ENTER); assertEquals(0, joined.get());
        c.handleKey(KeyCode.ESCAPE); c.handleKey(KeyCode.UP); c.handleKey(KeyCode.ENTER);
        assertEquals(0, joined.get());
    }); }
}
