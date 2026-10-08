package com.team.tetris.service;

import com.team.tetris.domain.Score;
import com.team.tetris.util.FileManager;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 상위 점수 기록 관리. 기록은 FileManager를 통해 파일에 저장된다.
 * 파일 형식: 한 줄에 "이름,점수,레벨,삭제줄수"
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

    /** 점수 내림차순 상위 기록 (최대 MAX_SCORES개). */
    public List<Score> getTopScores() {
        return Collections.unmodifiableList(new ArrayList<>(scores));
    }

    /** 이 점수가 스코어보드에 들어갈 수 있는지 여부. */
    public boolean isHighScore(int score) {
        return scores.size() < MAX_SCORES || score > scores.get(scores.size() - 1).getScore();
    }

    /**
     * 기록을 추가하고 저장한다.
     * @return 1부터 시작하는 등수. 순위권 밖이면 -1
     */
    public int addScore(String name, int score, int level, int clearedLines) {
        if (!isHighScore(score)) {
            return -1;
        }
        // 동점이면 기존 기록이 앞 순위
        int index = 0;
        while (index < scores.size() && scores.get(index).getScore() >= score) {
            index++;
        }
        scores.add(index, new Score(sanitizeName(name), score, level, clearedLines));
        if (scores.size() > MAX_SCORES) {
            scores.remove(scores.size() - 1);
        }
        save();
        return index + 1;
    }

    /** 모든 기록 삭제. */
    public void resetScoreboard() {
        scores.clear();
        save();
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
            if (parts.length != 4) {
                continue;
            }
            try {
                loaded.add(new Score(parts[0],
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim())));
            } catch (NumberFormatException ignored) {
                // 손상된 줄은 건너뜀
            }
        }
        // 파일이 수정됐을 경우를 대비해 정렬·개수 제한 (stable sort라 동점 순서 유지)
        loaded.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        while (loaded.size() > MAX_SCORES) {
            loaded.remove(loaded.size() - 1);
        }
        return loaded;
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (Score s : scores) {
            sb.append(s.getName()).append(',')
              .append(s.getScore()).append(',')
              .append(s.getLevel()).append(',')
              .append(s.getClearedLines()).append('\n');
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
