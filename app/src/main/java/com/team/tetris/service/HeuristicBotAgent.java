package com.team.tetris.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.Settings.KeyAction;

/**
 * 휴리스틱 봇.
 * 조각이 새로 스폰됐을 때(현재 조각의 행이 이전보다 낮아짐 또는 타입이 바뀜) 한 번만 플랜을 짜고,
 * 생각 지연 뒤 한 틱에 모든 액션을 쏜 후 다음 조각이 등장할 때까지 침묵한다.
 * 이렇게 해야 엔진 스레드 처리 지연으로 같은 조각에 플랜이 중복 실행되는 걸 방지한다.
 */
public class HeuristicBotAgent implements PlayerAgent {

    private static final int THINK_TICKS = 30;

    private final HeuristicBotPlanner planner = new HeuristicBotPlanner();
    private final Deque<KeyAction> queue = new ArrayDeque<>();

    private int thinkTicks;
    private int lastSeenRow = Integer.MIN_VALUE;
    private BlockType lastSeenType = null;

    @Override
    public void reset() {
        queue.clear();
        thinkTicks = 0;
        lastSeenRow = Integer.MIN_VALUE;
        lastSeenType = null;
    }

    @Override
    public List<KeyAction> decide(GameSnapshot snapshot) {
        if (snapshot == null || snapshot.isGameOver()) return List.of();
        Block current = snapshot.getCurrentBlock();
        if (current == null) {
            lastSeenType = null;
            lastSeenRow = Integer.MIN_VALUE;
            return List.of();
        }

        int currentRow = current.getPosition().getRow();
        BlockType currentType = current.getType();
        boolean newPiece = lastSeenType == null
                || currentType != lastSeenType
                || currentRow < lastSeenRow;

        if (newPiece) {
            queue.clear();
            queue.addAll(planner.plan(snapshot.getBoard(), current, snapshot.getNextBlock()));
            thinkTicks = THINK_TICKS;
        }
        lastSeenType = currentType;
        lastSeenRow = currentRow;

        if (thinkTicks > 0) {
            thinkTicks--;
            return List.of();
        }

        if (queue.isEmpty()) return List.of();
        return List.of(queue.poll());
    }
}
