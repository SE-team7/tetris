package com.team.tetris.view;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Position;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class GameView {

    private static final int VISIBLE_ROWS = 20;
    private static final int COLS = 10;
    private static final int NEXT_SIZE = 4;
    private static final int MAX_SKIP_COUNT = 7;

    private final BorderPane root;

    private final GridPane boardGrid;
    private final Label[][] boardCells;

    private final GridPane nextGrid;
    private final Label[][] nextCells;

    private final Label scoreLabel;
    private final Label statusLabel;
    private final Label skipRemainingLabel;

    private final Label moveKeyLabel;
    private final Label softDropKeyLabel;
    private final Label rotateKeyLabel;
    private final Label hardDropKeyLabel;
    private final Label skipKeyLabel;

    private boolean colorBlindMode;

    public GameView() {

        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #111111;");

        /*
         * BOARD
         */
        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(1);
        boardGrid.setVgap(0);

        boardCells = new Label[VISIBLE_ROWS][COLS];

        createTextBoard();

        /*
         * NEXT
         */
        Label nextTitle = createSectionTitle("NEXT");

        nextGrid = new GridPane();
        nextGrid.setAlignment(Pos.CENTER);
        nextGrid.setHgap(2);
        nextGrid.setVgap(1);

        nextCells = new Label[NEXT_SIZE][NEXT_SIZE];

        createNextArea();

        /*
         * SCORE
         */
        Label scoreTitle = createSectionTitle("SCORE");

        scoreLabel = new Label("0");
        scoreLabel.setStyle(
            "-fx-text-fill: white;"
            + "-fx-font-family: 'Consolas';"
            + "-fx-font-size: 24px;"
            + "-fx-font-weight: bold;"
        );

        /*
         * PAUSE STATUS
         */
        statusLabel = new Label("");
        statusLabel.setStyle(
            "-fx-text-fill: #FFD54F;"
            + "-fx-font-family: 'Consolas';"
            + "-fx-font-size: 14px;"
            + "-fx-font-weight: bold;"
        );

        /*
         * SKIP
         */
        Label skipTitle = createSectionTitle("SKIP");

        skipRemainingLabel =
            new Label(MAX_SKIP_COUNT + " / " + MAX_SKIP_COUNT);

        skipRemainingLabel.setStyle(
            "-fx-text-fill: white;"
            + "-fx-font-family: 'Consolas';"
            + "-fx-font-size: 18px;"
            + "-fx-font-weight: bold;"
        );

        VBox skipBox = new VBox(
            2,
            skipTitle,
            skipRemainingLabel
        );

        skipBox.setAlignment(Pos.CENTER);

        /*
         * TOP RIGHT
         */
        VBox infoBox = new VBox(
            8,
            nextTitle,
            nextGrid,
            scoreTitle,
            scoreLabel,
            statusLabel,
            skipBox
        );

        infoBox.setAlignment(Pos.TOP_CENTER);

        /*
         * CONTROLS
         */
        Label controlsTitle =
            createSectionTitle("CONTROLS");

        GridPane controlsGrid = new GridPane();

        controlsGrid.setAlignment(Pos.CENTER);
        controlsGrid.setHgap(10);
        controlsGrid.setVgap(3);

        moveKeyLabel =
            createControlKeyLabel("A D");

        softDropKeyLabel =
            createControlKeyLabel("S");

        rotateKeyLabel =
            createControlKeyLabel("P");

        hardDropKeyLabel =
            createControlKeyLabel("ENTER");

        skipKeyLabel =
            createControlKeyLabel("-");

        int row = 0;

        addControlRow(
            controlsGrid,
            row++,
            "이동",
            moveKeyLabel
        );

        addControlRow(
            controlsGrid,
            row++,
            "아래",
            softDropKeyLabel
        );

        addControlRow(
            controlsGrid,
            row++,
            "회전",
            rotateKeyLabel
        );

        addControlRow(
            controlsGrid,
            row++,
            "하드드랍",
            hardDropKeyLabel
        );

        addControlRow(
            controlsGrid,
            row++,
            "스킵",
            skipKeyLabel
        );

        addControlRow(
            controlsGrid,
            row++,
            "일시정지",
            createControlKeyLabel("SHIFT")
        );

        addControlRow(
            controlsGrid,
            row++,
            "메인메뉴",
            createControlKeyLabel("TAB")
        );

        addControlRow(
            controlsGrid,
            row,
            "게임종료",
            createControlKeyLabel("ESC")
        );

        VBox controlsBox = new VBox(
            4,
            controlsTitle,
            controlsGrid
        );

        controlsBox.setAlignment(Pos.CENTER);

        /*
         * RIGHT PANEL
         */
        BorderPane sidePanel = new BorderPane();

        sidePanel.setTop(infoBox);
        sidePanel.setBottom(controlsBox);

        BorderPane.setAlignment(
            infoBox,
            Pos.TOP_CENTER
        );

        BorderPane.setAlignment(
            controlsBox,
            Pos.BOTTOM_CENTER
        );

        sidePanel.setPadding(
            new Insets(8, 10, 8, 18)
        );

        sidePanel.setPrefWidth(225);

        root.setCenter(boardGrid);
        root.setRight(sidePanel);
    }

    /*
     * BOARD
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

                    resetBoardCell(cell);

                    boardCells[row - 1][col - 1] =
                        cell;
                }

                boardGrid.add(
                    cell,
                    col,
                    row
                );
            }
        }
    }

    /*
     * NEXT
     */
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

                    nextCells[row - 1][col - 1] =
                        cell;
                }

                nextGrid.add(
                    cell,
                    col,
                    row
                );
            }
        }
    }

    private Label createTextCell() {

        Label label = new Label(" ");

        label.setAlignment(Pos.CENTER);

        label.setMinSize(22, 22);
        label.setPrefSize(22, 22);
        label.setMaxSize(22, 22);

        label.setStyle(
            baseCellStyle()
        );

        return label;
    }

    private String baseCellStyle() {

        return
            "-fx-font-family: 'Consolas';"
            + "-fx-font-size: 18px;"
            + "-fx-background-color: #111111;"
            + "-fx-text-fill: white;";
    }

    private Label createSectionTitle(
        String text
    ) {

        Label label = new Label(text);

        label.setStyle(
            "-fx-text-fill: white;"
            + "-fx-font-family: 'Consolas';"
            + "-fx-font-size: 17px;"
            + "-fx-font-weight: bold;"
        );

        return label;
    }

    private Label createControlKeyLabel(
        String text
    ) {

        Label label = new Label(text);

        label.setAlignment(Pos.CENTER);
        label.setMinWidth(68);

        label.setStyle(
            "-fx-text-fill: white;"
            + "-fx-font-family: 'Consolas';"
            + "-fx-font-size: 11px;"
            + "-fx-font-weight: bold;"
            + "-fx-border-color: #555555;"
            + "-fx-border-width: 1px;"
            + "-fx-padding: 1px 5px;"
        );

        return label;
    }

    private void addControlRow(
        GridPane grid,
        int row,
        String actionText,
        Label keyLabel
    ) {

        Label actionLabel =
            new Label(actionText);

        actionLabel.setStyle(
            "-fx-text-fill: #BBBBBB;"
            + "-fx-font-family: 'Consolas';"
            + "-fx-font-size: 11px;"
        );

        grid.add(
            actionLabel,
            0,
            row
        );

        grid.add(
            keyLabel,
            1,
            row
        );
    }

    /*
     * RENDER
     */
    public void render(
        BlockType[][] board,
        Block currentBlock,
        Block nextBlock,
        int score,
        int level,
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
            renderNextBlock(
                nextBlock
            );
        }

        updateScore(score);
        setPaused(paused);
    }

    private void renderFixedBlocks(
        BlockType[][] board
    ) {

        int firstVisibleRow =
            Math.max(
                0,
                board.length - VISIBLE_ROWS
            );

        for (
            int visibleRow = 0;
            visibleRow < VISIBLE_ROWS;
            visibleRow++
        ) {

            int boardRow =
                firstVisibleRow
                + visibleRow;

            if (boardRow >= board.length) {
                break;
            }

            for (
                int col = 0;
                col < COLS
                    && col < board[boardRow].length;
                col++
            ) {

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

    private void renderCurrentBlock(
        BlockType[][] board,
        Block currentBlock
    ) {

        int boardRows =
            board == null
                ? Board.ROWS
                : board.length;

        int firstVisibleRow =
            Math.max(
                0,
                boardRows - VISIBLE_ROWS
            );

        for (
            Position position :
            currentBlock.getCells()
        ) {

            int visibleRow =
                position.getRow()
                - firstVisibleRow;

            int col =
                position.getCol();

            if (
                visibleRow >= 0
                && visibleRow < VISIBLE_ROWS
                && col >= 0
                && col < COLS
            ) {

                drawBlockCell(
                    boardCells[visibleRow][col],
                    currentBlock.getType()
                );
            }
        }
    }

    private void renderNextBlock(
        Block nextBlock
    ) {

        Position[] cells =
            nextBlock.getCells();

        if (
            cells == null
            || cells.length == 0
        ) {
            return;
        }

        int minRow =
            cells[0].getRow();

        int maxRow =
            cells[0].getRow();

        int minCol =
            cells[0].getCol();

        int maxCol =
            cells[0].getCol();

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

            if (
                row >= 0
                && row < NEXT_SIZE
                && col >= 0
                && col < NEXT_SIZE
            ) {

                drawBlockCell(
                    nextCells[row][col],
                    nextBlock.getType()
                );
            }
        }
    }

    /*
     * BLOCK DRAW
     */
    private void drawBlockCell(
        Label cell,
        BlockType type
    ) {

        if (colorBlindMode) {

            if (type == BlockType.L) {

                cell.setText("G");

            } else {

                cell.setText(
                    type.name()
                );
            }

        } else {

            cell.setText("O");
        }

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

    /*
     * BOARD RESET
     */
    private void clearBoard() {

        for (
            int row = 0;
            row < VISIBLE_ROWS;
            row++
        ) {

            for (
                int col = 0;
                col < COLS;
                col++
            ) {

                resetBoardCell(
                    boardCells[row][col]
                );
            }
        }
    }

    private void resetBoardCell(
        Label cell
    ) {

        cell.setText(".");

        cell.setStyle(
            baseCellStyle()
            + "-fx-text-fill: #444444;"
        );
    }

    /*
     * NEXT RESET
     */
    private void clearNext() {

        for (
            int row = 0;
            row < NEXT_SIZE;
            row++
        ) {

            for (
                int col = 0;
                col < NEXT_SIZE;
                col++
            ) {

                nextCells[row][col]
                    .setText(" ");

                nextCells[row][col]
                    .setStyle(
                        baseCellStyle()
                    );
            }
        }
    }

    public Parent getRoot() {
        return root;
    }

    public void updateScore(
        int score
    ) {

        scoreLabel.setText(
            String.valueOf(score)
        );
    }

    /*
     * SKIP
     */
    public void updateSkipInfo(
        String skipKey,
        int remaining
    ) {

        skipKeyLabel.setText(
            safeKey(skipKey)
        );

        skipRemainingLabel.setText(
            remaining
            + " / "
            + MAX_SKIP_COUNT
        );
    }

    /*
     * CONTROL DISPLAY
     */
    public void setControlKeyGuide(
        String moveLeft,
        String moveRight,
        String softDrop,
        String rotate,
        String hardDrop,
        String skip
    ) {

        moveKeyLabel.setText(
            safeKey(moveLeft)
            + " "
            + safeKey(moveRight)
        );

        softDropKeyLabel.setText(
            safeKey(softDrop)
        );

        rotateKeyLabel.setText(
            safeKey(rotate)
        );

        hardDropKeyLabel.setText(
            safeKey(hardDrop)
        );

        skipKeyLabel.setText(
            safeKey(skip)
        );
    }

    private String safeKey(
        String key
    ) {

        if (
            key == null
            || key.isBlank()
        ) {
            return "-";
        }

        return key;
    }

    public void applyColorBlindMode(
        boolean enabled
    ) {

        colorBlindMode = enabled;
    }

    public void setPaused(
        boolean paused
    ) {

        statusLabel.setText(
            paused
                ? "PAUSED"
                : ""
        );
    }
}