package com.team.tetris;

import java.util.function.Consumer;

import com.team.tetris.controller.*;
import com.team.tetris.domain.*;
import com.team.tetris.network.NetworkService;
import com.team.tetris.network.RecentHostStore;
import com.team.tetris.network.NetworkEvents;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.service.*;
import com.team.tetris.util.*;
import com.team.tetris.view.*;
import com.team.tetris.view.BattleModeView.BattleModeItem;
import com.team.tetris.view.BattleTypeView.BattleTypeItem;
import com.team.tetris.view.OpponentTypeView.OpponentType;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class Main extends Application {

    private BattleModeItem battleMode = BattleModeItem.NORMAL;
    private BattleTypeItem battleType = BattleTypeItem.LOCAL;
    private int battleMinutes;
    private boolean hostFlow;
    private boolean p2pBattleShown;
    /** 클라이언트가 접속한 서버 주소 (대기실 표시용) */
    private String joinedHost = "";

    private BattleModeController battleModeController;
    private BattleTypeController battleTypeController;
    private OpponentTypeController opponentTypeController;
    private TimeLimitController timeLimitController;
    private Runnable opponentBack = () -> {};
    private P2PConnectionController p2pConnectionController;
    private P2PWaitingController p2pWaitingController;

    private BattleService battleService;
    private NetworkController networkController;

    @Override
    public void start(Stage stage) {
        stage.setTitle("DP7");
        // ESC closes the application from every screen.
        stage.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
                e.consume();
            }
        });

        MainMenuView mainMenuView = new MainMenuView();
        SettingsView settingsView = new SettingsView();
        ScoreboardView scoreboardView = new ScoreboardView();
        GameView gameView = new GameView();
        GameOverView gameOverView = new GameOverView();

        BattleModeView battleModeView = new BattleModeView();
        BattleTypeView battleTypeView = new BattleTypeView();
        OpponentTypeView opponentTypeView = new OpponentTypeView();
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
            new RecentHostStore(files)
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
            opponentTypeView,
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

                opponentBack = () -> {
                    battleModeController.show();
                    scenes.showBattleMode();
                };
                opponentTypeController.show();
                scenes.showOpponentType();
            },
            () -> {
                if (battleType == BattleTypeItem.P2P && hostFlow) {
                    networkController.leave();
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

                opponentBack = () -> {
                    timeLimitController.show();
                    scenes.showTimeLimit();
                };
                opponentTypeController.show();
                scenes.showOpponentType();
            },
            () -> {
                battleModeController.show();
                scenes.showBattleMode();
            }
        );

        opponentTypeController = new OpponentTypeController(
            opponentTypeView,
            opponent -> {
                battleController.clearAgents();
                if (opponent == OpponentType.CPU) {
                    battleController.setAgent(PlayerId.PLAYER2, new HeuristicBotAgent());
                }
                startLocalBattle(
                    battleController,
                    battleView,
                    scenes,
                    settings,
                    battleBack
                );
            },
            () -> opponentBack.run()
        );

        p2pConnectionController = new P2PConnectionController(
            p2pConnectionView,
            () -> {
                try {
                    networkController.host(NetworkService.DEFAULT_PORT);
                    hostFlow = true;
                    battleType = BattleTypeItem.P2P;
                    p2pWaitingController.showHost(
                        networkController.getHostAddressDetails(), "모드 선택 전"
                    );
                    scenes.showP2PWaiting();
                } catch (RuntimeException e) {
                    p2pConnectionController.showError(e.getMessage());
                }
            },
            ip -> {
                try {
                    networkController.join(ip, NetworkService.DEFAULT_PORT);
                    hostFlow = false;
                    battleType = BattleTypeItem.P2P;
                    joinedHost = ip.trim();
                    p2pWaitingController.showClient(joinedHost);
                    scenes.showP2PWaiting();
                } catch (RuntimeException e) {
                    p2pConnectionController.showError(e.getMessage());
                }
            },
            () -> {
                battleTypeController.show();
                scenes.showBattleType();
            },
            networkController::getRecentHosts
        );

        p2pWaitingController = new P2PWaitingController(
            p2pWaitingView,
            // '게임 시작' 버튼(클릭·Enter): 양쪽이 모두 누르면 서버가 대전을 시작한다.
            // 예외가 나면 P2PWaitingController가 오류를 표시하고 버튼을 다시 연다.
            () -> {
                if (networkController.getState() != NetworkController.State.CONNECTED
                    || networkController.getConfig() == null) {
                    throw new IllegalStateException("연결 또는 게임 설정을 기다려주세요.");
                }
                networkController.setReady(true);
            },
            () -> {
                networkController.leave();

                p2pBattleShown = false;
                p2pConnectionController.show();
                scenes.showP2PConnection();
            }
        );

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
            battleModeView.getRoot(), battleModeController::handleKey,
            () -> { battleTypeController.show(); scenes.showBattleType(); }
        );

        bindMenu(
            timeLimitView.getRoot(), timeLimitController::handleKey,
            () -> { battleModeController.show(); scenes.showBattleMode(); }
        );

        bindMenu(
            opponentTypeView.getRoot(), opponentTypeController::handleKey,
            () -> opponentBack.run()
        );

        bindMenu(
            p2pConnectionView.getRoot(), p2pConnectionController::handleKey,
            () -> { networkController.leave(); battleTypeController.show(); scenes.showBattleType(); }
        );

        bindMenu(
            p2pWaitingView.getRoot(), p2pWaitingController::handleKey,
            () -> { networkController.leave(); p2pBattleShown = false; p2pConnectionController.show(); scenes.showP2PConnection(); }
        );

        battleView.getRoot().addEventFilter(
            KeyEvent.KEY_PRESSED,
            e -> {
                if (e.getCode() == KeyCode.TAB) {
                    battleBack[0].run();
                } else if (battleType == BattleTypeItem.P2P && battleView.handleFinishKey(e.getCode())) {
                    // P2P dialogs use arrow keys and Enter; do not pass them to gameplay.
                } else if (e.getCode() != KeyCode.ESCAPE) {
                    battleController.handleKeyPressed(e.getCode());
                }
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

        networkController.setEvents(new NetworkEvents() {
            @Override public void onStateChanged(NetworkController.State state) {
                if (state == NetworkController.State.CONNECTED) {
                    if (hostFlow && networkController.getConfig() == null) {
                        battleModeController.show();
                        scenes.showBattleMode();
                    } else {
                        p2pWaitingController.showConnected();
                        p2pWaitingController.setStartEnabled(networkController.getConfig() != null);
                    }
                }
            }
            @Override public void onConfigChanged(MessagePayload.Config config) {
                if (!hostFlow) {
                    p2pWaitingController.showClient(joinedHost, config);
                }
                p2pWaitingController.showConnected();
                p2pWaitingController.setStartEnabled(true);
            }
            @Override public void onPeerReadyChanged(boolean ready) {
                if (ready) p2pWaitingController.showOpponentReady();
            }
            @Override public void onLagChanged(long ms, boolean lagging) {
                p2pWaitingController.showLag(ms, lagging);
            }
            @Override public void onBattleStarted() {
                battleBack[0] = () -> {
                    battleController.stop();
                    networkController.leave();
                    p2pBattleShown = false;
                    p2pConnectionController.show();
                    scenes.showP2PConnection();
                };
                showP2PBattle(battleController, battleView, scenes);
            }
            @Override public void onBattleFinished(MessagePayload.Result result) {
                p2pBattleShown = false;
                battleController.showP2PResult(result, networkController.isHost());
                battleView.showResultChoices(
                    "REMATCH", () -> {
                        try {
                            networkController.requestRematch();
                            battleView.showWaiting("REMATCH REQUESTED", "Waiting for opponent's answer");
                        } catch (RuntimeException ex) {
                            battleView.showChoice("REMATCH UNAVAILABLE", ex.getMessage(),
                                "P2P MENU", () -> openP2PConnection(scenes),
                                "MAIN MENU", () -> leaveToMainMenu(mainMenuController));
                        }
                    },
                    "MAIN MENU", () -> leaveToMainMenu(mainMenuController)
                );
            }
            @Override public void onRematchRequested() {
                battleView.showChoice("REMATCH REQUEST", "Your opponent wants to play again.",
                    "ACCEPT", () -> {
                        try {
                            networkController.respondRematch(true);
                            showP2PRematchWaiting(scenes);
                        } catch (RuntimeException ex) {
                            battleView.showChoice("REMATCH ERROR", ex.getMessage(),
                                "P2P MENU", () -> openP2PConnection(scenes),
                                "MAIN MENU", () -> leaveToMainMenu(mainMenuController));
                        }
                    },
                    "DECLINE", () -> {
                        networkController.respondRematch(false);
                        battleView.showChoice("REMATCH DECLINED", "You declined the rematch.",
                            "NEW GAME", () -> requestAnotherRematch(battleView),
                            "MAIN MENU", () -> leaveToMainMenu(mainMenuController));
                    }
                );
            }
            @Override public void onRematchAnswered(boolean accepted) {
                if (accepted) {
                    showP2PRematchWaiting(scenes);
                } else {
                    battleView.showChoice("REMATCH DECLINED", "Your opponent declined the rematch.",
                        "NEW GAME", () -> requestAnotherRematch(battleView),
                        "MAIN MENU", () -> leaveToMainMenu(mainMenuController));
                }
            }
            @Override public void onConnectionClosed(String reason, boolean peerLeft) {
                p2pBattleShown = false;
                battleController.stopRenderingOnly();
                battleView.showChoice("CONNECTION CLOSED",
                    peerLeft ? (hostFlow ? "CLIENT LEFT THE GAME" : "SERVER LEFT THE GAME")
                             : "CONNECTION LOST: " + (reason == null ? "Unknown error" : reason),
                    "RECONNECT", () -> openP2PConnection(scenes),
                    "MAIN MENU", () -> leaveToMainMenu(mainMenuController));
                scenes.showBattle();
            }
            @Override public void showError(String message) {
                if (networkController.getState() != NetworkController.State.DISCONNECTED)
                    p2pWaitingController.showError(message);
            }
        });

        battleBack[0] = () -> {
            if (battleType == BattleTypeItem.P2P) {
                networkController.leave();
                p2pBattleShown = false;
                p2pConnectionController.show();
                scenes.showP2PConnection();
            } else {
                battleModeController.show();
                scenes.showBattleMode();
            }
        };
        mainMenuController.show();
    }

    private void requestAnotherRematch(BattleView view) {
        try {
            networkController.requestRematch();
            view.showWaiting("REMATCH REQUESTED", "Waiting for opponent's answer");
        } catch (RuntimeException ex) {
            view.showChoice("REMATCH UNAVAILABLE", ex.getMessage(),
                "TRY AGAIN", () -> requestAnotherRematch(view),
                "CLOSE", () -> view.showWaiting("REMATCH UNAVAILABLE", "Use TAB to go back"));
        }
    }

    /** 재대전 수락 후 대기실로: 첫 대전과 같이 양쪽이 다시 '게임 시작'을 눌러야 시작 */
    private void showP2PRematchWaiting(SceneManager scenes) {
        if (hostFlow) {
            p2pWaitingController.showHost(networkController.getHostAddressDetails(), getWaitingModeText());
        } else {
            p2pWaitingController.showClient(joinedHost, networkController.getConfig());
        }
        p2pWaitingController.showRematchWaiting(networkController.getConfig() != null);
        scenes.showP2PWaiting();
    }

    private void openP2PConnection(SceneManager scenes) {
        networkController.leave();
        p2pBattleShown = false;
        p2pConnectionController.show();
        scenes.showP2PConnection();
    }

    private void leaveToMainMenu(MainMenuController controller) {
        networkController.leave();
        p2pBattleShown = false;
        controller.show();
    }

    private void startLocalBattle(
        BattleController controller,
        BattleView view,
        SceneManager scenes,
        Settings settings,
        Runnable[] back
    ) {
        prepareBattleView(view, battleMode, battleMinutes, false, settings.getDifficulty());

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
                networkController.getHostAddressDetails(),
                getWaitingModeText()
            );

            if (networkController.getState()
                == NetworkController.State.CONNECTED) {
                networkController.sendBattleConfig(config);
            }
        }

        scenes.showP2PWaiting();
    }

    private void viewResultAfterBattle(BattleView view) {
        view.setStatus("P2P BATTLE FINISHED");
        // BattleController renders the winner; TAB returns to P2P connection.
    }

    private void showP2PBattle(BattleController controller, BattleView view, SceneManager scenes) {
        if (p2pBattleShown) return;
        p2pBattleShown = true;
        BattleConfig config = battleService.getConfig();
        prepareBattleView(view, modeFrom(config),
            config != null && config.isTimeLimited() ? config.getTimeLimitSeconds() / 60 : 0, true,
            config != null ? config.getDifficulty() : Difficulty.NORMAL);
        controller.startRendering();
        scenes.showBattle();
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
        boolean p2p,
        Difficulty difficulty
    ) {
        view.setItemMode(mode == BattleModeItem.ITEM);
        view.setPlayerNames(p2p);
        view.clearResult();

        view.setStatus(
            (p2p ? "P2P / " : "LOCAL / ") + getModeText(mode)
                + " / " + difficulty.name()
        );

        view.setTimer(
            minutes > 0
                ? String.format("TIME %02d:00", minutes)
                : ""
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
            if (e.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
                e.consume();
                return;
            }
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