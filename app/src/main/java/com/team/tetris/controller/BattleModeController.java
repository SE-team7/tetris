package com.team.tetris.controller;

import java.util.function.Consumer;

import com.team.tetris.view.BattleModeView;
import com.team.tetris.view.BattleModeView.BattleModeItem;

import javafx.scene.input.KeyCode;

public class BattleModeController {

    private final BattleModeView view;
    private final Consumer<BattleModeItem> onSelect;
    private final Runnable onBack;

    private BattleModeItem selected = BattleModeItem.NORMAL;

    public BattleModeController(
        BattleModeView view,
        Consumer<BattleModeItem> onSelect,
        Runnable onBack
    ) {
        this.view = view;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void show() {
        selected = BattleModeItem.NORMAL;
        view.render(selected);
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.UP) {
            move(-1);
        } else if (key == KeyCode.DOWN) {
            move(1);
        } else if (key == KeyCode.ENTER) {
            select();
        } else if (key == KeyCode.ESCAPE) {
            onBack.run();
        }
    }

    private void move(int direction) {
        BattleModeItem[] items = BattleModeItem.values();
        int index = (selected.ordinal() + direction + items.length) % items.length;

        selected = items[index];
        view.render(selected);
    }

    private void select() {
        if (selected == BattleModeItem.BACK) {
            onBack.run();
        } else {
            onSelect.accept(selected);
        }
    }

    public BattleModeItem getSelected() {
        return selected;
    }
}