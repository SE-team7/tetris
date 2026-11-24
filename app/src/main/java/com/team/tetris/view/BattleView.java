package com.team.tetris.view;

import com.team.tetris.domain.*;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class BattleView {

    private static final int ROWS = 20;
    private static final int COLS = Board.COLS;
    private static final int NEXT = 4;

    private final BorderPane root = new BorderPane();
    private final Label timer = new Label("TIME --:--");
    private final Label status = new Label("");
    private final Label lag = new Label("");
    private final Label result = new Label();
    private final Label rematch = new Label();

    private final PlayerArea p1 = new PlayerArea("PLAYER 1", true);
    private final PlayerArea p2 = new PlayerArea("PLAYER 2", false);

    private Runnable onBackAction;

    public BattleView() {
        root.getStyleClass().add("battle-root");

        Label title = new Label("BATTLE MODE");
        title.getStyleClass().add("battle-title");
        timer.getStyleClass().add("battle-timer");
        status.getStyleClass().add("battle-status");
        lag.getStyleClass().add("battle-lag");
        result.getStyleClass().add("battle-result");
        rematch.getStyleClass().add("battle-rematch");

        VBox top = new VBox(2, title, timer, status, lag, result, rematch);
        top.setAlignment(Pos.CENTER);

        HBox players = new HBox(30, p1.root, p2.root);
        players.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(players);
        root.setFocusTraversable(true);
    }

    public Parent getRoot() {
        return root;
    }

    public void renderPlayer(PlayerId player, GameSnapshot snapshot) {
        (player == PlayerId.PLAYER1 ? p1 : p2).render(snapshot);
    }

    public void setTimer(String text) {
        timer.setText(text == null ? "" : text);
    }

    public void setStatus(String text) {
        status.setText(text == null ? "" : text);
    }

    /** P2P 랙 표시. null 또는 빈 문자열이면 숨김 */
    public void setLag(String text) {
        lag.setText(text == null ? "" : text);
    }

    public String getLagText() {
        return lag.getText();
    }

    public void showResult(String text, String rematchText) {
        result.setText(text == null ? "" : text);
        rematch.setText(rematchText == null ? "" : rematchText);
    }

    public void clearResult() {
        result.setText("");
        rematch.setText("");
    }

    public void setItemMode(boolean enabled) {
        root.getStyleClass().remove("battle-item-mode");
        if (enabled) root.getStyleClass().add("battle-item-mode");
    }

    public void setOnBackAction(Runnable action) {
        onBackAction = action;
    }

    public void runBackAction() {
        if (onBackAction != null) onBackAction.run();
    }

    public void requestFocus() {
        root.requestFocus();
    }

    private static class PlayerArea {

        final VBox root;
        final Label[][] board = new Label[ROWS][COLS];
        final Label[][] next = new Label[NEXT][NEXT];

        final Label score = new Label("0");
        final Label level = new Label("LEVEL : 1");
        final Label skip = new Label("SKIP : 7");
        final Label incoming = new Label("INCOMING : 0 / 10");

        PlayerArea(String name, boolean first) {
            Label player = new Label(name);
            player.getStyleClass().add("battle-player-name");

            Label nextTitle = new Label("NEXT");
            nextTitle.getStyleClass().add("battle-next-title");

            GridPane nextGrid = createNext();
            VBox nextBox = new VBox(2, nextTitle, nextGrid);
            nextBox.setAlignment(Pos.CENTER);

            HBox header = new HBox(14, player, nextBox);
            header.setAlignment(Pos.CENTER);

            GridPane boardGrid = createBoard();

            Label scoreTitle = new Label("SCORE");
            scoreTitle.getStyleClass().add("battle-score-title");
            score.getStyleClass().add("battle-score");
            level.getStyleClass().add("battle-info-value");
            skip.getStyleClass().add("battle-info-value");
            incoming.getStyleClass().add("battle-attack");

            VBox info = new VBox(5, scoreTitle, score, level, skip, incoming);
            info.setAlignment(Pos.TOP_CENTER);

            HBox game = new HBox(10, boardGrid, info);
            game.setAlignment(Pos.CENTER);

            root = new VBox(6, header, game, createControls(first));
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("battle-player");
        }

        private GridPane createBoard() {
            GridPane grid = new GridPane();
            grid.getStyleClass().add("battle-board-grid");

            for (int r = 0; r < ROWS + 2; r++) {
                for (int c = 0; c < COLS + 2; c++) {
                    Label cell = new Label();
                    boolean frame = r == 0 || c == 0 || r == ROWS + 1 || c == COLS + 1;

                    cell.getStyleClass().add(
                        frame ? "battle-frame-cell" : "battle-board-cell"
                    );

                    if (!frame) board[r - 1][c - 1] = cell;
                    grid.add(cell, c, r);
                }
            }
            return grid;
        }

        private GridPane createNext() {
            GridPane grid = new GridPane();
            grid.getStyleClass().add("battle-next-grid");

            for (int r = 0; r < NEXT; r++) {
                for (int c = 0; c < NEXT; c++) {
                    Label cell = new Label();
                    cell.getStyleClass().add("battle-next-cell");
                    next[r][c] = cell;
                    grid.add(cell, c, r);
                }
            }
            return grid;
        }

        private VBox createControls(boolean first) {
            Label title = new Label("CONTROLS");
            title.getStyleClass().add("game-info-title");

            GridPane grid = new GridPane();
            grid.setAlignment(Pos.CENTER);
            grid.setHgap(7);
            grid.setVgap(3);

            if (first) {
                addControl(grid, 0, 0, "이동", "A / D");
                addControl(grid, 1, 0, "아래", "S");
                addControl(grid, 2, 0, "회전", "W");
                addControl(grid, 0, 2, "하드드랍", "SPACE");
                addControl(grid, 1, 2, "스킵", "CTRL");
                addControl(grid, 2, 2, "일시정지", "SHIFT");
            } else {
                addControl(grid, 0, 0, "이동", "← / →");
                addControl(grid, 1, 0, "아래", "↓");
                addControl(grid, 2, 0, "회전", "↑");
                addControl(grid, 0, 2, "하드드랍", "ENTER");
                addControl(grid, 1, 2, "스킵", "P");
                addControl(grid, 2, 2, "일시정지", "SHIFT");
            }

            VBox box = new VBox(3, title, grid);
            box.setAlignment(Pos.CENTER);
            return box;
        }

        private void addControl(
            GridPane grid, int row, int col, String action, String key
        ) {
            Label actionLabel = new Label(action);
            actionLabel.getStyleClass().add("item-info-action");

            Label keyLabel = new Label(key);
            keyLabel.setAlignment(Pos.CENTER);
            keyLabel.setMinWidth(58);
            keyLabel.getStyleClass().add("game-small-bordered-label");

            grid.add(actionLabel, col, row);
            grid.add(keyLabel, col + 1, row);
        }

        void render(GameSnapshot snapshot) {
            clear(board);
            clear(next);

            if (snapshot == null) return;

            BlockType[][] data = snapshot.getBoard();
            boolean[][] attacks = snapshot.getAttackCells();
            int first = Math.max(0, data.length - ROWS);

            for (int r = 0; r < ROWS; r++) {
                int source = first + r;
                if (source >= data.length) continue;

                for (int c = 0; c < COLS && c < data[source].length; c++) {
                    BlockType type = data[source][c];
                    if (type == null) continue;

                    boolean attack = attacks != null
                        && source < attacks.length
                        && c < attacks[source].length
                        && attacks[source][c];

                    draw(board[r][c], type, attack);
                }
            }

            renderCurrent(snapshot.getCurrentBlock(), first);
            renderNext(snapshot.getNextBlock());

            score.setText(String.valueOf(snapshot.getScore()));
            level.setText("LEVEL : " + snapshot.getLevel());
            skip.setText("SKIP : " + snapshot.getRemainingSkips());
            incoming.setText(
                "INCOMING : " + snapshot.getPendingAttack().getRowCount() + " / 10"
            );
        }

        private void renderCurrent(Block block, int first) {
            if (block == null) return;

            for (Position p : block.getCells()) {
                int r = p.getRow() - first;
                int c = p.getCol();

                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    draw(board[r][c], block.getType(), false);
                }
            }
        }

        private void renderNext(Block block) {
            if (block == null || block.getCells().length == 0) return;

            Position[] cells = block.getCells();
            int minR = cells[0].getRow(), maxR = minR;
            int minC = cells[0].getCol(), maxC = minC;

            for (Position p : cells) {
                minR = Math.min(minR, p.getRow());
                maxR = Math.max(maxR, p.getRow());
                minC = Math.min(minC, p.getCol());
                maxC = Math.max(maxC, p.getCol());
            }

            int startR = (NEXT - (maxR - minR + 1)) / 2;
            int startC = (NEXT - (maxC - minC + 1)) / 2;

            for (Position p : cells) {
                int r = startR + p.getRow() - minR;
                int c = startC + p.getCol() - minC;

                if (r >= 0 && r < NEXT && c >= 0 && c < NEXT) {
                    draw(next[r][c], block.getType(), false);
                }
            }
        }

        private void clear(Label[][] cells) {
            for (Label[] row : cells) {
                for (Label cell : row) {
                    cell.getStyleClass().removeIf(s -> s.startsWith("battle-block-"));
                }
            }
        }

        private void draw(Label cell, BlockType type, boolean attack) {
            cell.getStyleClass().removeIf(s -> s.startsWith("battle-block-"));
            cell.getStyleClass().add(
                attack
                    ? "battle-block-attack"
                    : "battle-block-" + type.name().toLowerCase()
            );
        }
    }
}