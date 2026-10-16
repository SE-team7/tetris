package com.team.tetris.view;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.ItemType;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

class GameViewTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX가 이미 실행 중인 경우
        }
    }

    @Test
    void normalModeUsesNormalUi() throws Exception {

        runFx(() -> {

            GameView view = new GameView();

            view.setGameInfo(
                GameMode.NORMAL,
                Difficulty.EASY
            );

            VBox itemInfoBox =
                getField(
                    view,
                    "itemInfoBox",
                    VBox.class
                );

            GridPane controlsGrid =
                getField(
                    view,
                    "controlsGrid",
                    GridPane.class
                );

            BorderPane sidePanel =
                getField(
                    view,
                    "sidePanel",
                    BorderPane.class
                );

            Label modeLabel =
                getField(
                    view,
                    "modeLabel",
                    Label.class
                );

            Label difficultyLabel =
                getField(
                    view,
                    "difficultyLabel",
                    Label.class
                );

            assertFalse(
                itemInfoBox.isVisible()
            );

            assertFalse(
                itemInfoBox.isManaged()
            );

            assertEquals(
                225.0,
                sidePanel.getPrefWidth(),
                0.001
            );

            assertEquals(
                "MODE: NORMAL",
                modeLabel.getText()
            );

            assertEquals(
                "DIFFICULTY: EASY",
                difficultyLabel.getText()
            );

            assertEquals(
                7,
                maxRow(controlsGrid)
            );

            assertEquals(
                1,
                maxColumn(controlsGrid)
            );

            return null;
        });
    }

    @Test
    void itemModeUsesItemUi() throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.setGameInfo(
                GameMode.ITEM,
                Difficulty.NORMAL
            );

            VBox itemInfoBox =
                getField(
                    view,
                    "itemInfoBox",
                    VBox.class
                );

            GridPane controlsGrid =
                getField(
                    view,
                    "controlsGrid",
                    GridPane.class
                );

            BorderPane sidePanel =
                getField(
                    view,
                    "sidePanel",
                    BorderPane.class
                );

            Label modeLabel =
                getField(
                    view,
                    "modeLabel",
                    Label.class
                );

            assertTrue(
                itemInfoBox.isVisible()
            );

            assertTrue(
                itemInfoBox.isManaged()
            );

            assertEquals(
                330.0,
                sidePanel.getPrefWidth(),
                0.001
            );

            assertEquals(
                "MODE: ITEM",
                modeLabel.getText()
            );

            assertEquals(
                3,
                maxRow(controlsGrid)
            );

            assertEquals(
                3,
                maxColumn(controlsGrid)
            );

            String text =
                collectLabelText(
                    itemInfoBox
                );

            assertTrue(
                text.contains("ITEM INFO")
            );

            assertTrue(
                text.contains("W")
            );

            assertTrue(
                text.contains(
                    "아래 블록 제거"
                )
            );

            assertTrue(
                text.contains("L")
            );

            assertTrue(
                text.contains(
                    "해당 줄 삭제"
                )
            );

            assertTrue(
                text.contains("B")
            );

            assertTrue(
                text.contains(
                    "중심 3×3 제거"
                )
            );

            assertTrue(
                text.contains("+")
            );

            assertTrue(
                text.contains(
                    "행·열 제거"
                )
            );

            assertTrue(
                text.contains("D")
            );

            assertTrue(
                text.contains(
                    "아래 두 열 압축"
                )
            );

            return null;
        });
    }

    @Test
    void switchingBackToNormalRemovesItemUi()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.setGameInfo(
                GameMode.ITEM,
                Difficulty.NORMAL
            );

            view.setGameInfo(
                GameMode.NORMAL,
                Difficulty.HARD
            );

            VBox itemInfoBox =
                getField(
                    view,
                    "itemInfoBox",
                    VBox.class
                );

            assertFalse(
                itemInfoBox.isVisible()
            );

            assertFalse(
                itemInfoBox.isManaged()
            );

            return null;
        });
    }

    @Test
    void nullGameInfoUsesDefaultNormalUi()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.setGameInfo(
                null,
                null
            );

            Label modeLabel =
                getField(
                    view,
                    "modeLabel",
                    Label.class
                );

            Label difficultyLabel =
                getField(
                    view,
                    "difficultyLabel",
                    Label.class
                );

            VBox itemInfoBox =
                getField(
                    view,
                    "itemInfoBox",
                    VBox.class
                );

            assertEquals(
                "MODE: -",
                modeLabel.getText()
            );

            assertEquals(
                "DIFFICULTY: -",
                difficultyLabel.getText()
            );

            assertFalse(
                itemInfoBox.isVisible()
            );

            return null;
        });
    }

    @Test
    void renderHandlesNullBoard() throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            assertDoesNotThrow(
                () -> view.render(
                    null,
                    null,
                    null,
                    123,
                    1,
                    false
                )
            );

            Label scoreLabel =
                getField(
                    view,
                    "scoreLabel",
                    Label.class
                );

            assertEquals(
                "123",
                scoreLabel.getText()
            );

            return null;
        });
    }

    @Test
    void renderHandlesSmallBoard() throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            BlockType[][] board = {
                {
                    BlockType.T
                }
            };

            view.render(
                board,
                null,
                null,
                10,
                1,
                false
            );

            Label[][] cells =
                getField(
                    view,
                    "boardCells",
                    Label[][].class
                );

            assertEquals(
                "O",
                cells[0][0].getText()
            );

            return null;
        });
    }

    @Test
    void renderDrawsEveryFixedBlockType()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            BlockType[][] board =
                new BlockType[20][10];

            BlockType[] types =
                BlockType.values();

            for (
                int i = 0;
                i < types.length;
                i++
            ) {

                int row = i / 10;
                int col = i % 10;

                board[row][col] =
                    types[i];
            }

            view.render(
                board,
                null,
                null,
                500,
                2,
                false
            );

            Label[][] cells =
                getField(
                    view,
                    "boardCells",
                    Label[][].class
                );

            for (
                int i = 0;
                i < types.length;
                i++
            ) {

                int row = i / 10;
                int col = i % 10;

                assertNotEquals(
                    ".",
                    cells[row][col]
                        .getText()
                );
            }

            return null;
        });
    }

    @Test
    void normalBlocksUseOInNormalMode()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            BlockType[][] board =
                new BlockType[20][10];

            board[0][0] =
                BlockType.I;

            board[0][1] =
                BlockType.J;

            board[0][2] =
                BlockType.L;

            board[0][3] =
                BlockType.O;

            board[0][4] =
                BlockType.S;

            board[0][5] =
                BlockType.T;

            board[0][6] =
                BlockType.Z;

            view.render(
                board,
                null,
                null,
                0,
                1,
                false
            );

            Label[][] cells =
                getField(
                    view,
                    "boardCells",
                    Label[][].class
                );

            for (
                int col = 0;
                col <= 6;
                col++
            ) {

                assertEquals(
                    "O",
                    cells[0][col]
                        .getText()
                );
            }

            return null;
        });
    }

    @Test
    void colorBlindModeUsesBlockLetters()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.applyColorBlindMode(
                true
            );

            BlockType[][] board =
                new BlockType[20][10];

            board[0][0] =
                BlockType.I;

            board[0][1] =
                BlockType.J;

            board[0][2] =
                BlockType.L;

            board[0][3] =
                BlockType.O;

            board[0][4] =
                BlockType.S;

            board[0][5] =
                BlockType.T;

            board[0][6] =
                BlockType.Z;

            view.render(
                board,
                null,
                null,
                0,
                1,
                false
            );

            Label[][] cells =
                getField(
                    view,
                    "boardCells",
                    Label[][].class
                );

            assertEquals(
                "I",
                cells[0][0].getText()
            );

            assertEquals(
                "J",
                cells[0][1].getText()
            );

            /*
             * 일반 L 블록은
             * 아이템 L과 구분하기 위해 G
             */
            assertEquals(
                "G",
                cells[0][2].getText()
            );

            assertEquals(
                "O",
                cells[0][3].getText()
            );

            assertEquals(
                "S",
                cells[0][4].getText()
            );

            assertEquals(
                "T",
                cells[0][5].getText()
            );

            assertEquals(
                "Z",
                cells[0][6].getText()
            );

            return null;
        });
    }

    @Test
    void specialFixedBlocksUseItemLetters()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            BlockType[][] board =
                new BlockType[20][10];

            board[0][0] =
                BlockType.WEIGHT;

            board[0][1] =
                BlockType.BOMB;

            board[0][2] =
                BlockType.CROSS;

            board[0][3] =
                BlockType.GAP_DESTROY;

            view.render(
                board,
                null,
                null,
                0,
                1,
                false
            );

            Label[][] cells =
                getField(
                    view,
                    "boardCells",
                    Label[][].class
                );

            assertEquals(
                "W",
                cells[0][0].getText()
            );

            assertEquals(
                "B",
                cells[0][1].getText()
            );

            assertEquals(
                "+",
                cells[0][2].getText()
            );

            assertEquals(
                "D",
                cells[0][3].getText()
            );

            return null;
        });
    }

    @Test
    void everyBlockColorWorksInNormalMode()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.applyColorBlindMode(
                false
            );

            for (
                BlockType type :
                    BlockType.values()
            ) {

                String color =
                    (String)
                    invokePrivate(
                        view,
                        "getBlockColor",
                        new Class<?>[] {
                            BlockType.class
                        },
                        type
                    );

                assertNotNull(color);

                assertTrue(
                    color.startsWith("#")
                );
            }

            return null;
        });
    }

    @Test
    void everyBlockColorWorksInColorBlindMode()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.applyColorBlindMode(
                true
            );

            for (
                BlockType type :
                    BlockType.values()
            ) {

                String color =
                    (String)
                    invokePrivate(
                        view,
                        "getBlockColor",
                        new Class<?>[] {
                            BlockType.class
                        },
                        type
                    );

                assertNotNull(color);

                assertTrue(
                    color.startsWith("#")
                );
            }

            return null;
        });
    }

    @Test
    void everyItemHasText()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            assertEquals(
                "W",
                invokePrivate(
                    view,
                    "getItemText",
                    new Class<?>[] {
                        ItemType.class
                    },
                    ItemType.WEIGHT
                )
            );

            assertEquals(
                "L",
                invokePrivate(
                    view,
                    "getItemText",
                    new Class<?>[] {
                        ItemType.class
                    },
                    ItemType.LINE_CLEAR
                )
            );

            assertEquals(
                "B",
                invokePrivate(
                    view,
                    "getItemText",
                    new Class<?>[] {
                        ItemType.class
                    },
                    ItemType.BOMB
                )
            );

            assertEquals(
                "+",
                invokePrivate(
                    view,
                    "getItemText",
                    new Class<?>[] {
                        ItemType.class
                    },
                    ItemType.CROSS
                )
            );

            assertEquals(
                "D",
                invokePrivate(
                    view,
                    "getItemText",
                    new Class<?>[] {
                        ItemType.class
                    },
                    ItemType.GAP_DESTROY
                )
            );

            assertEquals(
                "O",
                invokePrivate(
                    view,
                    "getItemText",
                    new Class<?>[] {
                        ItemType.class
                    },
                    ItemType.NONE
                )
            );

            return null;
        });
    }

    @Test
    void everyItemColorWorksInBothModes()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            for (
                boolean colorBlind :
                    new boolean[] {
                        false,
                        true
                    }
            ) {

                view.applyColorBlindMode(
                    colorBlind
                );

                for (
                    ItemType item :
                        ItemType.values()
                ) {

                    String color =
                        (String)
                        invokePrivate(
                            view,
                            "getItemColor",
                            new Class<?>[] {
                                ItemType.class
                            },
                            item
                        );

                    assertNotNull(color);

                    assertTrue(
                        color.startsWith("#")
                    );
                }
            }

            return null;
        });
    }

    @Test
    void drawItemCellHandlesAllItemTypes()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            for (
                boolean colorBlind :
                    new boolean[] {
                        false,
                        true
                    }
            ) {

                view.applyColorBlindMode(
                    colorBlind
                );

                for (
                    ItemType item :
                        ItemType.values()
                ) {

                    Label label =
                        new Label("TEST");

                    invokePrivate(
                        view,
                        "drawItemCell",
                        new Class<?>[] {
                            Label.class,
                            ItemType.class
                        },
                        label,
                        item
                    );

                    if (
                        item == ItemType.NONE
                    ) {

                        assertEquals(
                            "TEST",
                            label.getText()
                        );

                    } else {

                        assertNotEquals(
                            "TEST",
                            label.getText()
                        );

                        assertTrue(
                            label
                                .getStyle()
                                .contains(
                                    "-fx-text-fill"
                                )
                        );
                    }
                }
            }

            return null;
        });
    }

    @Test
    void drawBlockCellHandlesNullType()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            Label label =
                new Label("UNCHANGED");

            invokePrivate(
                view,
                "drawBlockCell",
                new Class<?>[] {
                    Label.class,
                    BlockType.class
                },
                label,
                null
            );

            assertEquals(
                "UNCHANGED",
                label.getText()
            );

            return null;
        });
    }

    @Test
    void boardFrameChangesByGameMode()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            String normalBoardStyle =
                (String)
                invokePrivate(
                    view,
                    "getFrameStyle",
                    new Class<?>[] {
                        boolean.class
                    },
                    false
                );

            String normalNextStyle =
                (String)
                invokePrivate(
                    view,
                    "getFrameStyle",
                    new Class<?>[] {
                        boolean.class
                    },
                    true
                );

            assertTrue(
                normalBoardStyle
                    .contains("white")
            );

            assertTrue(
                normalNextStyle
                    .contains("#777777")
            );

            view.setGameInfo(
                GameMode.ITEM,
                Difficulty.NORMAL
            );

            String itemBoardStyle =
                (String)
                invokePrivate(
                    view,
                    "getFrameStyle",
                    new Class<?>[] {
                        boolean.class
                    },
                    false
                );

            String itemNextStyle =
                (String)
                invokePrivate(
                    view,
                    "getFrameStyle",
                    new Class<?>[] {
                        boolean.class
                    },
                    true
                );

            assertTrue(
                itemBoardStyle
                    .contains("#D6C2FF")
            );

            assertTrue(
                itemNextStyle
                    .contains("#D6C2FF")
            );

            return null;
        });
    }

    @Test
    void scoreAndPauseCanBeUpdated()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.updateScore(
                777
            );

            Label scoreLabel =
                getField(
                    view,
                    "scoreLabel",
                    Label.class
                );

            assertEquals(
                "777",
                scoreLabel.getText()
            );

            view.setPaused(true);

            Label statusLabel =
                getField(
                    view,
                    "statusLabel",
                    Label.class
                );

            assertEquals(
                "PAUSED",
                statusLabel.getText()
            );

            view.setPaused(false);

            assertEquals(
                "",
                statusLabel.getText()
            );

            return null;
        });
    }

    @Test
    void skipInfoCanBeUpdated()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.updateSkipInfo(
                "K",
                4
            );

            Label skipKeyLabel =
                getField(
                    view,
                    "skipKeyLabel",
                    Label.class
                );

            Label skipRemainingLabel =
                getField(
                    view,
                    "skipRemainingLabel",
                    Label.class
                );

            assertEquals(
                "K",
                skipKeyLabel.getText()
            );

            assertEquals(
                "4 / 7",
                skipRemainingLabel
                    .getText()
            );

            return null;
        });
    }

    @Test
    void controlGuideCanBeUpdated()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.setControlKeyGuide(
                "LEFT",
                "RIGHT",
                "DOWN",
                "P",
                "SPACE",
                "K"
            );

            Label move =
                getField(
                    view,
                    "moveKeyLabel",
                    Label.class
                );

            Label soft =
                getField(
                    view,
                    "softDropKeyLabel",
                    Label.class
                );

            Label rotate =
                getField(
                    view,
                    "rotateKeyLabel",
                    Label.class
                );

            Label hard =
                getField(
                    view,
                    "hardDropKeyLabel",
                    Label.class
                );

            Label skip =
                getField(
                    view,
                    "skipKeyLabel",
                    Label.class
                );

            assertEquals(
                "LEFT RIGHT",
                move.getText()
            );

            assertEquals(
                "DOWN",
                soft.getText()
            );

            assertEquals(
                "P",
                rotate.getText()
            );

            assertEquals(
                "SPACE",
                hard.getText()
            );

            assertEquals(
                "K",
                skip.getText()
            );

            return null;
        });
    }

    @Test
    void emptyControlKeysUseDash()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.setControlKeyGuide(
                null,
                "",
                " ",
                null,
                "",
                null
            );

            Label move =
                getField(
                    view,
                    "moveKeyLabel",
                    Label.class
                );

            Label soft =
                getField(
                    view,
                    "softDropKeyLabel",
                    Label.class
                );

            Label rotate =
                getField(
                    view,
                    "rotateKeyLabel",
                    Label.class
                );

            Label hard =
                getField(
                    view,
                    "hardDropKeyLabel",
                    Label.class
                );

            Label skip =
                getField(
                    view,
                    "skipKeyLabel",
                    Label.class
                );

            assertEquals(
                "- -",
                move.getText()
            );

            assertEquals(
                "-",
                soft.getText()
            );

            assertEquals(
                "-",
                rotate.getText()
            );

            assertEquals(
                "-",
                hard.getText()
            );

            assertEquals(
                "-",
                skip.getText()
            );

            return null;
        });
    }

    @Test
    void colorBlindFlagCanChange()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            view.applyColorBlindMode(
                true
            );

            assertTrue(
                getBooleanField(
                    view,
                    "colorBlindMode"
                )
            );

            view.applyColorBlindMode(
                false
            );

            assertFalse(
                getBooleanField(
                    view,
                    "colorBlindMode"
                )
            );

            return null;
        });
    }

    @Test
    void nullClearAnimationRunsCallback()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            AtomicBoolean called =
                new AtomicBoolean(
                    false
                );

            view.playClearAnimation(
                null,
                () ->
                    called.set(true)
            );

            assertTrue(
                called.get()
            );

            return null;
        });
    }

    @Test
    void nullClearAnimationAllowsNullCallback()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            assertDoesNotThrow(
                () ->
                    view.playClearAnimation(
                        null,
                        null
                    )
            );

            return null;
        });
    }

    @Test
    void animationTargetCollectionHandlesNull()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            Object result =
                invokePrivate(
                    view,
                    "collectAnimationTargets",
                    new Class<?>[] {
                        com.team.tetris.domain.ClearResult.class
                    },
                    new Object[] {
                        null
                    }
                );

            assertTrue(
                result instanceof Set
            );

            assertTrue(
                ((Set<?>) result)
                    .isEmpty()
            );

            return null;
        });
    }

    @Test
    void animationUtilityMethodsWork()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            Label label =
                new Label("X");

            label.setStyle(
                "-fx-text-fill: red;"
            );

            Set<Label> targets =
                new LinkedHashSet<>();

            targets.add(label);

            @SuppressWarnings("unchecked")
            Map<Label, String>
                originalStyles =
                    (Map<Label, String>)
                    invokePrivate(
                        view,
                        "rememberStyles",
                        new Class<?>[] {
                            Set.class
                        },
                        targets
                    );

            assertEquals(
                "-fx-text-fill: red;",
                originalStyles.get(label)
            );

            invokePrivate(
                view,
                "applyEffectStyle",
                new Class<?>[] {
                    Set.class,
                    String.class,
                    String.class
                },
                targets,
                "#FFFFFF",
                "#000000"
            );

            assertTrue(
                label.getStyle()
                    .contains(
                        "#FFFFFF"
                    )
            );

            invokePrivate(
                view,
                "setTargetScale",
                new Class<?>[] {
                    Set.class,
                    double.class,
                    double.class
                },
                targets,
                1.5,
                0.5
            );

            assertEquals(
                1.5,
                label.getScaleX(),
                0.001
            );

            assertEquals(
                0.5,
                label.getScaleY(),
                0.001
            );

            invokePrivate(
                view,
                "setTargetOpacity",
                new Class<?>[] {
                    Set.class,
                    double.class
                },
                targets,
                0.4
            );

            assertEquals(
                0.4,
                label.getOpacity(),
                0.001
            );

            invokePrivate(
                view,
                "restoreAnimationState",
                new Class<?>[] {
                    Set.class,
                    Map.class
                },
                targets,
                originalStyles
            );

            assertEquals(
                1.0,
                label.getScaleX(),
                0.001
            );

            assertEquals(
                1.0,
                label.getScaleY(),
                0.001
            );

            assertEquals(
                1.0,
                label.getOpacity(),
                0.001
            );

            assertEquals(
                "-fx-text-fill: red;",
                label.getStyle()
            );

            return null;
        });
    }

    @Test
    void animationUtilitiesHandleNullTargets()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            assertDoesNotThrow(
                () -> {
                    invokePrivate(
                        view,
                        "applyEffectStyle",
                        new Class<?>[] {
                            Set.class,
                            String.class,
                            String.class
                        },
                        null,
                        "#FFFFFF",
                        "#000000"
                    );

                    invokePrivate(
                        view,
                        "setTargetScale",
                        new Class<?>[] {
                            Set.class,
                            double.class,
                            double.class
                        },
                        null,
                        1.0,
                        1.0
                    );

                    invokePrivate(
                        view,
                        "setTargetOpacity",
                        new Class<?>[] {
                            Set.class,
                            double.class
                        },
                        null,
                        1.0
                    );

                    invokePrivate(
                        view,
                        "restoreAnimationState",
                        new Class<?>[] {
                            Set.class,
                            Map.class
                        },
                        null,
                        null
                    );
                }
            );

            return null;
        });
    }

    @Test
    void animationUtilitiesSkipNullCells()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            Label label =
                new Label("A");

            Set<Label> targets =
                new LinkedHashSet<>();

            targets.add(null);
            targets.add(label);

            invokePrivate(
                view,
                "applyEffectStyle",
                new Class<?>[] {
                    Set.class,
                    String.class,
                    String.class
                },
                targets,
                "#AAAAAA",
                "#111111"
            );

            invokePrivate(
                view,
                "setTargetScale",
                new Class<?>[] {
                    Set.class,
                    double.class,
                    double.class
                },
                targets,
                1.2,
                1.2
            );

            invokePrivate(
                view,
                "setTargetOpacity",
                new Class<?>[] {
                    Set.class,
                    double.class
                },
                targets,
                0.7
            );

            Map<Label, String> styles =
                new IdentityHashMap<>();

            styles.put(
                label,
                "-fx-text-fill: blue;"
            );

            styles.put(
                null,
                "ignored"
            );

            invokePrivate(
                view,
                "restoreAnimationState",
                new Class<?>[] {
                    Set.class,
                    Map.class
                },
                targets,
                styles
            );

            assertEquals(
                1.0,
                label.getScaleX(),
                0.001
            );

            assertEquals(
                1.0,
                label.getOpacity(),
                0.001
            );

            assertEquals(
                "-fx-text-fill: blue;",
                label.getStyle()
            );

            return null;
        });
    }

    @Test
    void effectStatusCanBeShownAndRestored()
        throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            Label status =
                getField(
                    view,
                    "statusLabel",
                    Label.class
                );

            String originalStyle =
                status.getStyle();

            invokePrivate(
                view,
                "showEffectStatus",
                new Class<?>[] {
                    String.class,
                    String.class
                },
                "BOMB!",
                "#FF0000"
            );

            assertEquals(
                "BOMB!",
                status.getText()
            );

            assertTrue(
                status.getStyle()
                    .contains(
                        "#FF0000"
                    )
            );

            invokePrivate(
                view,
                "restoreEffectStatus",
                new Class<?>[] {
                    String.class,
                    String.class
                },
                null,
                null
            );

            assertEquals(
                "",
                status.getText()
            );

            invokePrivate(
                view,
                "restoreEffectStatus",
                new Class<?>[] {
                    String.class,
                    String.class
                },
                "PAUSED",
                originalStyle
            );

            assertEquals(
                "PAUSED",
                status.getText()
            );

            assertEquals(
                originalStyle,
                status.getStyle()
            );

            return null;
        });
    }

    @Test
    void rootExists() throws Exception {

        runFx(() -> {

            GameView view =
                new GameView();

            assertNotNull(
                view.getRoot()
            );

            return null;
        });
    }

    private static int maxRow(
        GridPane grid
    ) {

        return grid
            .getChildren()
            .stream()
            .mapToInt(node -> {

                Integer row =
                    GridPane
                        .getRowIndex(
                            node
                        );

                return row == null
                    ? 0
                    : row;
            })
            .max()
            .orElse(-1);
    }

    private static int maxColumn(
        GridPane grid
    ) {

        return grid
            .getChildren()
            .stream()
            .mapToInt(node -> {

                Integer col =
                    GridPane
                        .getColumnIndex(
                            node
                        );

                return col == null
                    ? 0
                    : col;
            })
            .max()
            .orElse(-1);
    }

    private static String collectLabelText(
        javafx.scene.Parent parent
    ) {

        StringBuilder result =
            new StringBuilder();

        for (
            javafx.scene.Node node :
                parent
                    .getChildrenUnmodifiable()
        ) {

            if (
                node instanceof Label label
            ) {

                result
                    .append(
                        label.getText()
                    )
                    .append('\n');
            }

            if (
                node
                    instanceof
                    javafx.scene.Parent child
            ) {

                result.append(
                    collectLabelText(
                        child
                    )
                );
            }
        }

        return result.toString();
    }

    private static Object invokePrivate(
        Object target,
        String methodName,
        Class<?>[] parameterTypes,
        Object... args
    ) throws Exception {

        Method method =
            target
                .getClass()
                .getDeclaredMethod(
                    methodName,
                    parameterTypes
                );

        method.setAccessible(
            true
        );

        return method.invoke(
            target,
            args
        );
    }

    private static <T> T getField(
        Object target,
        String fieldName,
        Class<T> type
    ) throws Exception {

        Field field =
            target
                .getClass()
                .getDeclaredField(
                    fieldName
                );

        field.setAccessible(
            true
        );

        return type.cast(
            field.get(
                target
            )
        );
    }

    private static boolean getBooleanField(
        Object target,
        String fieldName
    ) throws Exception {

        Field field =
            target
                .getClass()
                .getDeclaredField(
                    fieldName
                );

        field.setAccessible(
            true
        );

        return field.getBoolean(
            target
        );
    }

    private static <T> T runFx(
        Callable<T> action
    ) throws Exception {

        if (
            Platform
                .isFxApplicationThread()
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
}