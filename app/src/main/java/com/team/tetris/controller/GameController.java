package com.team.tetris.controller;

import com.team.tetris.domain.Settings;
import com.team.tetris.service.GameEngineService;
import com.team.tetris.view.GameView;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

public class GameController {

    private final GameEngineService gameEngine;
    private final GameView gameView;
    private final Settings settings;
    private final GameOverController gameOverController;

    private final AnimationTimer gameLoop;
    private long lastTickMillis;
    private long pausedAtMillis;
    private Runnable onQuit;

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

    // Main에서 메인메뉴 복귀 동작 연결
    public void setOnQuit(Runnable onQuit) {
        this.onQuit = onQuit;
    }

    //최초화면 표시
    public void startGame() {
        gameView.applyColorBlindMode(settings.isColorBlindMode());

        gameEngine.startGame();
        lastTickMillis = System.nanoTime() / 1_000_000;   // AnimationTimer 와 같은 기준 시각

        gameView.setControlKeyGuide(
                getKeyName(Settings.KeyAction.MOVE_LEFT),
                getKeyName(Settings.KeyAction.MOVE_RIGHT),
                getKeyName(Settings.KeyAction.SOFT_DROP),
                getKeyName(Settings.KeyAction.ROTATE),
                getKeyName(Settings.KeyAction.HARD_DROP),
                getKeyName(Settings.KeyAction.SKIP)
        );

        render();
        gameLoop.start();
    }

    private String getKeyName(Settings.KeyAction action) {
        return settings.getKey(action).getName();
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
        long nowMillis = System.nanoTime() / 1_000_000;

        if (gameEngine.isPaused()) {
            // 일시정지한 시간은 낙하 대기 시간에서 제외
            lastTickMillis += nowMillis - pausedAtMillis;
        } else {
            pausedAtMillis = nowMillis;
        }
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

        Settings.KeyAction action= settings.findGameAction(key);
        if (action == null) {
            return;
        }

        if (action == Settings.KeyAction.PAUSE) {
            togglePause();
            return;
        }

        if (action == Settings.KeyAction.EXIT) {
            quitGame();
            Platform.exit();
            return;
        }

        if (action == Settings.KeyAction.QUIT) {
            quitGame();
            if (onQuit != null) {
                onQuit.run();
            }
            return;
        }

        if (gameEngine.isPaused()) {
            return;
        }

        switch (action) {
            case MOVE_LEFT  -> gameEngine.moveLeft();
            case MOVE_RIGHT -> gameEngine.moveRight();
            case SOFT_DROP  -> gameEngine.moveDown();
            case ROTATE     -> gameEngine.rotate();
            case HARD_DROP  -> gameEngine.hardDrop();
            case SKIP       -> gameEngine.skipCurrentBlock();
            default         -> { }
        }

        if (gameEngine.isGameOver()) {
        handleGameOver();
        return;
    }

        render();
    }

    public void render() {
        gameView.render(
                gameEngine.getBoardCells(),
                gameEngine.getCurrentBlock(),
                gameEngine.getNextBlock(),
                gameEngine.getScore(),
                gameEngine.getLevel(),
                gameEngine.isPaused()
        );
        gameView.updateSkipInfo(
                getKeyName(Settings.KeyAction.SKIP),
                gameEngine.getSkipsRemaining()
        );
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