package com.team.tetris.service;


public class LevelService {

    //속도 오르는데 필요한 점수
    private static final int LINE_SCORE_PER_LEVEL = 30;

    //최초 블럭 떨어지는 속도
    private static final long BASE_DROP_INTERVAL_MILLIS = 1000;

    //속도 줄어드는 간격
    private static final long DROP_INTERVAL_STEP_MILLIS = 150;

    //최소 낙하 간격
    private static final long MIN_DROP_INTERVAL_MILLIS = 100;

    private int level;

    public LevelService() {
        reset();
    }

    public void reset() {
        level = 1;
    }

    public int updateLevel(int lineScore) {
        level = 1 + Math.max(lineScore, 0) / LINE_SCORE_PER_LEVEL;
        return level;
    }

    public int getLevel() {
        return level;
    }

    public long getDropIntervalMillis() {
        long interval = BASE_DROP_INTERVAL_MILLIS - (level - 1) * DROP_INTERVAL_STEP_MILLIS;
        return Math.max(interval, MIN_DROP_INTERVAL_MILLIS);
    }
}