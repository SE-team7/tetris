package com.team.tetris.view;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Position;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class GameView {

    private static final int VISIBLE_ROWS = 20;
    private static final int COLS = 10;
    private static final int NEXT_SIZE = 4;

    private final BorderPane root;

    private final GridPane boardGrid;
    private final Label[][] boardCells;

    private final GridPane nextGrid;
    private final Label[][] nextCells;

    private final Label scoreLabel;
    private final Label statusLabel;

    private final Button pauseButton;
    private final Button quitButton;

    private boolean colorBlindMode;
    private boolean paused;

    private Runnable pauseAction;
    private Runnable quitAction;

    public GameView() {

        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #111111;");

        /*
         * BOARD
         */
        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(2);
        boardGrid.setVgap(1);

        boardCells =
            new Label[VISIBLE_ROWS][COLS];

        createTextBoard();

        /*
         * NEXT
         */
        Label nextTitle = createTitle("NEXT");

        nextGrid = new GridPane();
        nextGrid.setAlignment(Pos.CENTER);
        nextGrid.setHgap(2);
        nextGrid.setVgap(1);

        nextCells =
            new Label[NEXT_SIZE][NEXT_SIZE];

        createNextArea();

        /*
         * SCORE
         */
        Label scoreTitle = createTitle("SCORE");

        scoreLabel = new Label("0");
        scoreLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 24px;"
        );

        /*
         * STATUS
         */
        statusLabel = new Label("");
        statusLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;"
        );

        /*
         * PAUSE / QUIT
         */
        pauseButton = new Button("일시정지");
        quitButton = new Button("게임 나가기");

        pauseButton.setPrefWidth(140);
        quitButton.setPrefWidth(140);

        pauseButton.setOnAction(event -> {
            if (pauseAction != null) {
                pauseAction.run();
            }
        });

        quitButton.setOnAction(event -> {
            if (quitAction != null) {
                quitAction.run();
            }
        });

        VBox sidePanel = new VBox(
            18,
            nextTitle,
            nextGrid,
            scoreTitle,
            scoreLabel,
            statusLabel,
            pauseButton,
            quitButton
        );

        sidePanel.setAlignment(Pos.TOP_CENTER);
        sidePanel.setPadding(new Insets(15));
        sidePanel.setPrefWidth(200);

        root.setCenter(boardGrid);
        root.setRight(sidePanel);
    }

    /*
     * Professor example:
     * X = board border
     * O / block letter = tetromino cell
     */
    private void createTextBoard() {

        boardGrid.getChildren().clear();

        for (int row = 0; row < VISIBLE_ROWS + 2; row++) {

            for (int col = 0; col < COLS + 2; col++) {

                boolean border =
                    row == 0
                    || row == VISIBLE_ROWS + 1
                    || col == 0
                    || col == COLS + 1;

                Label cell = createTextCell();

                if (border) {
                    cell.setText("X");
                    cell.setStyle(
                        baseCellStyle()
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                    );
                } else {
                    boardCells[row - 1][col - 1] = cell;
                }

                boardGrid.add(cell, col, row);
            }
        }
    }

    private void createNextArea() {

        nextGrid.getChildren().clear();

        for (int row = 0; row < NEXT_SIZE + 2; row++) {

            for (int col = 0; col < NEXT_SIZE + 2; col++) {

                boolean border =
                    row == 0
                    || row == NEXT_SIZE + 1
                    || col == 0
                    || col == NEXT_SIZE + 1;

                Label cell = createTextCell();

                if (border) {
                    cell.setText("X");
                    cell.setStyle(
                        baseCellStyle()
                        + "-fx-text-fill: #777777;"
                    );
                } else {
                    nextCells[row - 1][col - 1] = cell;
                }

                nextGrid.add(cell, col, row);
            }
        }
    }

    private Label createTextCell() {

        Label label = new Label(" ");

        label.setAlignment(Pos.CENTER);
        label.setMinSize(22, 22);
        label.setPrefSize(22, 22);
        label.setMaxSize(22, 22);

        label.setStyle(baseCellStyle());

        return label;
    }

    private String baseCellStyle() {

        return
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 18px;" +
            "-fx-background-color: #111111;" +
            "-fx-text-fill: white;";
    }

    private Label createTitle(String text) {

        Label label = new Label(text);

        label.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;"
        );

        return label;
    }

    /*
     * Main render API
     */
    public void render(
        BlockType[][] board,
        Block currentBlock,
        Block nextBlock,
        int score,
        boolean paused
    ) {

        clearBoard();
        clearNext();

        if (board != null) {
            renderFixedBlocks(board);
        }

        if (currentBlock != null) {
            renderCurrentBlock(
                board,
                currentBlock
            );
        }

        if (nextBlock != null) {
            renderNextBlock(nextBlock);
        }

        updateScore(score);
        setPaused(paused);
    }

    /*
     * Fixed blocks
     */
    private void renderFixedBlocks(
        BlockType[][] board
    ) {

        int firstVisibleRow =
            Math.max(
                0,
                board.length - VISIBLE_ROWS
            );

        for (int visibleRow = 0;
             visibleRow < VISIBLE_ROWS;
             visibleRow++) {

            int boardRow =
                firstVisibleRow + visibleRow;

            if (boardRow >= board.length) {
                break;
            }

            for (int col = 0;
                 col < COLS
                     && col < board[boardRow].length;
                 col++) {

                BlockType type =
                    board[boardRow][col];

                if (type != null) {
                    drawBlockCell(
                        boardCells[visibleRow][col],
                        type
                    );
                }
            }
        }
    }

    /*
     * Falling block
     */
    private void renderCurrentBlock(
        BlockType[][] board,
        Block currentBlock
    ) {

        int boardRows =
            board == null
                ? 25
                : board.length;

        int firstVisibleRow =
            Math.max(
                0,
                boardRows - VISIBLE_ROWS
            );

        for (Position position :
             currentBlock.getCells()) {

            int visibleRow =
                position.getRow()
                - firstVisibleRow;

            int col =
                position.getCol();

            if (visibleRow >= 0
                && visibleRow < VISIBLE_ROWS
                && col >= 0
                && col < COLS) {

                drawBlockCell(
                    boardCells[visibleRow][col],
                    currentBlock.getType()
                );
            }
        }
    }

    /*
     * NEXT block:
     * use coordinates given by Block.
     * UI does not redefine block shapes.
     */
    private void renderNextBlock(
        Block nextBlock
    ) {

        Position[] cells =
            nextBlock.getCells();

        if (cells == null
            || cells.length == 0) {
            return;
        }

        int minRow = cells[0].getRow();
        int maxRow = cells[0].getRow();

        int minCol = cells[0].getCol();
        int maxCol = cells[0].getCol();

        for (Position cell : cells) {

            minRow =
                Math.min(
                    minRow,
                    cell.getRow()
                );

            maxRow =
                Math.max(
                    maxRow,
                    cell.getRow()
                );

            minCol =
                Math.min(
                    minCol,
                    cell.getCol()
                );

            maxCol =
                Math.max(
                    maxCol,
                    cell.getCol()
                );
        }

        int height =
            maxRow - minRow + 1;

        int width =
            maxCol - minCol + 1;

        int startRow =
            (NEXT_SIZE - height) / 2;

        int startCol =
            (NEXT_SIZE - width) / 2;

        for (Position cell : cells) {

            int row =
                startRow
                + cell.getRow()
                - minRow;

            int col =
                startCol
                + cell.getCol()
                - minCol;

            if (row >= 0
                && row < NEXT_SIZE
                && col >= 0
                && col < NEXT_SIZE) {

                drawBlockCell(
                    nextCells[row][col],
                    nextBlock.getType()
                );
            }
        }
    }

    private void drawBlockCell(
        Label cell,
        BlockType type
    ) {

        cell.setText(
            colorBlindMode
                ? type.name()
                : "O"
        );

        cell.setStyle(
            baseCellStyle()
            + "-fx-text-fill: "
            + getBlockColor(type)
            + ";"
            + "-fx-font-weight: bold;"
        );
    }

    private String getBlockColor(
        BlockType type
    ) {

        if (colorBlindMode) {

            /*
             * Color-blind mode:
             * color + block letter are both used.
             */
            return switch (type) {

                case I -> "#56B4E9";
                case J -> "#0072B2";
                case L -> "#E69F00";
                case O -> "#F0E442";
                case S -> "#009E73";
                case T -> "#CC79A7";
                case Z -> "#D55E00";
            };
        }

        /*
         * Team-agreed normal colors.
         */
        return switch (type) {

            case I -> "#00FFFF";
            case J -> "#0000FF";
            case L -> "#FFA500";
            case O -> "#FFFF00";
            case S -> "#00FF00";
            case T -> "#800080";
            case Z -> "#FF0000";
        };
    }

    private void clearBoard() {

        for (int row = 0;
             row < VISIBLE_ROWS;
             row++) {

            for (int col = 0;
                 col < COLS;
                 col++) {

                resetCell(
                    boardCells[row][col]
                );
            }
        }
    }

    private void clearNext() {

        for (int row = 0;
             row < NEXT_SIZE;
             row++) {

            for (int col = 0;
                 col < NEXT_SIZE;
                 col++) {

                resetCell(
                    nextCells[row][col]
                );
            }
        }
    }

    private void resetCell(Label cell) {

        cell.setText(" ");
        cell.setStyle(baseCellStyle());
    }

    public Parent getRoot() {
        return root;
    }

    public void updateScore(int score) {
        scoreLabel.setText(
            String.valueOf(score)
        );
    }

    public void setColorBlindMode(
        boolean enabled
    ) {

        this.colorBlindMode = enabled;
    }

    public boolean isColorBlindMode() {
        return colorBlindMode;
    }

    public void setPaused(boolean paused) {

        this.paused = paused;

        pauseButton.setText(
            paused
                ? "계속하기"
                : "일시정지"
        );

        statusLabel.setText(
            paused
                ? "PAUSED"
                : ""
        );
    }

    public boolean isPaused() {
        return paused;
    }

    /*
     * GameController connection API
     */
    public void setGameActions(
        Runnable pauseAction,
        Runnable quitAction
    ) {

        this.pauseAction = pauseAction;
        this.quitAction = quitAction;
    }
}