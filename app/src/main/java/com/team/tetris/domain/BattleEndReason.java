package com.team.tetris.domain;

/**
 * 대전 종료 사유.
 */
public enum BattleEndReason {
    GAME_OVER,       // 한쪽 또는 양쪽이 더 이상 블럭을 쌓을 수 없음
    TIME_UP,         // 시간제한 모드에서 제한시간 종료
    PLAYER_LEFT,     // 상대가 나감 (남은 플레이어 승리)
    CONNECTION_LOST  // 연결 끊김 (승자 없음, 무승부와 구분)
}
