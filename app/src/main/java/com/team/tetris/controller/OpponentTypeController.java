package com.team.tetris.controller;

import java.util.function.Consumer;

import com.team.tetris.view.OpponentTypeView;
import com.team.tetris.view.OpponentTypeView.OpponentType;

import javafx.scene.input.KeyCode;

public class OpponentTypeController {

    private final OpponentTypeView view;
    private final Consumer<OpponentType> onSelect;
    private final Runnable onBack;

    private OpponentType selected = OpponentType.TWO_PLAYER;

    public OpponentTypeController(
        OpponentTypeView view,
        Consumer<OpponentType> onSelect,
        Runnable onBack
    ) {
        this.view = view;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void show() {
        selected = OpponentType.TWO_PLAYER;
        view.render(selected);
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.UP) {
            move(-1);
        } else if (key == KeyCode.DOWN) {
            move(1);
        } else if (key == KeyCode.ENTER) {
            onSelect.accept(selected);
        } else if (key == KeyCode.TAB || key == KeyCode.ESCAPE) {
            onBack.run();
        }
    }

    private void move(int direction) {
        OpponentType[] items = OpponentType.values();
        int index = Math.floorMod(selected.ordinal() + direction, items.length);
        selected = items[index];
        view.render(selected);
    }

    public OpponentType getSelected() {
        return selected;
    }
}
