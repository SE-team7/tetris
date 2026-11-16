package com.team.tetris.view;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Position;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BattleView {

    private static final int ROWS = 20;
    private static final int COLS = Board.COLS;
    private static final int NEXT = 4;

    private final BorderPane root = new BorderPane();
    private final Label timerLabel = new Label("TIME --:--");
    private final Label statusLabel = new Label("READY");
    private final Label resultLabel = new Label();
    private final Label rematchLabel = new Label();

    private final PlayerArea p1 = new PlayerArea("PLAYER 1");
    private final PlayerArea p2 = new PlayerArea("PLAYER 2");

    private Runnable onBackAction;

    public BattleView() {
        root.getStyleClass().add("battle-root");
        root.setPadding(new Insets(16));

        Label title = new Label("BATTLE MODE");
        title.getStyleClass().add("battle-title");

        timerLabel.getStyleClass().add("battle-timer");
        statusLabel.getStyleClass().add("battle-status");
        resultLabel.getStyleClass().add("battle-result");
        rematchLabel.getStyleClass().add("battle-rematch");

        Label back = new Label("ESC : 뒤로가기");
        back.getStyleClass().add("battle-back");

        VBox top = new VBox(
            4, title, timerLabel, statusLabel,
            resultLabel, rematchLabel, back
        );
        top.setAlignment(Pos.CENTER);

        HBox players = new HBox(45, p1.root, p2.root);
        players.setAlignment(Pos.CENTER);

        root.setTop(top);
        root.setCenter(players);
        root.setFocusTraversable(true);
    }

    public Parent getRoot() {
        return root;
    }

    public void renderPlayer(PlayerId player, GameSnapshot snapshot) {
        PlayerArea area = player == PlayerId.PLAYER1 ? p1 : p2;
        area.render(snapshot);
    }

    public void setTimer(String text) {
        timerLabel.setText(text == null ? "" : text);
    }

    public void setStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    public void showResult(String result, String rematch) {
        resultLabel.setText(result == null ? "" : result);
        rematchLabel.setText(rematch == null ? "" : rematch);
    }

    public void clearResult() {
        resultLabel.setText("");
        rematchLabel.setText("");
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

    private static final class PlayerArea {

        final VBox root;
        final Label[][] boardCells = new Label[ROWS][COLS];
        final Label[][] nextCells = new Label[NEXT][NEXT];

        final Label score = new Label("0");
        final Label level = new Label("LEVEL : 1");
        final Label skip = new Label("SKIP : 7");
        final Label incoming = new Label("INCOMING : 0 / 10");

        PlayerArea(String name) {
            Label player = new Label(name);
            player.getStyleClass().add("battle-player-name");

            GridPane board = createBoard();
            GridPane next = createNext();

            Label nextTitle = new Label("NEXT");
            nextTitle.getStyleClass().add("battle-info-title");

            Label scoreTitle = new Label("SCORE");
            scoreTitle.getStyleClass().add("battle-score-title");

            score.getStyleClass().add("battle-score");
            level.getStyleClass().add("battle-info-small");
            skip.getStyleClass().add("battle-info-small");
            incoming.getStyleClass().add("battle-attack");

            VBox side = new VBox(
                5,
                nextTitle,
                next,
                scoreTitle,
                score,
                level,
                skip,
                incoming
            );
            side.setAlignment(Pos.TOP_CENTER);

            HBox game = new HBox(12, board, side);
            game.setAlignment(Pos.CENTER);

            Label controls = new Label(
                name.equals("PLAYER 1")
                    ? "A/D 이동   S 아래   W 회전   SPACE 하드드랍   CTRL 스킵"
                    : "←/→ 이동   ↓ 아래   ↑ 회전   ENTER 하드드랍   P 스킵"
            );
            controls.getStyleClass().add("battle-controls");

            root = new VBox(8, player, game, controls);
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("battle-player");
        }

        private GridPane createBoard() {
            GridPane grid = new GridPane();
            grid.getStyleClass().add("battle-board-grid");

            for (int row = 0; row < ROWS + 2; row++) {
                for (int col = 0; col < COLS + 2; col++) {
                    Label cell = cell();

                    if (row == 0 || row == ROWS + 1
                        || col == 0 || col == COLS + 1) {
                        cell.getStyleClass().add("battle-frame-cell");
                    } else {
                        cell.getStyleClass().add("battle-board-cell");
                        boardCells[row - 1][col - 1] = cell;
                    }

                    grid.add(cell, col, row);
                }
            }

            return grid;
        }

        private GridPane createNext() {
            GridPane grid = new GridPane();
            grid.getStyleClass().add("battle-next-grid");

            for (int row = 0; row < NEXT; row++) {
                for (int col = 0; col < NEXT; col++) {
                    Label cell = cell();
                    cell.getStyleClass().add("battle-next-cell");
                    nextCells[row][col] = cell;
                    grid.add(cell, col, row);
                }
            }

            return grid;
        }

        private Label cell() {
            Label cell = new Label();
            cell.setMinSize(17, 17);
            cell.setPrefSize(17, 17);
            cell.setMaxSize(17, 17);
            return cell;
        }

        void render(GameSnapshot snapshot) {
            clear(boardCells, "battle-board-cell");
            clear(nextCells, "battle-next-cell");

            if (snapshot == null) return;

            BlockType[][] board = snapshot.getBoard();
            boolean[][] attack = snapshot.getAttackCells();

            int firstRow = Math.max(0, board.length - ROWS);

            for (int r = 0; r < ROWS; r++) {
                int sourceRow = firstRow + r;
                if (sourceRow >= board.length) continue;

                for (int c = 0; c < COLS; c++) {
                    BlockType type = board[sourceRow][c];

                    if (type != null) {
                        applyBlock(
                            boardCells[r][c],
                            type,
                            attack != null
                                && sourceRow < attack.length
                                && c < attack[sourceRow].length
                                && attack[sourceRow][c]
                        );
                    }
                }
            }

            renderCurrent(snapshot.getCurrentBlock(), firstRow);
            renderNext(snapshot.getNextBlock());

            score.setText(String.valueOf(snapshot.getScore()));
            level.setText("LEVEL : " + snapshot.getLevel());
            skip.setText("SKIP : " + snapshot.getRemainingSkips());
            incoming.setText(
                "INCOMING : "
                    + snapshot.getPendingAttack().getRowCount()
                    + " / 10"
            );
        }

        private void renderCurrent(Block block, int firstRow) {
            if (block == null) return;

            for (Position position : block.getCells()) {
                int row = position.getRow() - firstRow;
                int col = position.getCol();

                if (row >= 0 && row < ROWS
                    && col >= 0 && col < COLS) {
                    applyBlock(
                        boardCells[row][col],
                        block.getType(),
                        false
                    );
                }
            }
        }

        private void renderNext(Block block) {
            if (block == null) return;

            Position[] cells = block.getCells();
            if (cells == null || cells.length == 0) return;

            int minR = cells[0].getRow();
            int maxR = minR;
            int minC = cells[0].getCol();
            int maxC = minC;

            for (Position p : cells) {
                minR = Math.min(minR, p.getRow());
                maxR = Math.max(maxR, p.getRow());
                minC = Math.min(minC, p.getCol());
                maxC = Math.max(maxC, p.getCol());
            }

            int startR = (NEXT - (maxR - minR + 1)) / 2;
            int startC = (NEXT - (maxC - minC + 1)) / 2;

            for (Position p : cells) {
                int row = startR + p.getRow() - minR;
                int col = startC + p.getCol() - minC;

                if (row >= 0 && row < NEXT
                    && col >= 0 && col < NEXT) {
                    applyBlock(
                        nextCells[row][col],
                        block.getType(),
                        false
                    );
                }
            }
        }

        private void clear(Label[][] cells, String base) {
            for (Label[] row : cells) {
                for (Label cell : row) {
                    cell.setText("");
                    cell.getStyleClass().removeIf(
                        style -> style.startsWith("battle-block-")
                    );

                    if (!cell.getStyleClass().contains(base)) {
                        cell.getStyleClass().add(base);
                    }
                }
            }
        }

        private void applyBlock(
            Label cell,
            BlockType type,
            boolean attack
        ) {
            cell.getStyleClass().removeIf(
                style -> style.startsWith("battle-block-")
            );

            if (attack) {
                cell.getStyleClass().add("battle-block-attack");
                return;
            }

            if (type != null) {
                cell.getStyleClass().add(
                    "battle-block-" + type.name().toLowerCase()
                );
            }
        }
    }
}