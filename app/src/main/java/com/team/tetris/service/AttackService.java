package com.team.tetris.service;

import java.util.Objects;
import java.util.function.Consumer;

import com.team.tetris.domain.AttackQueue;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.Board;

public class AttackService {

    // 플레이어별 수신 공격 대기열
    private final AttackQueue attackQueue = new AttackQueue();

    //로컬 상대 엔진 또는 P2P 송신 기능에 연결
    private volatile Consumer<AttackRows> attackListener;

    public void setAttackListener(Consumer<AttackRows> listener) {
        this.attackListener = listener;
    }

    //자신의 엔진 스레드에서 호출
    //공격 행 생성은 Board에서 하고, 여기서는 전달만 수행
    public void sendAttack(AttackRows attackRows) {
        Objects.requireNonNull(attackRows, "attackRows must not be null");

        if (attackRows.getRowCount() == 0) {
            return;
        }

        Consumer<AttackRows> listener = attackListener;

        if (listener != null) {
            listener.accept(attackRows);
        }
    }

    // 10줄 제한·아래쪽 추가·초과분 제거는 기존 큐에서 처리
    public void receiveAttack(AttackRows attackRows) {
        attackQueue.add(attackRows);
    }


    //공격 삽입으로 상단을 초과했으면 true
    public boolean applyAttack(Board board) {
        Objects.requireNonNull(board, "board must not be null");

        AttackRows pending = attackQueue.drain();

        if (pending.getRowCount() == 0) {
            return false;
        }

        return board.insertAttackRows(pending);
    }

    // 화면 표시용 대기 공격 복사본
    public AttackRows getPendingAttack() {
        return attackQueue.snapshot();
    }

    public int getPendingRowCount() {
        return attackQueue.getRowCount();
    }

    // 새 게임 시작 시 호출
    public void reset() {
        attackQueue.clear();
    }
}