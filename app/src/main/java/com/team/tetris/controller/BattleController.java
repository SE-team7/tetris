package com.team.tetris.controller;

import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.BattleState;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.service.BattleService;
import com.team.tetris.view.BattleView;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

public class BattleController {

    private final BattleService service;
    private final BattleView view;
    private final Settings settings;
    private final Runnable onBack;
    private final AnimationTimer renderer;

    private BattleConfig lastConfig;
    private boolean localBattle;

    public BattleController(
        BattleService service,
        BattleView view,
        Settings settings,
        Runnable onBack
    ) {
        this.service = service;
        this.view = view;
        this.settings = settings;
        this.onBack = onBack;

        renderer = new AnimationTimer() {
            private long last;

            @Override
            public void handle(long now) {
                if (now - last < 33_000_000L) return;
                last = now;
                render();
            }
        };

        service.setResultListener(result ->
            Platform.runLater(() -> showResult(result))
        );
    }

    public void startLocal(BattleConfig config) {
        lastConfig = config;
        localBattle = true;

        view.clearResult();
        service.startLocalBattle(config);
        renderer.start();
        render();
        view.requestFocus();
    }

    public void startRendering() {
        localBattle = false;
        view.clearResult();
        renderer.start();
        render();
        view.requestFocus();
    }

    public void handleKeyPressed(KeyCode key) {
        if (key == null) return;

        if (key == KeyCode.ESCAPE) {
            stop();
            onBack.run();
            return;
        }

        if (key == KeyCode.ENTER
            && localBattle
            && service.getState() == BattleState.FINISHED
            && lastConfig != null) {
            startLocal(lastConfig);
            return;
        }

        if (service.getState() != BattleState.RUNNING) return;

        handlePlayerKey(PlayerId.PLAYER1, key, true);
        handlePlayerKey(PlayerId.PLAYER2, key, true);
    }

    public void handleKeyReleased(KeyCode key) {
        if (key == null) return;

        handlePlayerKey(PlayerId.PLAYER1, key, false);
        handlePlayerKey(PlayerId.PLAYER2, key, false);
    }

    private void handlePlayerKey(
        PlayerId player,
        KeyCode key,
        boolean pressed
    ) {
        KeyAction action = settings.findBattleAction(player, key);
        if (action == null) return;

        if (isRepeatable(action)) {
            service.submitKeyState(player, action, pressed);
        } else if (pressed) {
            service.submitAction(player, action);
        }
    }

    private boolean isRepeatable(KeyAction action) {
        return action == KeyAction.MOVE_LEFT
            || action == KeyAction.MOVE_RIGHT
            || action == KeyAction.SOFT_DROP;
    }

    private void render() {
        view.renderPlayer(
            PlayerId.PLAYER1,
            service.getPlayerSnapshot(PlayerId.PLAYER1)
        );

        view.renderPlayer(
            PlayerId.PLAYER2,
            service.getPlayerSnapshot(PlayerId.PLAYER2)
        );

        long remaining = service.getRemainingMillis();
        if (remaining >= 0) {
            long seconds = remaining / 1000;
            view.setTimer(String.format(
                "TIME %02d:%02d",
                seconds / 60,
                seconds % 60
            ));
        } else view.setTimer("");

        if (service.getState() == BattleState.FINISHED) {
            showResult(service.getResult());
        }
    }

    private void showResult(BattleResult result) {
        if (result == null) return;

        String text;

        if (result.isDraw()) {
            text = "DRAW";
        } else if (result.getWinner() == PlayerId.PLAYER1) {
            text = "PLAYER 1 WIN";
        } else if (result.getWinner() == PlayerId.PLAYER2) {
            text = "PLAYER 2 WIN";
        } else {
            text = "BATTLE FINISHED";
        }

        view.showResult(
            text,
            localBattle ? "ENTER : REMATCH" : ""
        );
    }

    public void clearInputStates() {
        service.clearInputStates();
    }

    public void stop() {
        renderer.stop();
        service.clearInputStates();
        service.stop();
    }
}