package com.team.tetris.controller;

import com.team.tetris.domain.Settings;
import com.team.tetris.service.GameEngineService;
import com.team.tetris.view.GameView;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;

public class GameController {

    // 키 입력이 변환될 게임 동작
    public enum GameAction {
        MOVE_LEFT, MOVE_RIGHT, MOVE_DOWN, ROTATE, HARD_DROP, PAUSE
    }

    private final GameEngineService gameEngine;
    private final GameView gameView;
    private final Settings settings;
    private final GameOverController gameOverController;

    private final AnimationTimer gameLoop;
    private long lastTickMillis;

    public GameController(GameEngineService gameEngine,
                          GameView gameView,
                          Settings settings,
                          GameOverController gameOverController) {
        this.gameEngine = gameEngine;
        this.gameView = gameView;
        this.settings = settings;
        this.gameOverController = gameOverController;

        // 매 프레임 실행되는 게임 루프
        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long nowNanos) {
                loop(nowNanos / 1_000_000);
            }
        };
    }

    //최초화면 표시
    public void startGame() {
        gameView.applyColorBlindMode(settings.isColorBlindMode());

        gameEngine.startGame();
        lastTickMillis = System.nanoTime() / 1_000_000;   // AnimationTimer 와 같은 기준 시각

        render();
        gameLoop.start();
    }

    private void loop(long nowMillis) {
        //정지상태면 대기
        if (gameEngine.isPaused()) {
            return;
        }

        //마지막 틱으로부터 틱 간격이 지났으면 틱 실행
        if (nowMillis - lastTickMillis > gameEngine.getDropIntervalMillis()) {
            lastTickMillis = nowMillis;
            updateGame();
        }
    }

    public void updateGame() {
        gameEngine.tick();

        if (gameEngine.isGameOver()) {
            handleGameOver();
            return;
        }

        render();
    }

    public void togglePause() {
        gameEngine.togglePause();
        render();
    }

    public void quitGame() {
        gameLoop.stop();
        gameEngine.quitGame();
    }

    public void handleKey(KeyCode key) {
        if (gameEngine.isGameOver()) {
            return;
        }

        GameAction action = settings.findGameAction(key);
        if (action == null) {
            return;
        }

        if (action == GameAction.PAUSE) {
            togglePause();
            return;
        }

        if (gameEngine.isPaused()) {
            return;
        }

        switch (action) {
            case MOVE_LEFT  -> gameEngine.moveLeft();
            case MOVE_RIGHT -> gameEngine.moveRight();
            case MOVE_DOWN  -> gameEngine.moveDown();
            case ROTATE     -> gameEngine.rotate();
            case HARD_DROP  -> gameEngine.hardDrop();
            default         -> { }
        }

        render();
    }

    public void render() {
        gameView.render(
                gameEngine.getBoardCells(),
                gameEngine.getCurrentBlock(),
                gameEngine.getNextBlock(),
                gameEngine.getScore(),
                gameEngine.getLevel()
        );

        // 3번 UI 연동: 일시정지 상태에 따라 버튼 문구 갱신
    gameView.setPaused(gameEngine.isPaused());

    
    }

    public void handleGameOver() {
        gameLoop.stop();
        render();
        gameOverController.show(
                gameEngine.getScore(),
                gameEngine.getLevel(),
                gameEngine.getClearedLines()
        );
    }
}