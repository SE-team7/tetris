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

    /** 연결 종료. P2P 첫 화면으로 돌아감. peerLeft면 상대가 정상적으로 나간 것 */
    default void onConnectionClosed(String reason, boolean peerLeft) {
    }
}
