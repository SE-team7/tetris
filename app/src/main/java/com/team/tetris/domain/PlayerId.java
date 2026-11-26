package com.team.tetris.domain;

/**
 * 대전 모드 플레이어 구분.
 */
public enum PlayerId {
    PLAYER1, PLAYER2;

    // 상대 플레이어 반환
    public PlayerId opponent() {
        return this == PLAYER1 ? PLAYER2 : PLAYER1;
    }
}
