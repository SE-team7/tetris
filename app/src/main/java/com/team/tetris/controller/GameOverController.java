package com.team.tetris.controller;

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

    /** GameController가 게임오버 시 최종 점수·레벨·삭제 줄 수를 전달한다. */
    public void show(int score, int level, int clearedLines) {
        this.score = score;
        this.level = level;
        this.clearedLines = clearedLines;
        this.name.setLength(0);
        this.inputtingName = scoreboardService.isHighScore(score);

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
                scoreboardController.show();
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

    /** 이름과 함께 점수를 저장하고, 등록된 등수를 강조한 스코어보드로 이동한다. */
    public void submitName(String name) {
        if (!inputtingName || name == null || name.isBlank()) {
            return;
        }
        inputtingName = false;
        int rank = scoreboardService.addScore(name.trim(), score, level, clearedLines);
        scoreboardController.show(rank);
    }

    // 영문자, 숫자(키패드 제외)만 이름으로 입력 허용
    private boolean isNameKey(KeyCode key) {
        return key.isLetterKey() || (key.isDigitKey() && !key.isKeypadKey());
    }
}
