package com.team.tetris.controller;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.team.tetris.network.HostAddress;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.view.P2PWaitingView;
import javafx.scene.input.KeyCode;

/**
 * P2P 대기실.
 * 요구사항: 서버와 클라이언트가 모두 '게임 시작' 버튼을 누르면 대전을 시작한다.
 *  - 버튼은 연결 완료와 게임 설정 확정 후에만 활성화 ({@link #setStartEnabled(boolean)})
 *  - 버튼 클릭 또는 Enter → {@link #startBattle()} → onStart 실행 (Main에서 NetworkController.setReady(true) 연결)
 *  - 한 번 누르면 버튼을 잠가 중복 입력을 막고, 상대의 게임 시작을 기다린다
 *  - onStart가 실패(예외)하면 버튼을 다시 열고 오류를 표시
 */
public class P2PWaitingController {
    private final P2PWaitingView view;
    private final Runnable onStart, onBack;
    private final Consumer<Boolean> onReadyChange;
    private boolean ready, enabled;

    public P2PWaitingController(P2PWaitingView view, Runnable onStart, Runnable onBack) {
        this(view, onStart, onBack, r -> {});
    }

    /**
     * @param onReadyChange 기존 Main 연결용. 게임 시작을 누르면 onStart 다음에 true로 호출된다.
     *                      onStart에서 setReady(true)를 호출한다면 이 생성자 대신 3개짜리를 쓰면 된다.
     */
    public P2PWaitingController(P2PWaitingView view, Runnable onStart,
                                Runnable onBack, Consumer<Boolean> onReadyChange) {
        this.view = Objects.requireNonNull(view, "view must not be null");
        this.onStart = onStart == null ? () -> {} : onStart;
        this.onBack = onBack == null ? () -> {} : onBack;
        this.onReadyChange = onReadyChange == null ? r -> {} : onReadyChange;
        view.setOnStart(this::startBattle);
    }

    public void show() {
        reset();
        view.setInfo("");
    }

    public void showHost(String ip, String mode) {
        reset();
        view.setInfo("HOST IP : " + ip + "\nMODE : " + mode);
    }

    /**
     * 서버 대기실: 이 PC의 IP를 "IP (어댑터 이름)" 형식으로 한 줄씩 표시.
     * 주소가 여러 개면 접속에 쓸 가능성이 높은 주소가 맨 위 (NetworkService.getLocalAddressDetails 순서).
     */
    public void showHost(List<HostAddress> addresses, String mode) {
        reset();
        view.setInfo(formatHostAddresses(addresses) + "\nMODE : " + (mode == null ? "" : mode));
    }

    /** "HOST IP" 아래에 주소를 한 줄씩. 여러 개면 접속할 때 괄호 앞 IP만 입력하라고 안내 */
    static String formatHostAddresses(List<HostAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return "HOST IP : (확인할 수 없음)";
        }
        if (addresses.size() == 1) {
            return "HOST IP : " + addresses.get(0).displayText();
        }
        StringBuilder text = new StringBuilder("HOST IP (접속할 때는 괄호 앞 IP만 입력)");
        for (HostAddress address : addresses) {
            text.append('\n').append(address.displayText());
        }
        return text.toString();
    }

    public void showClient(String ip) {
        reset();
        view.setInfo("SERVER IP : " + ip + "\n서버의 게임 설정을 기다리는 중...");
    }

    /** 클라이언트 대기실: 서버가 확정한 모드·난이도·제한시간 표시 */
    public void showClient(String ip, MessagePayload.Config config) {
        if (config == null) {
            showClient(ip);
            return;
        }
        reset();
        view.setInfo("SERVER IP : " + ip + "\nMODE : " + configText(config));
    }

    /** 대전 설정을 화면 문구로. 예) "NORMAL / HARD", "ITEM", "TIME LIMIT / 3분 / EASY" */
    static String configText(MessagePayload.Config config) {
        String difficulty = config.difficulty() == null ? "" : " / " + config.difficulty().name();
        return switch (config.mode()) {
            case "ITEM" -> "ITEM";
            case "TIME_LIMIT" -> "TIME LIMIT / " + (config.timeLimitSeconds() / 60) + "분" + difficulty;
            default -> config.mode() + difficulty;
        };
    }

    /**
     * 재대전 수락 후 대기실로 돌아왔을 때.
     * 요구사항대로 첫 대전과 같이 양쪽이 다시 '게임 시작'을 눌러야 대전이 시작된다.
     */
    public void showRematchWaiting(boolean configReady) {
        setStartEnabled(configReady);
        view.setStatus(configReady ? "재대전 - 게임 시작을 눌러주세요." : "재대전 - 서버의 게임 설정을 기다리는 중...");
    }

    private void reset() {
        ready = false;
        enabled = false;
        view.setStatus("상대방을 기다리는 중...");
        view.setStartEnabled(false);
        view.setLag(0, false);
    }

    /** 연결 완료 + 게임 설정 확정이면 true. 이미 누른 뒤에는 버튼을 다시 열지 않는다 */
    public void setStartEnabled(boolean value) {
        enabled = value && !ready;
        if (!ready) view.setStartEnabled(enabled);
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.ENTER) {
            startBattle();
        } else if (key == KeyCode.TAB) {
            onBack.run();
        }
    }

    public void showConnected() {
        view.setStatus("연결 완료");
    }

    public void showWaiting() {
        view.setStatus("상대방을 기다리는 중...");
    }

    /** 클라이언트: 서버를 아직 못 찾아 다시 시도하는 중 (회의 7차 문제 6·7) */
    public void showConnectRetry(long remainingMillis) {
        long seconds = (Math.max(0, remainingMillis) + 999) / 1000;
        view.setStatus("서버를 찾는 중... 3초마다 다시 시도합니다 (" + seconds + "초 남음)");
    }

    public void showOpponentReady() {
        view.setStatus(ready ? "양쪽 모두 게임 시작" : "상대방이 게임 시작을 눌렀습니다.");
    }

    /**
     * 게임 시작 버튼 (클릭·Enter 공통).
     * 활성화 전이거나 이미 눌렀으면 아무 것도 보내지 않는다.
     */
    public void startBattle() {
        if (ready) {
            return;
        }
        if (!enabled) {
            view.setStatus("연결 및 게임 설정을 기다리는 중입니다.");
            return;
        }
        ready = true;
        enabled = false;
        view.setReady(true);
        view.setStatus("상대방의 게임 시작을 기다리는 중...");
        try {
            onStart.run();
            onReadyChange.accept(true);
        } catch (RuntimeException e) {
            // 연결·설정이 유효하지 않음 → 다시 누를 수 있게 되돌림
            ready = false;
            enabled = true;
            view.setReady(false);
            view.setStartEnabled(true);
            showError(e.getMessage());
        }
    }

    public void showError(String message) {
        view.setStatus("오류: " + message);
    }

    public void showLag(long ms, boolean lagging) {
        view.setLag(ms, lagging);
    }

    public boolean isReady() { return ready; }

    public boolean isStartEnabled() { return enabled; }
}
