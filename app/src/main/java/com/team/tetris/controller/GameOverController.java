package com.team.tetris.controller;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.view.GameOverView;

import javafx.scene.input.KeyCode;

/**
 * 게임오버 화면 진행 및 점수 저장 담당.
 *
 * 화면 전환은 SceneManager(3번)가 담당하므로, 생성 시 전환 동작을 전달받는다.
 * 예) new GameOverController(scoreboardService, scoreboardController, gameOverView, sceneManager::showGameOver)
 */
public class GameOverController {

    public static final int MAX_NAME_LENGTH = 10;

    private final ScoreboardService scoreboardService;
    private final ScoreboardController scoreboardController;
    private final GameOverView gameOverView;
    private final Runnable showGameOverScene;

    private int score;
    private int level;
    private int clearedLines;
    private Difficulty difficulty = Difficulty.NORMAL;
    private GameMode gameMode = GameMode.NORMAL;
    private final StringBuilder name = new StringBuilder();
    private boolean inputtingName;

    public GameOverController(ScoreboardService scoreboardService,
                              ScoreboardController scoreboardController,
                              GameOverView gameOverView,
                              Runnable showGameOverScene) {
        this.scoreboardService = scoreboardService;
        this.scoreboardController = scoreboardController;
        this.gameOverView = gameOverView;
        this.showGameOverScene = showGameOverScene;
    }

    /** 난이도·모드 정보 없이 호출하면 NORMAL 난이도, 일반 모드 기록으로 처리한다. */
    public void show(int score, int level, int clearedLines) {
        show(score, level, clearedLines, Difficulty.NORMAL, GameMode.NORMAL);
    }

    /** GameController가 게임오버 시 최종 점수·레벨·삭제 줄 수와 플레이한 난이도·모드를 전달한다. */
    public void show(int score, int level, int clearedLines, Difficulty difficulty, GameMode gameMode) {
        this.score = score;
        this.level = level;
        this.clearedLines = clearedLines;
        this.difficulty = difficulty != null ? difficulty : Difficulty.NORMAL;
        this.gameMode = gameMode != null ? gameMode : GameMode.NORMAL;
        this.name.setLength(0);
        this.inputtingName = scoreboardService.isHighScore(score, this.gameMode);

        gameOverView.showResult(score, level, clearedLines);
        if (inputtingName) {
            gameOverView.showNameInput(score);
        }
        showGameOverScene.run();
    }

    public void handleKey(KeyCode key) {
        if (key == null) {
            return;
        }
        if (!inputtingName) {
            if (key == KeyCode.ENTER) {
                scoreboardController.show(gameMode, -1);
            }
            return;
        }

        if (key == KeyCode.ENTER) {
            submitName(name.toString());
        } else if (key == KeyCode.BACK_SPACE) {
            if (name.length() > 0) {
                name.deleteCharAt(name.length() - 1);
                gameOverView.updateName(name.toString());
            }
        } else if (isNameKey(key) && name.length() < MAX_NAME_LENGTH) {
            name.append(key.getChar());
            gameOverView.updateName(name.toString());
        }
    }

    /** 이름과 함께 점수를 저장하고, 해당 모드 스코어보드에서 새 기록을 강조해서 보여준다. */
    public void submitName(String name) {
        if (!inputtingName || name == null || name.isBlank()) {
            return;
        }
        inputtingName = false;
        int position = scoreboardService.addScore(name.trim(), score, level, clearedLines, difficulty, gameMode);
        scoreboardController.show(gameMode, position);
    }

    // 영문자, 숫자(키패드 제외)만 이름으로 입력 허용
    private boolean isNameKey(KeyCode key) {
        return key.isLetterKey() || (key.isDigitKey() && !key.isKeypadKey());
    }
}
