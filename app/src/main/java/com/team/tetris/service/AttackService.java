package com.team.tetris.service;

import java.util.Objects;
import java.util.function.Consumer;

import com.team.tetris.domain.AttackQueue;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.Board;

public class AttackService {

    private final Board board;
    private final AttackQueue incomingAttackQueue;
    private final Consumer<AttackRows> attackSender;

    public AttackService(Board board,
                         AttackQueue incomingAttackQueue,
                         Consumer<AttackRows> attackSender) {
        this.board = Objects.requireNonNull(
                board, "board must not be null"
        );
        this.incomingAttackQueue = Objects.requireNonNull(
                incomingAttackQueue,
                "incomingAttackQueue must not be null"
        );
        this.attackSender = attackSender;
    }

    // 생성된 공격 행을 상대에게 전달
    public void sendAttack(AttackRows attackRows) {
        Objects.requireNonNull(
                attackRows, "attackRows must not be null"
        );

        if (attackRows.isEmpty()) {
            return;
        }

        if (attackSender != null) {
            attackSender.accept(attackRows);
        }
    }

    // 받은 공격을 자신의 대기열에 추가
    public void receiveAttack(AttackRows attackRows) {
        incomingAttackQueue.add(attackRows);
    }

    // 대기열을 변경하지 않고 화면 표시용 복사본 반환
    public AttackRows getPendingAttack() {
        return incomingAttackQueue.snapshot();
    }

    public int getPendingRowCount() {
        return incomingAttackQueue.getRowCount();
    }

    // 대기 공격 전체 적용 후 상단 초과 여부 반환
    public boolean applyAttack() {
        AttackRows pending = incomingAttackQueue.drain();

        if (pending.isEmpty()) {
            return false;
        }

        return board.insertAttackRows(pending);
    }

    // 새 게임 시작 시 대기열 초기화
    public void reset() {
        incomingAttackQueue.clear();
    }
}