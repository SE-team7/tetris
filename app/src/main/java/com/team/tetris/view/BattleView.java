package com.team.tetris.view;

import com.team.tetris.domain.*;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;

public class BattleView {
    private final BorderPane root = new BorderPane();
    private final Label timer = new Label(""), status = new Label("");
    private final Label result = new Label(), rematch = new Label();
    private final PlayerArea p1 = new PlayerArea("PLAYER 1", true);
    private final PlayerArea p2 = new PlayerArea("PLAYER 2", false);
    private Runnable onBackAction;

    public BattleView() {
        root.getStyleClass().add("battle-root");
        Label title = new Label("BATTLE MODE");
        title.getStyleClass().add("battle-title");
        timer.getStyleClass().add("battle-timer");
        status.getStyleClass().add("battle-status");
        result.getStyleClass().add("battle-result");
        rematch.getStyleClass().add("battle-rematch");
        VBox top = new VBox(2, title, timer, status, result, rematch);
        top.setAlignment(Pos.CENTER);
        HBox players = new HBox(100, p1.root, p2.root);
        players.setAlignment(Pos.CENTER);
        VBox content = new VBox(14, top, players);
        content.setAlignment(Pos.CENTER);
        root.setCenter(fitted(content));
        root.setFocusTraversable(true);
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
    public void setTimer(String text) { timer.setText(text == null ? "" : text); }
    public void setStatus(String text) { status.setText(text == null ? "" : text); }
    public void showResult(String text, String rematchText) {
        result.setText(text == null ? "" : text);
        rematch.setText(rematchText == null ? "" : rematchText);
    }
    public void clearResult() { showResult("", ""); }
    public void setItemMode(boolean enabled) {
        root.getStyleClass().remove("battle-item-mode");
        if (enabled) root.getStyleClass().add("battle-item-mode");
    }
    public void setOnBackAction(Runnable action) { onBackAction = action; }
    public void runBackAction() { if (onBackAction != null) onBackAction.run(); }
    public void requestFocus() { root.requestFocus(); }

    private static class PlayerArea {
        final VBox root;
        final GameView game = new GameView();
        final Label score = new Label("0"), level = new Label("LEVEL : 1");
        final Label skip = new Label("SKIP : 7"), incoming = new Label("INCOMING : 0 / 10");

        PlayerArea(String name, boolean first) {
            Label player = new Label(name);
            player.getStyleClass().add("battle-player-name");
            Label nextTitle = new Label("NEXT");
            nextTitle.getStyleClass().add("battle-next-title");
            VBox preview = new VBox(2, nextTitle, game.detachBattleNext());
            preview.setAlignment(Pos.CENTER);
            HBox header = new HBox(28, player, preview);
            header.setAlignment(Pos.CENTER);
            Label scoreTitle = new Label("SCORE");
            scoreTitle.getStyleClass().add("battle-score-title");
            score.getStyleClass().add("battle-score");
            level.getStyleClass().add("battle-info-value");
            skip.getStyleClass().add("battle-info-value");
            incoming.getStyleClass().add("battle-attack");
            VBox info = new VBox(5, scoreTitle, score, level, skip, incoming);
            info.setAlignment(Pos.TOP_CENTER);
            HBox field = new HBox(18, game.detachBattleBoard(), info);
            field.setAlignment(Pos.CENTER);
            root = new VBox(18, header, field, controls(first));
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("battle-player");
        }

        void render(GameSnapshot s) {
            game.renderBattle(s);
            if (s == null) return;
            score.setText(String.valueOf(s.getScore()));
            level.setText("LEVEL : " + s.getLevel());
            skip.setText("SKIP : " + s.getRemainingSkips());
            incoming.setText("INCOMING : " + s.getPendingAttack().getRowCount() + " / 10");
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
                key.setMinWidth(58);
                key.setAlignment(Pos.CENTER);
                key.getStyleClass().add("game-small-bordered-label");
                key.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                Label action = new Label(names[i]);
                action.getStyleClass().add("item-info-action");
                action.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                grid.add(action, (i / 3) * 2, i % 3);
                grid.add(key, (i / 3) * 2 + 1, i % 3);
            }
            VBox box = new VBox(3, title, grid);
            box.setAlignment(Pos.CENTER);
            return box;
        }
    }
}
