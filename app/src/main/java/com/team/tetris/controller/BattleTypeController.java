package com.team.tetris.controller;

import java.util.function.Consumer;

import com.team.tetris.view.BattleTypeView;
import com.team.tetris.view.BattleTypeView.BattleTypeItem;

import javafx.scene.input.KeyCode;

public class BattleTypeController {

    private final BattleTypeView view;
    private final Consumer<BattleTypeItem> onSelect;
    private final Runnable onBack;

    private BattleTypeItem selected = BattleTypeItem.LOCAL;

    public BattleTypeController(
        BattleTypeView view,
        Consumer<BattleTypeItem> onSelect,
        Runnable onBack
    ) {
        this.view = view;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void show() {
        selected = BattleTypeItem.LOCAL;
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
        BattleTypeItem[] items = BattleTypeItem.values();
        int index = (selected.ordinal() + direction + items.length) % items.length;

        selected = items[index];
        view.render(selected);
    }

    private void select() {
        if (selected == BattleTypeItem.BACK) {
            onBack.run();
        } else {
            onSelect.accept(selected);
        }
    }

    public BattleTypeItem getSelected() {
        return selected;
    }
}