package com.team.tetris.domain;

/**
 * 대전 진행 상태: 시작 대기, 진행 중, 일시정지, 종료.
 */
public enum BattleState {
    WAITING, RUNNING, PAUSED, FINISHED
}
