package com.team.tetris.view;

import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Settings;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

public class BattleView {
    private final BorderPane root = new BorderPane();
    private final Label timer = new Label(""), status = new Label("");
    private final Label lag = new Label();
    private final Label result = new Label(), rematch = new Label();
    private final PlayerArea p1 = new PlayerArea("PLAYER 1", true);
    private final PlayerArea p2 = new PlayerArea("PLAYER 2", false);
    private final VBox itemInfo = p1.game.createBattleItemInfoBox();
    private Runnable onBackAction;
    private final StackPane screens = new StackPane();
    private final VBox finishScreen = new VBox(20);
    private final Label finishTitle = new Label(), finishReason = new Label();
    private final Label finishScores = new Label(), finishGuide = new Label();
    private final HBox finishActions = new HBox(30);
    private final StackPane postMatchChat = new StackPane();
    private Runnable firstAction, secondAction;
    private int selectedAction;
    private boolean interactiveFinish;

    public BattleView() {
        root.getStyleClass().add("battle-root");
        Label title = new Label("BATTLE MODE");
        title.getStyleClass().add("battle-title");
        timer.getStyleClass().add("battle-timer");
        status.getStyleClass().add("battle-status");
        lag.getStyleClass().add("battle-lag");
        setLag(0, false);
        result.getStyleClass().add("battle-result");
        rematch.getStyleClass().add("battle-rematch");
        HBox statusRow = new HBox(12, status, lag);
        statusRow.setAlignment(Pos.CENTER);
        VBox top = new VBox(5, title, statusRow, timer, result, rematch);
        top.setAlignment(Pos.CENTER);
        HBox players = new HBox(100, p1.root, p2.root);
        players.setAlignment(Pos.CENTER);
        itemInfo.setVisible(false);
        itemInfo.setManaged(false);
        // Keep the existing player spacing; use the empty area below both incoming previews.
        GridPane itemGrid = (GridPane) itemInfo.getChildren().get(1);
        // GameView adds each image and description consecutively.
        for (int i = 0; i < itemGrid.getChildren().size(); i++) {
            Node node = itemGrid.getChildren().get(i);
            int index = i / 2;
            GridPane.setRowIndex(node, index);
            GridPane.setColumnIndex(node, i % 2);
            if (node instanceof Label label && i % 2 == 1) {
                label.setStyle(label.getStyle() + "; -fx-font-size: 15px;");
                label.setMinWidth(Region.USE_PREF_SIZE);
                label.setMaxWidth(Region.USE_PREF_SIZE);
            }
        }
        itemGrid.setHgap(10);
        itemGrid.setVgap(6);
        itemGrid.setAlignment(Pos.CENTER);
        for (Node node : itemGrid.getChildren()) {
            if (node instanceof javafx.scene.image.ImageView image) {
                image.setFitWidth(32);
                image.setFitHeight(32);
                image.setPreserveRatio(true);
            }
        }
        itemInfo.setMouseTransparent(true);
        Scale itemScale = new Scale(1, 1, 0, 0);
        itemInfo.getTransforms().add(itemScale);
        StackPane playerLayout = new StackPane(players, itemInfo) {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                // Resolve nested board and incoming positions before measuring them.
                players.layout();
                if (!itemInfo.isVisible()) return;
                Bounds leftBoard = boundsIn(p1.board, this);
                Bounds rightBoard = boundsIn(p2.board, this);
                Bounds leftInfo = boundsIn(p1.attackFrame, this);
                Bounds rightInfo = boundsIn(p2.attackFrame, this);
                double x = leftBoard.getMaxX() + 8;
                double y = Math.max(leftInfo.getMaxY(), rightInfo.getMaxY()) + 8;
                double w = rightBoard.getMinX() - 8 - x;
                double h = Math.min(leftBoard.getMaxY(), rightBoard.getMaxY()) - 8 - y;
                itemInfo.applyCss();
                double iw = itemInfo.prefWidth(-1), ih = itemInfo.prefHeight(iw);
                if (w <= 0 || h <= 0 || iw <= 0 || ih <= 0) return;
                double factor = Math.min(1.25, Math.min(w / iw, h / ih));
                itemInfo.resize(iw, ih);
                itemGrid.resize(itemGrid.prefWidth(-1), itemGrid.prefHeight(-1));
                itemInfo.layout();
                itemScale.setX(factor);
                itemScale.setY(factor);
                itemInfo.relocate(x + (w - iw * factor) / 2, y + (h - ih * factor) * 0.80);
            }
        };
        VBox content = new VBox(8, playerLayout);
        content.setAlignment(Pos.CENTER);
        screens.getChildren().add(fitted(content));
        // Keep title, mode and timer readable in SMALL without scaling them with boards.
        root.setTop(top);
        finishTitle.getStyleClass().add("battle-finish-title");
        finishReason.getStyleClass().add("battle-finish-reason");
        finishScores.getStyleClass().add("battle-finish-scores");
        finishGuide.getStyleClass().add("battle-finish-guide");
        VBox summary = new VBox(16, finishReason, finishScores);
        summary.setAlignment(Pos.CENTER);
        summary.getStyleClass().add("battle-finish-summary");
        finishActions.setAlignment(Pos.CENTER);
        finishScreen.getChildren().addAll(finishTitle, summary, finishGuide, finishActions);
        finishScreen.setAlignment(Pos.CENTER);
        finishScreen.getStyleClass().add("battle-finish-screen");
        finishScreen.setVisible(false);
        finishScreen.setManaged(false);
        screens.getChildren().add(finishScreen);
        root.setCenter(screens);
        postMatchChat.getStyleClass().add("battle-post-match-chat");
        root.setFocusTraversable(true);
    }

    private static Bounds boundsIn(Node node, Parent ancestor) {
        Bounds bounds = node.getBoundsInLocal();
        while (node != ancestor) {
            bounds = node.localToParent(bounds);
            node = node.getParent();
        }
        return bounds;
    }

    private Pane fitted(VBox content) {
        Scale scale = new Scale(1, 1, 0, 0);
        content.getTransforms().add(scale);
        content.setManaged(false);
        Pane pane = new Pane(content) {
            @Override protected void layoutChildren() {
                double w = getWidth(), h = getHeight();
                if (w <= 0 || h <= 0) return;
                double cw = content.prefWidth(-1), ch = content.prefHeight(cw);
                if (cw <= 0 || ch <= 0) return;
                double factor = Math.min(w / cw, h / ch);
                content.resize(cw, ch);
                content.layout();
                scale.setX(factor);
                scale.setY(factor);
                content.relocate((w - cw * factor) / 2, (h - ch * factor) / 2);
            }
        };
        pane.setMinSize(0, 0);
        return pane;
    }

    public Parent getRoot() { return root; }
    public void renderPlayer(PlayerId id, GameSnapshot snapshot) {
        (id == PlayerId.PLAYER1 ? p1 : p2).render(snapshot);
    }
    public boolean isClearAnimating(PlayerId id) {
        return (id == PlayerId.PLAYER1 ? p1 : p2).animating;
    }
    public void playClearAnimation(PlayerId id, ClearResult event) {
        PlayerArea area = id == PlayerId.PLAYER1 ? p1 : p2;
        if (event == null || area.animating) return;
        area.animating = true;
        area.game.playClearAnimation(event, () -> area.animating = false);
    }
    public void setTimer(String text) {
        timer.setText(text == null ? "" : text);
        timer.setVisible(!timer.getText().isBlank());
        timer.setManaged(timer.isVisible());
        timer.getStyleClass().removeAll("battle-timer-warning", "battle-timer-urgent");
        if (timer.isVisible()) {
            String value = timer.getText().replace("TIME ", "");
            String[] parts = value.split(":");
            try {
                int seconds = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                if (seconds <= 30) timer.getStyleClass().add("battle-timer-urgent");
                else if (seconds <= 60) timer.getStyleClass().add("battle-timer-warning");
            } catch (NumberFormatException ignored) { }
        }
    }
    public void setStatus(String text) { status.setText(text == null ? "" : text); }
    public void setLag(long ms, boolean lagging) {
        lag.setText(lagging ? "NETWORK LAG : " + ms + "ms" : "");
        lag.setVisible(lagging);
        lag.setManaged(lagging);
    }
    public void showResult(String text, String rematchText) {
        result.setText(text == null ? "" : text);
        rematch.setText(rematchText == null ? "" : rematchText);
    }
    public void clearResult() {
        showResult("", "");
        setPostMatchChat(null);
        finishScreen.setVisible(false);
        finishScreen.setManaged(false);
        interactiveFinish = false;
        finishActions.getChildren().clear();
    }

    public void setPostMatchChat(Parent panel) {
        boolean open = panel != null;
        timer.setManaged(timer.isVisible() && (!open || !timer.getText().isBlank()));
        result.setManaged(!open || !result.getText().isBlank());
        rematch.setManaged(!open || !rematch.getText().isBlank());
        finishScores.setManaged(!open || !finishScores.getText().isBlank());
        if (panel == null) {
            postMatchChat.getChildren().clear();
            root.setBottom(null);
        } else {
            postMatchChat.getChildren().setAll(panel);
            root.setBottom(postMatchChat);
        }
    }

    public boolean hasPostMatchChat() {
        return !postMatchChat.getChildren().isEmpty();
    }

    private void requestFinishFocus() {
        if (!postMatchChat.isFocusWithin()) requestFocus();
    }
    public void showFinish(String winner, String reason, int first, int second, boolean local) {
        finishTitle.setText(winner);
        finishReason.setText(reason);
        boolean p2p = "SERVER".equals(p1.name.getText());
        finishScores.setText((p2p ? "SERVER SCORE : " : "PLAYER 1 SCORE : ") + first
            + "       " + (p2p ? "CLIENT SCORE : " : "PLAYER 2 SCORE : ") + second);
        finishScores.setManaged(true);
        finishTitle.getStyleClass().removeAll("battle-finish-win", "battle-finish-lose");
        if (winner != null && (winner.contains("WINS") || winner.equals("YOU WIN!")))
            finishTitle.getStyleClass().add("battle-finish-win");
        else if ("YOU LOSE".equals(winner))
            finishTitle.getStyleClass().add("battle-finish-lose");
        finishGuide.setText(local ? "ENTER : REMATCH     TAB : 뒤로가기" : "");
        finishGuide.setManaged(local);
        finishActions.getChildren().clear();
        interactiveFinish = false;
        finishScreen.setManaged(true);
        finishScreen.setVisible(true);
    }
    /** A two-option result dialog. Mouse and arrow/Enter navigation share the same actions. */
    public void showChoice(String title, String detail, String left, Runnable onLeft,
                           String right, Runnable onRight) {
        finishTitle.setText(title);
        finishTitle.getStyleClass().removeAll("battle-finish-win", "battle-finish-lose");
        if (title.contains("WIN")) finishTitle.getStyleClass().add("battle-finish-win");
        else if (title.contains("LOSE")) finishTitle.getStyleClass().add("battle-finish-lose");
        finishReason.setText(detail == null ? "" : detail);
        finishScores.setText("");
        finishScores.setManaged(!hasPostMatchChat());
        finishGuide.setText("");
        finishGuide.setManaged(false);
        firstAction = onLeft;
        secondAction = onRight;
        selectedAction = 0;
        interactiveFinish = true;
        finishActions.getChildren().clear();
        Label a = actionLabel(left, 0);
        Label b = actionLabel(right, 1);
        finishActions.getChildren().addAll(a, b);
        refreshSelection();
        finishScreen.setManaged(true);
        finishScreen.setVisible(true);
        requestFinishFocus();
    }

    /** Retains the win/loss title, reason and scores while enabling actions. */
    public void showResultChoices(String left, Runnable onLeft, String right, Runnable onRight) {
        firstAction = onLeft;
        secondAction = onRight;
        selectedAction = 0;
        interactiveFinish = true;
        finishGuide.setText("");
        finishGuide.setManaged(false);
        finishActions.getChildren().clear();
        finishActions.getChildren().addAll(actionLabel(left, 0), actionLabel(right, 1));
        refreshSelection();
        finishScreen.setManaged(true);
        finishScreen.setVisible(true);
        requestFinishFocus();
    }

    public void showWaiting(String title, String detail) {
        showChoice(title, detail, "MAIN MENU", null, "CANCEL", null);
        // Waiting is not a menu; no accidental Enter while waiting for response.
        interactiveFinish = false;
        finishActions.getChildren().clear();
        finishGuide.setManaged(true);
        finishGuide.setText("WAITING FOR OPPONENT...");
    }

    private Label actionLabel(String text, int index) {
        Label label = new Label(text);
        label.getStyleClass().add("battle-choice");
        label.setMinWidth(142);
        label.setAlignment(Pos.CENTER);
        label.setCursor(Cursor.HAND);
        label.setOnMouseClicked(e -> {
            selectedAction = index;
            refreshSelection();
            activateSelection();
            requestFocus();
        });
        return label;
    }

    private void refreshSelection() {
        for (int i = 0; i < finishActions.getChildren().size(); i++) {
            var node = finishActions.getChildren().get(i);
            node.getStyleClass().remove("battle-choice-selected");
            if (i == selectedAction) node.getStyleClass().add("battle-choice-selected");
        }
    }

    private void activateSelection() {
        Runnable action = selectedAction == 0 ? firstAction : secondAction;
        if (action != null) action.run();
    }

    public boolean handleFinishKey(KeyCode key) {
        if (!interactiveFinish) return false;
        if (key == KeyCode.LEFT || key == KeyCode.UP) {
            selectedAction = 0; refreshSelection(); return true;
        }
        if (key == KeyCode.RIGHT || key == KeyCode.DOWN) {
            selectedAction = 1; refreshSelection(); return true;
        }
        if (key == KeyCode.ENTER) {
            activateSelection(); return true;
        }
        return false;
    }

    // Preserve the last rendered scores even after the network service stops the battle.
    public int getDisplayedScore(PlayerId player) {
        String text = (player == PlayerId.PLAYER1 ? p1.score : p2.score).getText();
        try { return Integer.parseInt(text); }
        catch (NumberFormatException ignored) { return 0; }
    }

    public void setPlayerNames(boolean p2p) {
        p1.name.setText(p2p ? "SERVER" : "PLAYER 1");
        p2.name.setText(p2p ? "CLIENT" : "PLAYER 2");
    }

    public void setControlsVisible(PlayerId id, boolean visible) {
        (id == PlayerId.PLAYER1 ? p1 : p2).controlBox.setVisible(visible);
    }
    public void applyColorBlindMode(boolean enabled) {
        p1.game.applyColorBlindMode(enabled);
        p2.game.applyColorBlindMode(enabled);
    }

    public void setItemMode(boolean enabled) {
        root.getStyleClass().remove("battle-item-mode");
        if (enabled) root.getStyleClass().add("battle-item-mode");
        itemInfo.setVisible(enabled);
        itemInfo.setManaged(false);
        itemInfo.getParent().requestLayout();
        p1.game.setBattleItemMode(enabled);
        p2.game.setBattleItemMode(enabled);
    }
    public void updateControlGuides(Settings settings, PlayerId localPlayer) {
        p1.updateControls(settings, PlayerId.PLAYER1, localPlayer);
        p2.updateControls(settings, PlayerId.PLAYER2, localPlayer);
    }
    public void setOnBackAction(Runnable action) { onBackAction = action; }
    public void runBackAction() { if (onBackAction != null) onBackAction.run(); }
    public void requestFocus() { root.requestFocus(); }

    private static class PlayerArea {
        final VBox root;
        final Label name;
        final Label[] controlKeys = new Label[5];
        final VBox controlBox;
        final VBox info;
        final Node board;
        final StackPane attackFrame;
        final GameView game = new GameView();
        final Label score = new Label("0"), level = new Label("LEVEL : 1");
        final Label skip = new Label("SKIP : 7"), incoming = new Label("INCOMING : 0 / 10");
        private static final int ATTACK_ROWS = 10, ATTACK_COLS = Board.COLS;
        private final Rectangle[][] attackCells = new Rectangle[ATTACK_ROWS][ATTACK_COLS];
        private final GridPane attackPreview = new GridPane();
        PlayerArea(String name, boolean first) {
            this.name = new Label(name);
            Label player = this.name;
            player.getStyleClass().add("battle-player-name");
            Label nextTitle = new Label("NEXT");
            nextTitle.getStyleClass().add("battle-next-title");
            VBox preview = new VBox(2, nextTitle, game.detachBattleNext());
            preview.setAlignment(Pos.CENTER);
            Label scoreTitle = new Label("SCORE");
            scoreTitle.getStyleClass().add("battle-score-title");
            score.getStyleClass().add("battle-score");
            VBox playerScore = new VBox(3, player, scoreTitle, score);
            playerScore.setAlignment(Pos.CENTER);
            HBox header = new HBox(28, playerScore, preview);
            header.setAlignment(first ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            level.getStyleClass().add("battle-info-value");
            skip.getStyleClass().add("battle-info-value");
            incoming.getStyleClass().add("battle-attack");
            attackPreview.getStyleClass().add("battle-incoming-grid");
            attackPreview.setHgap(1);
            attackPreview.setVgap(1);
            for (int r = 0; r < ATTACK_ROWS; r++) {
                for (int c = 0; c < ATTACK_COLS; c++) {
                    Rectangle cell = new Rectangle(7, 7);
                    cell.getStyleClass().add("battle-incoming-cell");
                    attackCells[r][c] = cell;
                    attackPreview.add(cell, c, r);
                }
            }
            attackFrame = new StackPane(attackPreview);
            attackFrame.getStyleClass().add("battle-incoming-frame");
            info = new VBox(7, level, skip, incoming, attackFrame);
            info.setAlignment(Pos.TOP_CENTER);
            board = game.detachBattleBoard();
            HBox field = first ? new HBox(18, board, info) : new HBox(18, info, board);
            field.setAlignment(Pos.CENTER);
            controlBox = controls(first);
            controlBox.setMaxWidth(Region.USE_PREF_SIZE);
            root = new VBox(18, header, field, controlBox);
            root.setAlignment(first ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
            root.getStyleClass().add("battle-player");
            root.setStyle(first ? "-fx-alignment: top-left;" : "-fx-alignment: top-right;");
        }

        boolean animating;

        void render(GameSnapshot s) {
            if (!animating) game.renderBattle(s);
            if (s == null) return;
            score.setText(String.valueOf(s.getScore()));
            level.setText("LEVEL : " + s.getLevel());
            skip.setText("SKIP : " + s.getRemainingSkips());
            AttackRows pending = s.getPendingAttack();
            incoming.setText("INCOMING : " + pending.getRowCount() + " / 10");
            BlockType[][] rows = pending.getRows();
            int visible = Math.min(rows.length, ATTACK_ROWS);
            for (int r = 0; r < ATTACK_ROWS; r++) {
                int source = r - (ATTACK_ROWS - visible);
                for (int c = 0; c < ATTACK_COLS; c++) {
                    boolean filled = source >= 0 && rows[source] != null
                        && c < rows[source].length && rows[source][c] != null;
                    attackCells[r][c].setFill(filled ? Color.web("#9B9B9B") : Color.web("#111111"));
                }
            }
        }

        void updateControls(Settings settings, PlayerId player, PlayerId localPlayer) {
            boolean ownControls = localPlayer == null || localPlayer == player;
            controlBox.setVisible(ownControls);
            // Keep both player areas aligned, even when the remote guide is hidden.
            if (!ownControls) return;
            java.util.function.Function<Settings.KeyAction, String> keyName = action -> {
                KeyCode key = localPlayer == null ? settings.getBattleKey(player, action) : settings.getKey(action);
                return key == null ? "-" : key.getName();
            };
            controlKeys[0].setText(keyName.apply(Settings.KeyAction.MOVE_LEFT) + " / " + keyName.apply(Settings.KeyAction.MOVE_RIGHT));
            controlKeys[1].setText(keyName.apply(Settings.KeyAction.SOFT_DROP));
            controlKeys[2].setText(keyName.apply(Settings.KeyAction.ROTATE));
            controlKeys[3].setText(keyName.apply(Settings.KeyAction.HARD_DROP));
            controlKeys[4].setText(keyName.apply(Settings.KeyAction.SKIP));
        }

        private VBox controls(boolean first) {
            Label title = new Label("CONTROLS");
            title.getStyleClass().add("game-info-title");
            GridPane grid = new GridPane();
            grid.setAlignment(Pos.CENTER);
            grid.setHgap(12);
            grid.setVgap(6);
            String[] keys = first
                ? new String[]{"A / D", "S", "W", "SPACE", "CTRL"}
                : new String[]{"← / →", "↓", "↑", "ENTER", "P"};
            String[] names = {"이동", "아래", "회전", "하드드랍", "스킵"};
            for (int i = 0; i < keys.length; i++) {
                Label key = new Label(keys[i]);
                controlKeys[i] = key;
                key.setMinWidth(58);
                key.setAlignment(Pos.CENTER);
                key.getStyleClass().addAll("game-small-bordered-label", "battle-control-key");

                Label action = new Label(names[i]);
                action.getStyleClass().addAll("item-info-action", "battle-control-action");

                grid.add(action, (i / 3) * 2, i % 3);
                grid.add(key, (i / 3) * 2 + 1, i % 3);
            }
            VBox box = new VBox(3, title, grid);
            box.setAlignment(Pos.CENTER);
            return box;
        }
    }
}
