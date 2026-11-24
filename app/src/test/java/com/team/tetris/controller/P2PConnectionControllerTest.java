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
    // [회의 7차 문제 6·7] 30초 동안 서버를 못 찾으면: 주소 재확인 안내 → IP 입력란
    @Test void addressCheckOpensIpInputWithPreviousIpAndMessage() throws Exception { fx(() -> {
        P2PConnectionView view = new P2PConnectionView(); AtomicReference<String> joined = new AtomicReference<>();
        P2PConnectionController c = new P2PConnectionController(view, () -> {}, joined::set, () -> {},
            () -> List.of(new HostEntry("192.168.1.2", 7777)));
        c.show();
        c.showAddressCheck("10.0.0.9", "서버(10.0.0.9:7777)를 찾을 수 없습니다. IP 주소를 다시 확인해주세요.");
        assertTrue(c.isEnteringIp(), "IP 입력란으로 바로 이동");
        assertEquals("10.0.0.9", view.getIp(), "입력했던 IP를 채워 둠");
        assertTrue(view.getStatus().contains("IP 주소를 다시 확인"), view.getStatus());
        // 최근 접속 IP로 고친 뒤 Enter → 다시 접속
        c.handleKey(KeyCode.DOWN); assertEquals("192.168.1.2", view.getIp());
        c.handleKey(KeyCode.ENTER); assertEquals("192.168.1.2", joined.get());
    }); }
    @Test void addressCheckWithoutMessageUsesDefaultAndShowClearsIt() throws Exception { fx(() -> {
        P2PConnectionView view = new P2PConnectionView();
        P2PConnectionController c = new P2PConnectionController(view, () -> {}, s -> {}, () -> {});
        c.showAddressCheck("10.0.0.9", null);
        assertEquals("서버를 찾을 수 없습니다. IP 주소를 다시 확인해주세요.", view.getStatus());
        c.show();
        assertEquals("", view.getStatus(), "처음 화면으로 돌아가면 안내 문구 지움");
        assertFalse(c.isEnteringIp());
        c.showError("port in use"); assertEquals("오류: port in use", view.getStatus());
        c.showError(null); assertEquals("", view.getStatus());
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
