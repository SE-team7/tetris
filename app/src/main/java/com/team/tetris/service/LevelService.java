package com.team.tetris.service;

import com.team.tetris.domain.Difficulty;

public class LevelService {

    //속도 오르는데 필요한 점수
    private static final int LINE_SCORE_PER_LEVEL = 30;

    //최초 블럭 떨어지는 속도
    private static final long BASE_DROP_INTERVAL_MILLIS = 1000;

    //속도 줄어드는 간격 (NORMAL 기준)
    private static final long DROP_INTERVAL_STEP_MILLIS = 80;

    //최소 낙하 간격 (NORMAL 기준)
    private static final long MIN_DROP_INTERVAL_MILLIS = 200;

    private int level;
    private long dropIntervalStepMillis = DROP_INTERVAL_STEP_MILLIS;
    private long minDropIntervalMillis = MIN_DROP_INTERVAL_MILLIS;

    public LevelService() {
        reset();
    }

    public void reset() {
        reset(Difficulty.NORMAL);
    }

    //난이도별 가속치 & 최고속도: EASY는 가속 20% 덜, 최고속도 20% 느림 / HARD는 반대
    public void reset(Difficulty difficulty) {
        level = 1;
        switch (difficulty) {
            case EASY -> {
                dropIntervalStepMillis = (long) (DROP_INTERVAL_STEP_MILLIS * 0.8);
                minDropIntervalMillis = (long) (MIN_DROP_INTERVAL_MILLIS * 1.2);
            }
            case NORMAL -> {
                dropIntervalStepMillis = DROP_INTERVAL_STEP_MILLIS;
                minDropIntervalMillis = MIN_DROP_INTERVAL_MILLIS;
            }
            case HARD -> {
                dropIntervalStepMillis = (long) (DROP_INTERVAL_STEP_MILLIS * 1.2);
                minDropIntervalMillis = (long) (MIN_DROP_INTERVAL_MILLIS * 0.8);
            }
        }
    }

    public int updateLevel(int lineScore) {
        level = 1 + Math.max(lineScore, 0) / LINE_SCORE_PER_LEVEL;
        return level;
    }

    public int getLevel() {
        return level;
    }

    public long getDropIntervalMillis() {
        long interval = BASE_DROP_INTERVAL_MILLIS - (level - 1) * dropIntervalStepMillis;
        return Math.max(interval, minDropIntervalMillis);
    }
}