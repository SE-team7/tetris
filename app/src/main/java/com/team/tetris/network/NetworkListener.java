package com.team.tetris.network;

/**
 * NetworkService 이벤트 콜백.
 * 모두 통신 스레드에서 호출되므로, 화면을 바꿀 때는 Platform.runLater로 넘겨야 한다.
 */
public interface NetworkListener {

    /** 연결 완료 통지 */
    void onConnected();

    /** 메시지 수신 통지. 받은 순서대로 한 스레드에서 호출 (PING·PONG은 오지 않음) */
    void onMessage(NetworkMessage message);

    /**
     * 예상하지 못한 연결 종료 통지.
     * 상대 소켓 종료·응답 없음(시간 초과)일 때 호출. 자신이 disconnect()한 경우에는 호출하지 않음.
     */
    void onDisconnected();

    /** 통신 오류 통지 (접속 실패·포트 사용 중·잘못된 데이터 수신 등) */
    void onError(String message);

    /**
     * 지연 상태 통지. 랙 표시용.
     * 왕복 지연(ms)이 측정될 때마다 호출되고, lagging은 랙 기준을 넘었는지 여부.
     */
    default void onLatencyChanged(long roundTripMillis, boolean lagging) {
    }
}
