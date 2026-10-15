package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Settings;
import com.team.tetris.util.SceneManager;
import com.team.tetris.view.GameOverView;
import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.MainMenuView.MainMenuItem;
import com.team.tetris.view.ScoreboardView;
import com.team.tetris.view.SettingsView;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

class MainMenuControllerTest {

    @BeforeAll
    static void startJavaFx() throws Exception {

        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX가 이미 실행 중인 경우
        }

        Platform.setImplicitExit(false);
    }

    @Test
    void initialSelectionIsStartGame()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            assertEquals(
                MainMenuItem.START_GAME,
                context.controller.getSelectedMenu()
            );

            context.stage.hide();

            return null;
        });
    }

    @Test
    void downMovesToNextMenu()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            context.controller.handleKey(
                KeyCode.DOWN
            );

            assertEquals(
                MainMenuItem.START_ITEM_GAME,
                context.controller.getSelectedMenu()
            );

            context.stage.hide();

            return null;
        });
    }

    @Test
    void upFromFirstMenuWrapsToExit()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            context.controller.handleKey(
                KeyCode.UP
            );

            assertEquals(
                MainMenuItem.EXIT,
                context.controller.getSelectedMenu()
            );

            context.stage.hide();

            return null;
        });
    }

    @Test
    void downFromExitWrapsToStartGame()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            context.controller.handleKey(
                KeyCode.UP
            );

            assertEquals(
                MainMenuItem.EXIT,
                context.controller.getSelectedMenu()
            );

            context.controller.handleKey(
                KeyCode.DOWN
            );

            assertEquals(
                MainMenuItem.START_GAME,
                context.controller.getSelectedMenu()
            );

            context.stage.hide();

            return null;
        });
    }

    @Test
    void wAndSDoNotMoveSelection()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            assertEquals(
                MainMenuItem.START_GAME,
                context.controller.getSelectedMenu()
            );

            context.controller.handleKey(
                KeyCode.W
            );

            assertEquals(
                MainMenuItem.START_GAME,
                context.controller.getSelectedMenu()
            );

            context.controller.handleKey(
                KeyCode.S
            );

            assertEquals(
                MainMenuItem.START_GAME,
                context.controller.getSelectedMenu()
            );

            context.stage.hide();

            return null;
        });
    }

    @Test
    void showResetsSelectionAndDisplaysMainMenu()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            context.controller.handleKey(
                KeyCode.DOWN
            );

            assertEquals(
                MainMenuItem.START_ITEM_GAME,
                context.controller.getSelectedMenu()
            );

            context.controller.show();

            assertEquals(
                MainMenuItem.START_GAME,
                context.controller.getSelectedMenu()
            );

            assertSame(
                context.mainMenuView.getRoot(),
                context.stage
                    .getScene()
                    .getRoot()
            );

            context.stage.hide();

            return null;
        });
    }

    @Test
    void enterOnSettingsDisplaysSettingsScene()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            /*
             * START_GAME
             * -> START_ITEM_GAME
             * -> SETTINGS
             */
            context.controller.handleKey(
                KeyCode.DOWN
            );

            context.controller.handleKey(
                KeyCode.DOWN
            );

            assertEquals(
                MainMenuItem.SETTINGS,
                context.controller.getSelectedMenu()
            );

            context.controller.handleKey(
                KeyCode.ENTER
            );

            assertSame(
                context.settingsView.getRoot(),
                context.stage
                    .getScene()
                    .getRoot()
            );

            context.stage.hide();

            return null;
        });
    }

    @Test
    void unrelatedKeyDoesNothing()
        throws Exception {

        runFx(() -> {

            TestContext context =
                createContext();

            context.controller.handleKey(
                KeyCode.SPACE
            );

            assertEquals(
                MainMenuItem.START_GAME,
                context.controller.getSelectedMenu()
            );

            context.stage.hide();

            return null;
        });
    }

    private static TestContext
        createContext() {

        Stage stage =
            new Stage();

        MainMenuView mainMenuView =
            new MainMenuView();

        SettingsView settingsView =
            new SettingsView();

        ScoreboardView scoreboardView =
            new ScoreboardView();

        GameView gameView =
            new GameView();

        GameOverView gameOverView =
            new GameOverView();

        SceneManager sceneManager =
            new SceneManager(
                stage,
                mainMenuView,
                settingsView,
                scoreboardView,
                gameView,
                gameOverView
            );

        Settings settings =
            new Settings();

        /*
         * 이 테스트에서는
         * GameController / ScoreboardController가
         * 필요한 메뉴를 실행하지 않으므로 null 사용
         */
        MainMenuController controller =
            new MainMenuController(
                mainMenuView,
                sceneManager,
                null,
                null,
                settings
            );

        return new TestContext(
            stage,
            mainMenuView,
            settingsView,
            controller
        );
    }

    private static <T> T runFx(
        Callable<T> action
    ) throws Exception {

        if (
            Platform.isFxApplicationThread()
        ) {
            return action.call();
        }

        FutureTask<T> task =
            new FutureTask<>(
                action
            );

        Platform.runLater(
            task
        );

        return task.get();
    }

    private static class TestContext {

        private final Stage stage;
        private final MainMenuView mainMenuView;
        private final SettingsView settingsView;
        private final MainMenuController controller;

        private TestContext(
            Stage stage,
            MainMenuView mainMenuView,
            SettingsView settingsView,
            MainMenuController controller
        ) {

            this.stage =
                stage;

            this.mainMenuView =
                mainMenuView;

            this.settingsView =
                settingsView;

            this.controller =
                controller;
        }
    }
}