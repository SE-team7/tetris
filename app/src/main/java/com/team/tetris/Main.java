package com.team.tetris;

import java.util.function.Consumer;

import com.team.tetris.controller.*;
import com.team.tetris.domain.*;
import com.team.tetris.network.NetworkEvents;
import com.team.tetris.network.NetworkService;
import com.team.tetris.service.*;
import com.team.tetris.util.*;
import com.team.tetris.view.*;
import com.team.tetris.view.BattleModeView.BattleModeItem;
import com.team.tetris.view.BattleTypeView.BattleTypeItem;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    private BattleModeItem battleMode = BattleModeItem.NORMAL;
    private BattleTypeItem battleType = BattleTypeItem.LOCAL;
    private int battleMinutes;
    private boolean hostFlow;
    private boolean p2pBattleShown;

    private BattleModeController battleModeController;
    private BattleTypeController battleTypeController;
    private TimeLimitController timeLimitController;
    private P2PConnectionController p2pConnectionController;
    private P2PWaitingController p2pWaitingController;

    private BattleService battleService;
    private NetworkController networkController;

    @Override
    public void start(Stage stage) {
        stage.setTitle("DP7");

        MainMenuView mainMenuView = new MainMenuView();
        SettingsView settingsView = new SettingsView();
        ScoreboardView scoreboardView = new ScoreboardView();
        GameView gameView = new GameView();
        GameOverView gameOverView = new GameOverView();

        BattleModeView battleModeView = new BattleModeView();
        BattleTypeView battleTypeView = new BattleTypeView();
        TimeLimitView timeLimitView = new TimeLimitView();
        P2PConnectionView p2pConnectionView = new P2PConnectionView();
        P2PWaitingView p2pWaitingView = new P2PWaitingView();
        BattleView battleView = new BattleView();

        FileManager files = new FileManager();
        ScoreboardService scoreboardService = new ScoreboardService(files);
        SettingsService settingsService = new SettingsService(files);
        Settings settings = settingsService.load();

        GameEngineService gameEngine = new GameEngineService();
        battleService = new BattleService();

        NetworkService networkService = new NetworkService();
        networkController = new NetworkController(
            networkService,
            battleService,
            null
        );

        SceneManager scenes = new SceneManager(
            stage,
            mainMenuView,
            settingsView,
            scoreboardView,
            gameView,
            gameOverView,
            battleModeView,
            battleTypeView,
            timeLimitView,
            p2pConnectionView,
            p2pWaitingView,
            battleView
        );

        scenes.applyScreenSize(settings.getScreenSize());

        ScoreboardController scoreboardController = new ScoreboardController(
            scoreboardService,
            scoreboardView,
            scenes::showScoreboard,
            scenes::showMainMenu
        );

        GameOverController gameOverController = new GameOverController(
            scoreboardService,
            scoreboardController,
            gameOverView,
            scenes::showGameOver
        );

        GameController gameController = new GameController(
            gameEngine,
            gameView,
            settings,
            gameOverController,
            settings.getDifficulty(),
            false
        );

        MainMenuController mainMenuController = new MainMenuController(
            mainMenuView,
            scenes,
            gameController,
            scoreboardController,
            settings
        );

        gameController.setOnQuit(mainMenuController::show);

        SettingsController settingsController = new SettingsController(
            settingsView,
            settingsService,
            scoreboardService,
            scenes,
            settings
        );

        Runnable[] battleBack = { mainMenuController::show };

        BattleController battleController = new BattleController(
            battleService,
            battleView,
            settings,
            () -> battleBack[0].run()
        );

        battleTypeController = new BattleTypeController(
            battleTypeView,
            type -> {
                battleType = type;
                battleMinutes = 0;

                if (type == BattleTypeItem.LOCAL) {
                    hostFlow = false;
                    battleModeController.show();
                    scenes.showBattleMode();
                } else {
                    p2pConnectionController.show();
                    scenes.showP2PConnection();
                }
            },
            mainMenuController::show
        );

        battleModeController = new BattleModeController(
            battleModeView,
            mode -> {
                battleMode = mode;
                battleMinutes = 0;

                if (mode == BattleModeItem.TIME_LIMIT) {
                    timeLimitController.show();
                    scenes.showTimeLimit();
                    return;
                }

                if (battleType == BattleTypeItem.P2P) {
                    enterP2PWaiting(settings, scenes);
                    return;
                }

                startLocalBattle(
                    battleController,
                    battleView,
                    scenes,
                    settings,
                    battleBack
                );
            },
            () -> {
                if (battleType == BattleTypeItem.P2P && hostFlow) {
                    p2pConnectionController.show();
                    scenes.showP2PConnection();
                } else {
                    battleTypeController.show();
                    scenes.showBattleType();
                }
            }
        );

        timeLimitController = new TimeLimitController(
            timeLimitView,
            minutes -> {
                battleMinutes = minutes;

                if (battleType == BattleTypeItem.P2P) {
                    enterP2PWaiting(settings, scenes);
                    return;
                }

                startLocalBattle(
                    battleController,
                    battleView,
                    scenes,
                    settings,
                    battleBack
                );
            },
            () -> {
                battleModeController.show();
                scenes.showBattleMode();
            }
        );

        p2pConnectionController = new P2PConnectionController(
            p2pConnectionView,
            () -> {
                try {
                    networkController.host(NetworkService.DEFAULT_PORT);
                    hostFlow = true;
                    battleType = BattleTypeItem.P2P;
                    battleModeController.show();
                    scenes.showBattleMode();
                } catch (RuntimeException e) {
                    p2pConnectionController.showError(e.getMessage());
                }
            },
            ip -> {
                try {
                    networkController.join(ip, NetworkService.DEFAULT_PORT);
                    hostFlow = false;
                    battleType = BattleTypeItem.P2P;
                    p2pWaitingController.showClient(ip);
                    scenes.showP2PWaiting();
                } catch (RuntimeException e) {
                    p2pConnectionController.showError(e.getMessage());
                }
            },
            () -> {
                battleTypeController.show();
                scenes.showBattleType();
            }
        );

        p2pWaitingController = new P2PWaitingController(
            p2pWaitingView,
            () -> {},
            () -> {
                networkController.leave();

                if (hostFlow) {
                    battleModeController.show();
                    scenes.showBattleMode();
                } else {
                    p2pConnectionController.show();
                    scenes.showP2PConnection();
                }
            },
            ready -> {
                try {
                    if (networkController.getState() == NetworkController.State.CONNECTED
                        && networkController.getConfig() != null) {
                        networkController.setReady(ready);
                    } else {
                        p2pWaitingController.showError(
                            "연결 또는 게임 설정을 기다려주세요."
                        );
                    }
                } catch (RuntimeException e) {
                    p2pWaitingController.showError(e.getMessage());
                }
            }
        );

        networkController.setEvents(new NetworkEvents() {
            @Override
            public void showError(String message) {
                p2pConnectionController.showError(message);
                p2pWaitingController.showError(message);
            }

            @Override
            public void onBattleStarted() {
                battleView.setLag("");
            }

            @Override
            public void onLagChanged(long roundTripMillis, boolean lagging) {
                battleView.setLag(
                    lagging ? "⚠ LAG " + roundTripMillis + "ms - 네트워크 지연 중" : ""
                );
            }

            @Override
            public void onConnectionClosed(String reason, boolean peerLeft) {
                battleView.setLag("");
                // p2pBattleShown은 감시 타이머가 먼저 false로 바꿀 수 있어 실제 화면으로 판단
                boolean onBattleScreen =
                    stage.getScene() == battleView.getRoot().getScene();

                // 대전 중 상대가 정상적으로 나간 경우: 결과(승리)를 대전 화면에 그대로 보여준다
                if (peerLeft && onBattleScreen) {
                    battleView.setStatus(reason);
                    return;
                }

                // 연결 끊김(또는 대기실에서 상대 퇴장): 에러 표시 후 P2P 첫 화면으로
                returnToP2PStart(battleController, scenes, reason, onBattleScreen);
            }
        });

        mainMenuView.getRoot().setOnKeyPressed(
            e -> mainMenuController.handleKey(e.getCode())
        );

        settingsView.getRoot().setOnKeyPressed(
            e -> settingsController.handleKey(e.getCode())
        );

        gameView.getRoot().setOnKeyPressed(
            e -> gameController.handleKey(e.getCode())
        );

        gameOverView.getRoot().setOnKeyPressed(
            e -> gameOverController.handleKey(e.getCode())
        );

        scoreboardView.getRoot().addEventFilter(
            KeyEvent.KEY_PRESSED,
            e -> {
                scoreboardController.handleKey(e.getCode());
                e.consume();
            }
        );

        bindMenu(
            battleTypeView.getRoot(),
            battleTypeController::handleKey,
            mainMenuController::show
        );

        bindMenu(
            battleModeView.getRoot(),
            battleModeController::handleKey,
            mainMenuController::show
        );

        bindMenu(
            timeLimitView.getRoot(),
            timeLimitController::handleKey,
            mainMenuController::show
        );

        bindMenu(
            p2pConnectionView.getRoot(),
            p2pConnectionController::handleKey,
            mainMenuController::show
        );

        bindMenu(
            p2pWaitingView.getRoot(),
            p2pWaitingController::handleKey,
            mainMenuController::show
        );

        battleView.getRoot().addEventFilter(
            KeyEvent.KEY_PRESSED,
            e -> {
                battleController.handleKeyPressed(e.getCode());
                e.consume();
            }
        );

        battleView.getRoot().addEventFilter(
            KeyEvent.KEY_RELEASED,
            e -> {
                battleController.handleKeyReleased(e.getCode());
                e.consume();
            }
        );

        battleView.getRoot().focusedProperty().addListener(
            (obs, oldValue, focused) -> {
                if (!focused) battleController.clearInputStates();
            }
        );

        Timeline networkWatcher = new Timeline(
            new KeyFrame(
                Duration.millis(100),
                e -> updateP2P(
                    battleController,
                    battleView,
                    scenes,
                    settings
                )
            )
        );
        networkWatcher.setCycleCount(Timeline.INDEFINITE);
        networkWatcher.play();

        mainMenuController.show();
    }

    private void startLocalBattle(
        BattleController controller,
        BattleView view,
        SceneManager scenes,
        Settings settings,
        Runnable[] back
    ) {
        prepareBattleView(view, battleMode, battleMinutes, false);

        back[0] = () -> {
            controller.stop();
            battleModeController.show();
            scenes.showBattleMode();
        };

        controller.startLocal(getBattleConfig(settings));
        scenes.showBattle();
    }

    private void enterP2PWaiting(
        Settings settings,
        SceneManager scenes
    ) {
        BattleConfig config = getBattleConfig(settings);

        if (hostFlow) {
            p2pWaitingController.showHost(
                String.join(", ", networkController.getHostAddresses()),
                getWaitingModeText()
            );

            if (networkController.getState()
                == NetworkController.State.CONNECTED) {
                networkController.sendBattleConfig(config);
            }
        }

        scenes.showP2PWaiting();
    }

    private void updateP2P(
        BattleController controller,
        BattleView view,
        SceneManager scenes,
        Settings settings
    ) {
        if (battleType != BattleTypeItem.P2P) return;

        if (hostFlow
            && networkController.getState() == NetworkController.State.CONNECTED
            && networkController.getConfig() == null
            && battleMode != BattleModeItem.BACK) {

            try {
                networkController.sendBattleConfig(getBattleConfig(settings));
            } catch (RuntimeException ignored) {}
        }

        if (networkController.isPeerReady()) {
            p2pWaitingController.showOpponentReady();
        }

        if (networkController.getState() == NetworkController.State.IN_BATTLE
            && !p2pBattleShown) {

            p2pBattleShown = true;

            BattleConfig config = battleService.getConfig();
            BattleModeItem mode = modeFrom(config);

            prepareBattleView(
                view,
                mode,
                config != null && config.isTimeLimited()
                    ? config.getTimeLimitSeconds() / 60
                    : 0,
                true
            );

            controller.startRendering();
            scenes.showBattle();
        }

        if (networkController.getState() != NetworkController.State.IN_BATTLE) {
            p2pBattleShown = false;
        }
    }

    private void returnToP2PStart(
        BattleController controller,
        SceneManager scenes,
        String reason,
        boolean onBattleScreen
    ) {
        if (onBattleScreen) {
            controller.stop();
        }
        p2pBattleShown = false;

        hostFlow = false;
        p2pConnectionController.show();
        p2pConnectionController.showError(reason);
        scenes.showP2PConnection();
    }

    private BattleModeItem modeFrom(BattleConfig config) {
        if (config == null) return battleMode;
        if (config.isItemMode()) return BattleModeItem.ITEM;
        if (config.isTimeLimited()) return BattleModeItem.TIME_LIMIT;
        return BattleModeItem.NORMAL;
    }

    private BattleConfig getBattleConfig(Settings settings) {
        return switch (battleMode) {
            case NORMAL ->
                BattleConfig.normal(settings.getDifficulty());

            case ITEM ->
                BattleConfig.item();

            case TIME_LIMIT ->
                BattleConfig.timeLimit(
                    settings.getDifficulty(),
                    battleMinutes * 60
                );

            case BACK ->
                BattleConfig.normal(settings.getDifficulty());
        };
    }

    private void prepareBattleView(
        BattleView view,
        BattleModeItem mode,
        int minutes,
        boolean p2p
    ) {
        view.setItemMode(mode == BattleModeItem.ITEM);
        view.clearResult();

        view.setStatus(
            (p2p ? "P2P / " : "LOCAL / ") + getModeText(mode)
        );

        view.setTimer(
            minutes > 0
                ? String.format("TIME %02d:00", minutes)
                : "TIME --:--"
        );
    }

    private String getModeText(BattleModeItem mode) {
        return switch (mode) {
            case NORMAL -> "NORMAL";
            case ITEM -> "ITEM";
            case TIME_LIMIT -> "TIME LIMIT";
            case BACK -> "";
        };
    }

    private String getWaitingModeText() {
        if (battleMode == BattleModeItem.TIME_LIMIT
            && battleMinutes > 0) {
            return "TIME LIMIT / " + battleMinutes + "분";
        }

        return getModeText(battleMode);
    }

    private void bindMenu(
        Parent root,
        Consumer<KeyCode> handler,
        Runnable mainMenu
    ) {
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                mainMenu.run();
                e.consume();
                return;
            }

            handler.accept(e.getCode());
        });
    }

    @Override
    public void stop() {
        if (networkController != null) networkController.shutdown();
        if (battleService != null) battleService.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}