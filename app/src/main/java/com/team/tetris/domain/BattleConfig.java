package com.team.tetris.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * 대전 설정: 모드, 난이도, 제한시간.
 * P2P에서 서버가 고른 설정을 상대에게 그대로 전달할 수 있도록 Serializable.
 */
public final class BattleConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    // 시간제한 모드에서 고를 수 있는 제한시간(초)
    public static final Set<Integer> TIME_LIMIT_OPTIONS = Set.of(180, 300, 600);

    private final BattleMode mode;
    private final Difficulty difficulty;
    private final int timeLimitSeconds;

    public BattleConfig(BattleMode mode, Difficulty difficulty, int timeLimitSeconds) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");

        if (mode == BattleMode.TIME_LIMIT) {
            if (!TIME_LIMIT_OPTIONS.contains(timeLimitSeconds)) {
                throw new IllegalArgumentException(
                        "time limit must be one of " + TIME_LIMIT_OPTIONS + ": " + timeLimitSeconds);
            }
        } else if (timeLimitSeconds != 0) {
            throw new IllegalArgumentException("time limit must be 0 unless TIME_LIMIT mode");
        }
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public static BattleConfig normal(Difficulty difficulty) {
        return new BattleConfig(BattleMode.NORMAL, difficulty, 0);
    }

    // 아이템 모드는 난이도를 쓰지 않으므로 엔진 기본값(NORMAL)을 넣어 둔다
    public static BattleConfig item() {
        return new BattleConfig(BattleMode.ITEM, Difficulty.NORMAL, 0);
    }

    public static BattleConfig timeLimit(Difficulty difficulty, int seconds) {
        return new BattleConfig(BattleMode.TIME_LIMIT, difficulty, seconds);
    }

    public BattleMode getMode() {
        return mode;
    }

    // 일반·시간제한 난이도. 아이템 모드에서는 사용하지 않음
    public Difficulty getDifficulty() {
        return difficulty;
    }

    // 시간제한 모드: 180·300·600, 그 외 0
    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public boolean isItemMode() {
        return mode == BattleMode.ITEM;
    }

    public boolean isTimeLimited() {
        return mode == BattleMode.TIME_LIMIT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BattleConfig other)) {
            return false;
        }
        return mode == other.mode
                && difficulty == other.difficulty
                && timeLimitSeconds == other.timeLimitSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, difficulty, timeLimitSeconds);
    }

    @Override
    public String toString() {
        return "BattleConfig{" + mode + ", " + difficulty + ", " + timeLimitSeconds + "s}";
    }
}
