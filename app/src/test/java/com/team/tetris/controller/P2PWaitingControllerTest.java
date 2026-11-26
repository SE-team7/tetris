package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.network.HostAddress;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.view.P2PWaitingView;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;

/**
 * [요구사항] P2P 대기실의 '게임 시작' 버튼.
 * "서버와 클라이언트가 모두 '게임 시작' 버튼을 누르면 대전을 시작합니다."
 *  - 연결 완료·설정 확정 전에는 누를 수 없음
 *  - 클릭·Enter 모두 같은 동작, 한 번만 전송
 *  - 실패하면 다시 누를 수 있음
 * [회의 7차 문제 5-1] 서버 IP를 "IP (어댑터 이름)" 형식으로 한 줄씩 표시
 */
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

    private final P2PWaitingView view = new P2PWaitingView();
    private final AtomicInteger starts = new AtomicInteger();
    private final AtomicInteger backs = new AtomicInteger();

    private P2PWaitingController controller() {
        return new P2PWaitingController(view, starts::incrementAndGet, backs::incrementAndGet);
    }

    // ================================================================ 버튼 활성화

    @Test
    void startButton_isDisabledUntilConnectedAndConfigured() {
        P2PWaitingController controller = controller();
        controller.show();

        assertTrue(view.getStartButton().isDisabled());
        view.getStartButton().fire();
        controller.handleKey(KeyCode.ENTER);
        controller.startBattle();

        assertEquals(0, starts.get(), "활성화 전에는 게임 시작이 전송되지 않음");
        assertFalse(controller.isReady());
        assertTrue(view.getStatusText().contains("기다리는 중"), view.getStatusText());
    }

    @Test
    void startButton_isEnabledAfterConnectionAndConfig() {
        P2PWaitingController controller = controller();
        controller.show();

        controller.setStartEnabled(true);

        assertFalse(view.getStartButton().isDisabled());
        assertTrue(controller.isStartEnabled());
        assertEquals("게임 시작", view.getStartButton().getText());
        controller.setStartEnabled(false);
        assertTrue(view.getStartButton().isDisabled());
    }

    // ================================================================ 누르기 (클릭·Enter)

    @Test
    void clickingStartButton_runsOnStartOnceAndLocksButton() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);

        view.getStartButton().fire();
        view.getStartButton().fire();
        controller.handleKey(KeyCode.ENTER);

        assertEquals(1, starts.get(), "중복 입력 차단");
        assertTrue(controller.isReady());
        assertTrue(view.getStartButton().isDisabled());
        assertEquals("게임 시작 완료", view.getStartButton().getText());
        assertEquals("상대방의 게임 시작을 기다리는 중...", view.getStatusText());
    }

    @Test
    void enterKey_worksLikeClick() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);

        controller.handleKey(KeyCode.ENTER);
        controller.handleKey(KeyCode.ENTER);

        assertEquals(1, starts.get());
        assertTrue(controller.isReady());
    }

    @Test
    void startBattle_doesNotShowStartTextBeforeBattleBegins() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);

        controller.startBattle();

        assertNotEquals("START", view.getStatusText(), "첫 클릭만으로 대전이 시작된 것처럼 보이면 안 됨");
    }

    @Test
    void afterPressing_enablingAgainDoesNotUnlockButton() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);
        controller.startBattle();

        controller.setStartEnabled(true);   // 설정 재수신 등

        assertTrue(view.getStartButton().isDisabled());
        assertFalse(controller.isStartEnabled());
        controller.startBattle();
        assertEquals(1, starts.get());
    }

    @Test
    void legacyReadyCallback_isCalledWithTrueAfterOnStart() {
        AtomicInteger order = new AtomicInteger();
        AtomicInteger startOrder = new AtomicInteger();
        AtomicInteger readyOrder = new AtomicInteger();
        AtomicBoolean lastReady = new AtomicBoolean();
        P2PWaitingController controller = new P2PWaitingController(view,
                () -> startOrder.set(order.incrementAndGet()), () -> {},
                ready -> {
                    readyOrder.set(order.incrementAndGet());
                    lastReady.set(ready);
                });
        controller.setStartEnabled(true);

        view.getStartButton().fire();

        assertEquals(1, startOrder.get());
        assertEquals(2, readyOrder.get());
        assertTrue(lastReady.get());
    }

    @Test
    void startFailure_showsErrorAndAllowsRetry() {
        AtomicInteger attempts = new AtomicInteger();
        P2PWaitingController controller = new P2PWaitingController(view, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("battle config has not been selected");
            }
        }, () -> {});
        controller.setStartEnabled(true);

        controller.startBattle();

        assertFalse(controller.isReady());
        assertFalse(view.getStartButton().isDisabled(), "다시 누를 수 있어야 함");
        assertEquals("오류: battle config has not been selected", view.getStatusText());

        controller.startBattle();
        assertEquals(2, attempts.get());
        assertTrue(controller.isReady());
    }

    // ================================================================ 화면 전환·초기화

    @Test
    void show_resetsPressedState() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);
        controller.startBattle();

        controller.show();

        assertFalse(controller.isReady());
        assertTrue(view.getStartButton().isDisabled());
        controller.handleKey(KeyCode.ENTER);
        assertEquals(1, starts.get(), "초기화 후에는 다시 활성화될 때까지 비활성");
    }

    @Test
    void hostAndClientScreensResetReady() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.ENTER);
        controller.showHost("127.0.0.1", "NORMAL");
        assertFalse(controller.isReady());
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.ENTER);
        controller.showClient("127.0.0.1");
        assertFalse(controller.isReady());
        assertTrue(view.getInfoText().startsWith("SERVER IP : 127.0.0.1"));
    }

    @Test
    void tabReturnsToPreviousScreen() {
        P2PWaitingController controller = controller();
        controller.handleKey(KeyCode.TAB);
        assertEquals(1, backs.get());
        assertFalse(controller.isReady());
        assertEquals(0, starts.get());
    }

    @Test
    void otherKeysAreIgnored() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);
        controller.handleKey(KeyCode.SPACE);
        controller.handleKey(KeyCode.ESCAPE);
        assertEquals(0, starts.get());
        assertEquals(0, backs.get());
    }

    // ================================================================ 상태 표시

    @Test
    void opponentPressedStart_isShown() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);

        controller.showOpponentReady();
        assertEquals("상대방이 게임 시작을 눌렀습니다.", view.getStatusText());

        controller.startBattle();
        controller.showOpponentReady();
        assertEquals("양쪽 모두 게임 시작", view.getStatusText());
    }

    @Test
    void statusAndLagMethodsDoNotChangeReadyState() {
        P2PWaitingController controller = controller();
        controller.showConnected();
        assertEquals("연결 완료", view.getStatusText());
        controller.showWaiting();
        controller.showOpponentReady();
        controller.showLag(250, true);
        assertEquals("NETWORK LAG : 250ms", view.getLagText());
        controller.showLag(20, false);
        assertEquals("", view.getLagText());
        controller.showError("connection closed");
        assertEquals("오류: connection closed", view.getStatusText());
        assertFalse(controller.isReady());
    }

    // ================================================================ 서버 IP 표시 (IP 두 개 문제)

    @Test
    void showHost_withSeveralAddresses_listsEachWithAdapterName() {
        P2PWaitingController controller = controller();

        controller.showHost(List.of(
                new HostAddress("10.50.90.62", "Wi-Fi"),
                new HostAddress("192.168.56.1", "VirtualBox Host-Only Ethernet Adapter")), "NORMAL");

        assertEquals("""
                HOST IP (접속할 때는 괄호 앞 IP만 입력)
                10.50.90.62 (Wi-Fi)
                192.168.56.1 (VirtualBox Host-Only Ethernet Adapter)
                MODE : NORMAL""", view.getInfoText());
        assertFalse(controller.isReady());
    }

    @Test
    void showHost_withOneAddress_showsSingleLine() {
        controller().showHost(List.of(new HostAddress("10.50.90.62", "Wi-Fi")), "ITEM");

        assertEquals("HOST IP : 10.50.90.62 (Wi-Fi)\nMODE : ITEM", view.getInfoText());
    }

    @Test
    void showHost_withoutAddresses_doesNotFail() {
        controller().showHost(List.of(), null);

        assertEquals("HOST IP : (확인할 수 없음)\nMODE : ", view.getInfoText());
        assertEquals("HOST IP : (확인할 수 없음)", P2PWaitingController.formatHostAddresses(null));
    }

    // ================================================================ 클라이언트 설정 표시

    @Test
    void showClient_withConfig_showsModeDifficultyAndTime() {
        P2PWaitingController controller = controller();

        controller.showClient("10.0.0.2",
                new MessagePayload.Config("TIME_LIMIT", Difficulty.EASY, 180));
        assertEquals("SERVER IP : 10.0.0.2\nMODE : TIME LIMIT / 3분 / EASY", view.getInfoText());

        controller.showClient("10.0.0.2", new MessagePayload.Config("NORMAL", Difficulty.HARD, 0));
        assertEquals("SERVER IP : 10.0.0.2\nMODE : NORMAL / HARD", view.getInfoText());

        controller.showClient("10.0.0.2", new MessagePayload.Config("ITEM", null, 0));
        assertEquals("SERVER IP : 10.0.0.2\nMODE : ITEM", view.getInfoText());
    }

    @Test
    void showClient_withoutConfig_waitsForServerSettings() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);
        controller.startBattle();

        controller.showClient("10.0.0.2", null);

        assertFalse(controller.isReady());
        assertEquals("SERVER IP : 10.0.0.2\n서버의 게임 설정을 기다리는 중...", view.getInfoText());
    }

    // ================================================================ 재대전 (다시 게임 시작 버튼)

    @Test
    void rematchWaiting_requiresPressingStartAgain() {
        P2PWaitingController controller = controller();
        controller.setStartEnabled(true);
        controller.startBattle();   // 첫 대전
        assertEquals(1, starts.get());

        // 대전 종료 → 재대전 수락 → 대기실 복귀
        controller.showHost(List.of(new HostAddress("10.50.90.62", "Wi-Fi")), "NORMAL");
        controller.showRematchWaiting(true);

        assertFalse(controller.isReady(), "재대전은 자동 준비되지 않음");
        assertFalse(view.getStartButton().isDisabled());
        assertEquals("재대전 - 게임 시작을 눌러주세요.", view.getStatusText());
        assertEquals(1, starts.get());

        view.getStartButton().fire();
        assertEquals(2, starts.get());
        assertTrue(controller.isReady());
    }

    @Test
    void rematchWaiting_withoutConfig_keepsButtonDisabled() {
        P2PWaitingController controller = controller();
        controller.show();

        controller.showRematchWaiting(false);

        assertTrue(view.getStartButton().isDisabled());
        assertEquals("재대전 - 서버의 게임 설정을 기다리는 중...", view.getStatusText());
        controller.startBattle();
        assertEquals(0, starts.get());
    }

    // ================================================================ 서버를 찾는 중 (회의 7차 문제 6·7)

    @Test
    void connectRetry_showsRemainingSecondsWithoutPressingStart() {
        P2PWaitingController controller = controller();
        controller.showClient("10.0.0.2");

        controller.showConnectRetry(26_400);
        assertEquals("서버를 찾는 중... 3초마다 다시 시도합니다 (27초 남음)", view.getStatusText());
        controller.showConnectRetry(-5);
        assertEquals("서버를 찾는 중... 3초마다 다시 시도합니다 (0초 남음)", view.getStatusText());

        assertTrue(view.getStartButton().isDisabled(), "연결 전에는 게임 시작 불가");
        assertFalse(controller.isReady());
    }

    @Test
    void nullCallbacksAreAllowed() {
        P2PWaitingController controller = new P2PWaitingController(view, null, null, null);
        controller.setStartEnabled(true);

        controller.startBattle();
        controller.handleKey(KeyCode.TAB);

        assertTrue(controller.isReady());
        assertThrows(NullPointerException.class, () -> new P2PWaitingController(null, () -> {}, () -> {}));
    }
}
