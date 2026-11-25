package com.team.tetris.controller;

import java.util.EnumMap;
import java.util.Map;

import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.BattleState;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.service.BattleService;
import com.team.tetris.service.PlayerAgent;
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
    private final Map<PlayerId, PlayerAgent> agents = new EnumMap<>(PlayerId.class);

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
            Platform.runLater(() -> { if (localBattle) showResult(result); })
        );
    }

    public void startLocal(BattleConfig config) {
        lastConfig = config;
        localBattle = true;

        view.clearResult();
        service.startLocalBattle(config);
        agents.values().forEach(PlayerAgent::reset);
        renderer.start();
        render();
        view.requestFocus();
    }

    /**
     * 플레이어의 입력을 봇으로 대체. null을 넘기면 다시 사람 조작으로 돌아간다.
     * 봇이 설정된 플레이어의 키 입력은 무시된다. startLocal 전에 호출.
     */
    public void setAgent(PlayerId player, PlayerAgent agent) {
        if (agent == null) {
            agents.remove(player);
        } else {
            agents.put(player, agent);
        }
        view.setControlsVisible(player, agent == null);
    }

    public void clearAgents() {
        for (PlayerId player : agents.keySet()) {
            view.setControlsVisible(player, true);
        }
        agents.clear();
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
        if (!pressed || agents.containsKey(player)) return;

        KeyAction action = settings.findBattleAction(player, key);
        if (action == null) return;

        // 싱글플레이와 동일하게 OS 키 반복을 사용한다.
        service.submitAction(player, action);
    }

    private void driveAgents() {
        if (agents.isEmpty() || service.getState() != BattleState.RUNNING) return;

        for (Map.Entry<PlayerId, PlayerAgent> entry : agents.entrySet()) {
            PlayerId player = entry.getKey();
            var snapshot = service.getPlayerSnapshot(player);
            if (snapshot == null) continue;

            var actions = entry.getValue().decide(snapshot);
            if (actions == null) continue;

            for (KeyAction action : actions) {
                if (action == null) continue;
                service.submitAction(player, action);
            }
        }
    }

    private void render() {
        driveAgents();
        view.applyColorBlindMode(settings.isColorBlindMode());
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

        var first = service.getPlayerSnapshot(PlayerId.PLAYER1);
        var second = service.getPlayerSnapshot(PlayerId.PLAYER2);
        int p1 = first == null ? 0 : first.getScore();
        int p2 = second == null ? 0 : second.getScore();
        String winner = result.isDraw() ? "DRAW" :
            result.getWinner() == PlayerId.PLAYER1 ? "PLAYER 1 WINS!" :
            result.getWinner() == PlayerId.PLAYER2 ? "PLAYER 2 WINS!" : "BATTLE FINISHED";
        String reason = switch (result.getReason()) {
            case GAME_OVER -> result.isDraw() ? "BOTH PLAYERS TOPPED OUT" :
                (result.getWinner() == PlayerId.PLAYER1 ? "PLAYER 2 TOPPED OUT" : "PLAYER 1 TOPPED OUT");
            case TIME_UP -> "TIME UP — " + (result.isDraw() ? "EQUAL SCORE" : "HIGHER SCORE WINS");
            case PLAYER_LEFT -> "OPPONENT LEFT";
            case CONNECTION_LOST -> "CONNECTION LOST";
        };
        view.showFinish(winner, reason, p1, p2, localBattle);
    }

    /** Show the authoritative network result from the current computer's perspective. */
    public void showP2PResult(MessagePayload.Result result, boolean isHost) {
        if (result == null) return;
        renderer.stop(); // Do not overwrite the result with a stopped battle snapshot.

        PlayerId me = isHost ? PlayerId.PLAYER1 : PlayerId.PLAYER2;
        String title;
        if (result.draw()) title = "DRAW";
        else if (result.winner() == null) title = "BATTLE FINISHED";
        else {
            boolean hostWon = NetworkController.HOST_PLAYER.equals(result.winner());
            boolean clientWon = NetworkController.CLIENT_PLAYER.equals(result.winner());
            if (!hostWon && !clientWon) title = "BATTLE FINISHED";
            else title = (hostWon && me == PlayerId.PLAYER1)
                || (clientWon && me == PlayerId.PLAYER2) ? "YOU WIN!" : "YOU LOSE";
        }

        String reason = switch (result.reason()) {
            case "GAME_OVER" -> result.draw() ? "BOTH PLAYERS TOPPED OUT" :
                NetworkController.HOST_PLAYER.equals(result.winner())
                    ? "CLIENT TOPPED OUT" : "SERVER TOPPED OUT";
            case "TIME_UP" -> "TIME UP — " + (result.draw() ? "EQUAL SCORE" : "HIGHER SCORE WINS");
            case "PLAYER_LEFT" -> "OPPONENT LEFT";
            case "CONNECTION_LOST" -> "CONNECTION LOST";
            default -> result.reason();
        };
        view.showFinish(title, reason,
            view.getDisplayedScore(PlayerId.PLAYER1),
            view.getDisplayedScore(PlayerId.PLAYER2), false);
    }

    public void stopRenderingOnly() { renderer.stop(); service.clearInputStates(); }

    public void clearInputStates() {
        service.clearInputStates();
    }

    public void stop() {
        renderer.stop();
        service.clearInputStates();
        service.stop();
    }
}