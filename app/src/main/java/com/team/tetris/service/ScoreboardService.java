package com.team.tetris.service;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Score;
import com.team.tetris.util.FileManager;

/**
 * 상위 점수 기록 관리. 기록은 FileManager를 통해 파일에 저장된다.
 *
 * 게임 모드(일반/아이템)별로 따로 순위를 매긴다. 난이도는 기록에 함께 저장되어 표시된다.
 * 순위는 동점자에게 같은 등수를 주는 방식(1, 2, 2, 4 ...)이고,
 * MAX_SCORES위 안에 드는 기록은 동점자를 포함해 모두 남긴다. (그래서 10개를 넘을 수 있음)
 *
 * 파일 형식: 한 줄에 "이름,점수,레벨,삭제줄수,난이도,게임모드"
 * (예전 형식 "이름,점수,레벨,삭제줄수"는 NORMAL 난이도, 일반 모드로 읽는다)
 */
public class ScoreboardService {

    public static final int MAX_SCORES = 10;
    private static final String FILE_NAME = "scoreboard.csv";

    private final FileManager fileManager;
    private final List<Score> scores;

    public ScoreboardService(FileManager fileManager) {
        this.fileManager = fileManager;
        this.scores = load();
    }

    /** 일반 모드 상위 기록. */
    public List<Score> getTopScores() {
        return getTopScores(GameMode.NORMAL);
    }

    /** 해당 모드의 상위 기록 (점수 내림차순, 동점자 포함 MAX_SCORES위까지). */
    public List<Score> getTopScores(GameMode gameMode) {
        return Collections.unmodifiableList(filterByMode(scores, gameMode));
    }

    /** 일반 모드 기준으로 스코어보드에 들어갈 수 있는지 여부. */
    public boolean isHighScore(int score) {
        return isHighScore(score, GameMode.NORMAL);
    }

    /** 해당 모드에서 MAX_SCORES위 안에 드는지 여부 (MAX_SCORES위와 동점이면 포함). */
    public boolean isHighScore(int score, GameMode gameMode) {
        int higher = 0;
        for (Score s : scores) {
            if (s.getGameMode() == gameMode && s.getScore() > score) {
                higher++;
            }
        }
        return higher < MAX_SCORES;
    }

    /** 일반 모드, NORMAL 난이도로 기록을 추가한다. */
    public int addScore(String name, int score, int level, int clearedLines) {
        return addScore(name, score, level, clearedLines, Difficulty.NORMAL, GameMode.NORMAL);
    }

    /**
     * 기록을 추가하고 저장한다.
     * @return 해당 모드 목록에서 새 기록이 들어간 위치(1부터, 화면에서 강조할 줄). 순위권 밖이면 -1
     */
    public int addScore(String name, int score, int level, int clearedLines,
                        Difficulty difficulty, GameMode gameMode) {
        if (!isHighScore(score, gameMode)) {
            return -1;
        }
        // 동점이면 기존 기록이 앞
        int index = 0;
        int position = 1;
        while (index < scores.size() && scores.get(index).getScore() >= score) {
            if (scores.get(index).getGameMode() == gameMode) {
                position++;
            }
            index++;
        }
        scores.add(index, new Score(sanitizeName(name), score, level, clearedLines, difficulty, gameMode));
        removeOutOfRank(scores);
        save();
        return position;
    }

    /** 모든 모드의 기록 삭제. */
    public void resetScoreboard() {
        scores.clear();
        save();
    }

    /**
     * 동점자 순위 방식으로 등수를 계산한다. (예: 점수 300, 200, 200, 100 → 1, 2, 2, 4)
     * @param sortedScores 점수 내림차순으로 정렬된 기록
     */
    public static int[] calculateRanks(List<Score> sortedScores) {
        int[] ranks = new int[sortedScores.size()];
        for (int i = 0; i < ranks.length; i++) {
            boolean tied = i > 0 && sortedScores.get(i).getScore() == sortedScores.get(i - 1).getScore();
            ranks[i] = tied ? ranks[i - 1] : i + 1;
        }
        return ranks;
    }

    private static List<Score> filterByMode(List<Score> list, GameMode gameMode) {
        List<Score> result = new ArrayList<>();
        for (Score s : list) {
            if (s.getGameMode() == gameMode) {
                result.add(s);
            }
        }
        return result;
    }

    // 모드별로 MAX_SCORES위 밖으로 밀려난 기록 제거 (list는 점수 내림차순)
    private static void removeOutOfRank(List<Score> list) {
        for (GameMode mode : GameMode.values()) {
            int[] ranks = calculateRanks(filterByMode(list, mode));
            int i = 0;
            for (var it = list.iterator(); it.hasNext(); ) {
                Score s = it.next();
                if (s.getGameMode() == mode && ranks[i++] > MAX_SCORES) {
                    it.remove();
                }
            }
        }
    }

    private List<Score> load() {
        List<Score> loaded = new ArrayList<>();
        String content;
        try {
            content = fileManager.readFile(FILE_NAME);
        } catch (UncheckedIOException e) {
            // 읽기 실패 시 빈 스코어보드로 시작 (게임은 계속 진행)
            System.err.println("스코어보드 불러오기 실패: " + e.getMessage());
            return loaded;
        }
        for (String line : content.split("\\R")) {
            String[] parts = line.split(",");
            if (parts.length != 4 && parts.length != 6) {
                continue;
            }
            try {
                Difficulty difficulty = parts.length == 6 ? Difficulty.valueOf(parts[4].trim()) : Difficulty.NORMAL;
                GameMode gameMode = parts.length == 6 ? GameMode.valueOf(parts[5].trim()) : GameMode.NORMAL;
                loaded.add(new Score(parts[0],
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim()),
                        difficulty, gameMode));
            } catch (IllegalArgumentException ignored) {
                // 손상된 줄은 건너뜀 (NumberFormatException 포함)
            }
        }
        // 파일이 수정됐을 경우를 대비해 정렬·순위 밖 기록 제거 (stable sort라 동점 순서 유지)
        loaded.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        removeOutOfRank(loaded);
        return loaded;
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (Score s : scores) {
            sb.append(s.getName()).append(',')
              .append(s.getScore()).append(',')
              .append(s.getLevel()).append(',')
              .append(s.getClearedLines()).append(',')
              .append(s.getDifficulty().name()).append(',')
              .append(s.getGameMode().name()).append('\n');
        }
        try {
            fileManager.writeFile(FILE_NAME, sb.toString());
        } catch (UncheckedIOException e) {
            // 저장 실패 시 메모리의 기록은 유지 (이번 실행 동안은 스코어보드에 보임)
            System.err.println("스코어보드 저장 실패: " + e.getMessage());
        }
    }

    // 파일 형식을 깨뜨리는 문자(쉼표, 줄바꿈) 제거
    private String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[,\\r\\n]", "").trim();
    }
}
