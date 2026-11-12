package com.team.tetris;

import java.util.function.Consumer;

import com.team.tetris.controller.BattleModeController;
import com.team.tetris.controller.BattleTypeController;
import com.team.tetris.controller.GameController;
import com.team.tetris.controller.GameOverController;
import com.team.tetris.controller.MainMenuController;
import com.team.tetris.controller.P2PConnectionController;
import com.team.tetris.controller.P2PWaitingController;
import com.team.tetris.controller.ScoreboardController;
import com.team.tetris.controller.SettingsController;
import com.team.tetris.controller.TimeLimitController;
import com.team.tetris.domain.Settings;
import com.team.tetris.service.GameEngineService;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.service.SettingsService;
import com.team.tetris.util.FileManager;
import com.team.tetris.util.SceneManager;
import com.team.tetris.view.BattleModeView;
import com.team.tetris.view.BattleModeView.BattleModeItem;
import com.team.tetris.view.BattleTypeView;
import com.team.tetris.view.BattleTypeView.BattleTypeItem;
import com.team.tetris.view.BattleView;
import com.team.tetris.view.GameOverView;
import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.P2PConnectionView;
import com.team.tetris.view.P2PWaitingView;
import com.team.tetris.view.ScoreboardView;
import com.team.tetris.view.SettingsView;
import com.team.tetris.view.TimeLimitView;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class Main extends Application {

    private BattleModeItem selectedBattleMode = BattleModeItem.NORMAL;

    private BattleModeController battleModeController;
    private BattleTypeController battleTypeController;
    private TimeLimitController timeLimitController;
    private P2PConnectionController p2pConnectionController;
    private P2PWaitingController p2pWaitingController;

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

        FileManager fileManager = new FileManager();
        ScoreboardService scoreboardService = new ScoreboardService(fileManager);
        SettingsService settingsService = new SettingsService(fileManager);
        Settings settings = settingsService.load();
        GameEngineService gameEngine = new GameEngineService();

        SceneManager sceneManager = new SceneManager(
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

        sceneManager.applyScreenSize(settings.getScreenSize());

        ScoreboardController scoreboardController = new ScoreboardController(
            scoreboardService,
            scoreboardView,
            sceneManager::showScoreboard,
            sceneManager::showMainMenu
        );

        GameOverController gameOverController = new GameOverController(
            scoreboardService,
            scoreboardController,
            gameOverView,
            sceneManager::showGameOver
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
            sceneManager,
            gameController,
            scoreboardController,
            settings
        );

        gameController.setOnQuit(mainMenuController::show);

        SettingsController settingsController = new SettingsController(
            settingsView,
            settingsService,
            scoreboardService,
            sceneManager,
            settings
        );

        battleModeController = new BattleModeController(
            battleModeView,
            mode -> {
                selectedBattleMode = mode;
                battleTypeController.show();
                sceneManager.showBattleType();
            },
            mainMenuController::show
        );

        battleTypeController = new BattleTypeController(
            battleTypeView,
            type -> {
                if (type == BattleTypeItem.P2P) {
                    p2pConnectionController.show();
                    sceneManager.showP2PConnection();

                } else if (selectedBattleMode == BattleModeItem.TIME_LIMIT) {
                    timeLimitController.show();
                    sceneManager.showTimeLimit();

                } else {
                    prepareBattleView(
                        battleView,
                        selectedBattleMode,
                        0,
                        false
                    );

                    battleView.setOnBackAction(() -> {
                        battleTypeController.show();
                        sceneManager.showBattleType();
                    });

                    sceneManager.showBattle();
                }
            },
            () -> {
                battleModeController.show();
                sceneManager.showBattleMode();
            }
        );

        timeLimitController = new TimeLimitController(
            timeLimitView,
            minutes -> {
                prepareBattleView(
                    battleView,
                    BattleModeItem.TIME_LIMIT,
                    minutes,
                    false
                );

                battleView.setOnBackAction(() -> {
                    timeLimitController.show();
                    sceneManager.showTimeLimit();
                });

                sceneManager.showBattle();
            },
            () -> {
                battleTypeController.show();
                sceneManager.showBattleType();
            }
        );

        p2pConnectionController = new P2PConnectionController(
            p2pConnectionView,
            () -> {
                p2pWaitingController.show();
                sceneManager.showP2PWaiting();
            },
            ip -> {
                p2pWaitingController.show();
                sceneManager.showP2PWaiting();
            },
            () -> {
                battleTypeController.show();
                sceneManager.showBattleType();
            }
        );

        p2pWaitingController = new P2PWaitingController(
            p2pWaitingView,
            () -> {
                prepareBattleView(
                    battleView,
                    selectedBattleMode,
                    0,
                    true
                );

                battleView.setOnBackAction(() -> {
                    p2pWaitingController.show();
                    sceneManager.showP2PWaiting();
                });

                sceneManager.showBattle();
            },
            () -> {
                p2pConnectionController.show();
                sceneManager.showP2PConnection();
            }
        );

        mainMenuView.getRoot().setOnKeyPressed(
            event -> mainMenuController.handleKey(event.getCode())
        );

        settingsView.getRoot().setOnKeyPressed(
            event -> settingsController.handleKey(event.getCode())
        );

        gameView.getRoot().setOnKeyPressed(
            event -> gameController.handleKey(event.getCode())
        );

        gameOverView.getRoot().setOnKeyPressed(
            event -> gameOverController.handleKey(event.getCode())
        );

        bindBattleKeys(
            battleModeView.getRoot(),
            battleModeController::handleKey,
            mainMenuController::show
        );

        bindBattleKeys(
            battleTypeView.getRoot(),
            battleTypeController::handleKey,
            mainMenuController::show
        );

        bindBattleKeys(
            timeLimitView.getRoot(),
            timeLimitController::handleKey,
            mainMenuController::show
        );

        bindBattleKeys(
            p2pConnectionView.getRoot(),
            p2pConnectionController::handleKey,
            mainMenuController::show
        );

        bindBattleKeys(
            p2pWaitingView.getRoot(),
            p2pWaitingController::handleKey,
            mainMenuController::show
        );

        bindBattleKeys(
            battleView.getRoot(),
            key -> {},
            mainMenuController::show
        );

        mainMenuController.show();
    }

    private void bindBattleKeys(
        Parent root,
        Consumer<KeyCode> handler,
        Runnable mainMenu
    ) {
        root.addEventFilter(
            KeyEvent.KEY_PRESSED,
            event -> {
                if (event.getCode() == KeyCode.TAB) {
                    mainMenu.run();
                    event.consume();
                    return;
                }

                handler.accept(event.getCode());
            }
        );
    }

    private void prepareBattleView(
        BattleView battleView,
        BattleModeItem mode,
        int minutes,
        boolean p2p
    ) {
        String modeText = switch (mode) {
            case NORMAL -> "NORMAL";
            case ITEM -> "ITEM";
            case TIME_LIMIT -> "TIME LIMIT";
            case BACK -> "";
        };

        battleView.setStatus(
            (p2p ? "P2P / " : "LOCAL / ") + modeText
        );

        if (minutes > 0) {
            battleView.setTimer(
                String.format("TIME %02d:00", minutes)
            );
        } else {
            battleView.setTimer("TIME --:--");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
