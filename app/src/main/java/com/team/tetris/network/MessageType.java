package com.team.tetris.network;

/**
 * P2P 메시지 종류.
 * CONFIG ~ LEAVE는 명세의 게임 메시지이고,
 * PING·PONG은 NetworkService가 지연(랙)·연결 끊김을 감지하기 위해 내부에서만 주고받는다.
 * (PING·PONG은 리스너로 전달되지 않음)
 */
public enum MessageType {
    CONFIG,
    READY,
    START,
    ATTACK,
    SNAPSHOT,
    GAME_OVER,
    RESULT,
    PAUSE,
    RESUME,
    REMATCH_REQUEST,
    REMATCH_RESPONSE,
    LEAVE,
    PING,
    PONG;

    /** 빠뜨리지 않고 순서대로 처리해야 하는 이벤트인지. SNAPSHOT·PING·PONG만 아니다. */
    public boolean isEvent() {
        return this != SNAPSHOT && this != PING && this != PONG;
    }

    /** NetworkService 내부용 메시지인지 */
    public boolean isControl() {
        return this == PING || this == PONG;
    }
}
