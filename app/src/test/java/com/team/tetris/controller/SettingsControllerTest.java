package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.util.SceneManager;
import com.team.tetris.view.GameOverView;
import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.ScoreboardView;
import com.team.tetris.view.SettingsView;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

class SettingsControllerTest {

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
    void constructorRendersCurrentSettings() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            assertEquals(
                context.settings.getScreenSize().name(),
                context.settingsView.getScreenSizeComboBox().getValue()
            );

            assertEquals(
                context.settings.getDifficulty(),
                context.settingsView.getDifficultyComboBox().getValue()
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void changeDifficultyUpdatesSettings() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            context.controller.changeDifficulty(Difficulty.HARD);

            assertEquals(
                Difficulty.HARD,
                context.settings.getDifficulty()
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void nullDifficultyDoesNothing() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            Difficulty before =
                context.settings.getDifficulty();

            context.controller.changeDifficulty(null);

            assertEquals(
                before,
                context.settings.getDifficulty()
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void changeScreenSizeUpdatesSettingsAndStage() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            context.controller.changeScreenSize(
                ScreenSize.LARGE
            );

            assertEquals(
                ScreenSize.LARGE,
                context.settings.getScreenSize()
            );

            assertEquals(
                ScreenSize.LARGE.getWidth(),
                context.stage.getWidth(),
                0.001
            );

            assertEquals(
                ScreenSize.LARGE.getHeight(),
                context.stage.getHeight(),
                0.001
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void changeKeyUpdatesSettings() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            boolean changed =
                context.controller.changeKey(
                    KeyAction.MOVE_LEFT,
                    KeyCode.Q
                );

            assertTrue(changed);

            assertEquals(
                KeyCode.Q,
                context.settings.getKey(
                    KeyAction.MOVE_LEFT
                )
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void duplicateKeyIsRejected() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            KeyCode alreadyUsed =
                context.settings.getKey(
                    KeyAction.MOVE_RIGHT
                );

            KeyCode original =
                context.settings.getKey(
                    KeyAction.MOVE_LEFT
                );

            boolean changed =
                context.controller.changeKey(
                    KeyAction.MOVE_LEFT,
                    alreadyUsed
                );

            assertFalse(changed);

            assertEquals(
                original,
                context.settings.getKey(
                    KeyAction.MOVE_LEFT
                )
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void colorBlindButtonTogglesMode() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            assertFalse(
                context.settings.isColorBlindMode()
            );

            context.settingsView
                .getColorBlindButton()
                .fire();

            assertTrue(
                context.settings.isColorBlindMode()
            );

            context.settingsView
                .getColorBlindButton()
                .fire();

            assertFalse(
                context.settings.isColorBlindMode()
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void keyCaptureChangesKeyAfterEnter() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            context.settingsView
                .getKeyButton(
                    KeyAction.MOVE_LEFT.name()
                )
                .fire();

            context.controller.handleKey(KeyCode.Q);
            context.controller.handleKey(KeyCode.ENTER);

            assertEquals(
                KeyCode.Q,
                context.settings.getKey(
                    KeyAction.MOVE_LEFT
                )
            );

            context.stage.hide();
            return null;
        });
    }

    @Test
    void showDisplaysSettingsScene() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            context.controller.show();

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
    void backToMainMenuDisplaysMainMenuScene() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            context.controller.show();
            context.controller.backToMainMenu();

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
    void screenSizeComboBoxChangesScreenSize() throws Exception {
        runFx(() -> {
            TestContext context = createContext();

            context.settingsView
                .getScreenSizeComboBox()
                .setValue(
                    ScreenSize.SMALL.name()
                );

            context.settingsView
                .getScreenSizeComboBox()
                .getOnAction()
                .handle(null);

            assertEquals(
                ScreenSize.SMALL,
                context.settings.getScreenSize()
            );

            assertEquals(
                ScreenSize.SMALL.getWidth(),
                context.stage.getWidth(),
                0.001
            );

            assertEquals(
                ScreenSize.SMALL.getHeight(),
                context.stage.getHeight(),
                0.001
            );

            context.stage.hide();
            return null;
        });
    }

    private static TestContext createContext() {
        Stage stage = new Stage();

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
         * 이 테스트에서는 save/resetScoreboard를
         * 직접 테스트하지 않으므로 service는 null 사용
         */
        SettingsController controller =
            new SettingsController(
                settingsView,
                null,
                null,
                sceneManager,
                settings
            );

        return new TestContext(
            stage,
            mainMenuView,
            settingsView,
            settings,
            controller
        );
    }

    private static <T> T runFx(
        Callable<T> action
    ) throws Exception {

        if (Platform.isFxApplicationThread()) {
            return action.call();
        }

        FutureTask<T> task =
            new FutureTask<>(action);

        Platform.runLater(task);

        return task.get();
    }

    private static class TestContext {

        private final Stage stage;
        private final MainMenuView mainMenuView;
        private final SettingsView settingsView;
        private final Settings settings;
        private final SettingsController controller;

        private TestContext(
            Stage stage,
            MainMenuView mainMenuView,
            SettingsView settingsView,
            Settings settings,
            SettingsController controller
        ) {
            this.stage = stage;
            this.mainMenuView = mainMenuView;
            this.settingsView = settingsView;
            this.settings = settings;
            this.controller = controller;
        }
    }
}
