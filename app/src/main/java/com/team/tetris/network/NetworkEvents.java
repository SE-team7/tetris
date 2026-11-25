package com.team.tetris.network;

import com.team.tetris.controller.NetworkController;

/**
 * NetworkController → 화면(3번) 통지.
 * NetworkController가 JavaFX Application Thread에서 호출하므로 바로 화면을 바꿔도 된다.
 * 필요한 것만 구현하면 되도록 모두 기본 구현(아무것도 안 함)을 둔다.
 */
public interface NetworkEvents {

    /** 연결 대기·연결 완료·상대 준비 등 진행 상황 문구 (NetworkView.showConnectionStatus) */
    default void showConnectionStatus(String status) {
    }

    /** 연결·통신 오류 문구 (NetworkView.showError) */
    default void showError(String message) {
    }

    /** 연결 단계 변경 */
    default void onStateChanged(NetworkController.State state) {
    }

    /** 서버가 정한 대전 설정 (클라이언트 화면에 모드 표시용, 서버에서도 호출) */
    default void onConfigChanged(MessagePayload.Config config) {
    }

    /** 상대 준비 상태 변경 */
    default void onPeerReadyChanged(boolean ready) {
    }

    /** 시작 카운트다운 시작. delayMillis 후 대전 시작 */
    default void onBattleStarting(long delayMillis) {
    }

    /** 대전 시작 → 대전 화면으로 전환 */
    default void onBattleStarted() {
    }

    /** 양쪽 일시정지·재개 */
    default void onPauseChanged(boolean paused) {
    }

    /** 대전 종료. 결과 표시 후 연결 직후 대기 상태로 돌아감 */
    default void onBattleFinished(MessagePayload.Result result) {
    }

    /** 상대의 재대전 요청 */
    default void onRematchRequested() {
    }

    /** 내 재대전 요청에 대한 상대 응답 */
    default void onRematchAnswered(boolean accepted) {
    }

    /** 왕복 지연·랙 여부 (랙 표시용) */
    default void onLagChanged(long roundTripMillis, boolean lagging) {
    }

    /**
     * 서버를 아직 찾지 못해 다시 접속을 시도하는 중 (클라이언트가 서버보다 먼저 접속한 경우 등).
     * @param attempt         지금까지 실패한 시도 횟수
     * @param remainingMillis 포기하기까지 남은 시간 (화면에 "N초 남음" 표시용)
     */
    default void onConnectRetry(int attempt, long remainingMillis) {
    }

    /**
     * 서버 접속 최종 실패 (30초 동안 서버를 찾지 못함 등). 상태는 이미 DISCONNECTED.
     * 화면은 주소를 다시 확인하도록 IP 입력란으로 돌아가면 된다. 기본 구현은 showError로 전달
     */
    default void onConnectFailed(String message) {
        showError(message);
    }

    /** 연결 종료. P2P 첫 화면으로 돌아감. peerLeft면 상대가 정상적으로 나간 것 */
    default void onConnectionClosed(String reason, boolean peerLeft) {
    }

    /** 상대가 대기실에서 보낸 채팅 메시지 */
    default void onChatReceived(String text) {
    }
}
