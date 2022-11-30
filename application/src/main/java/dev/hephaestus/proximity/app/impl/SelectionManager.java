package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import javafx.scene.Parent;
import javafx.scene.layout.Background;

import java.util.*;

public final class SelectionManager {
    private final Set<DataWidget.Entry<?>> selected = new LinkedHashSet<>(1);

    private DataRow<?> row;

    public boolean isSelected(DataWidget.Entry<?> entry) {
        return this.selected.contains(entry);
    }

    public int selectionCount() {
        return this.selected.size();
    }

    public void select(List<DataWidget.Entry<?>> entries) {
        /* TODO if (!app.isShiftDown()) */
        for (Iterator<DataWidget.Entry<?>> itr = this.selected.iterator(); itr.hasNext(); ) {
            DataWidget.Entry<?> entry = itr.next();

            if (entry.getWidget().size() == 1) {
                (this.getRow(entry)).setBackground(Background.EMPTY);
            } else {
                entry.getRootPane().setBackground(Background.EMPTY);
            }

            itr.remove();
        }

        if (this.row != null) {
            this.row.setBackground(Background.EMPTY);
            this.row = null;
        }

        for (DataWidget.Entry<?> entry : entries) {
            if (!this.selected.contains(entry)) {
                if (entry.getWidget().size() == 1) {
                    (this.getRow(entry)).setBackground(Appearance.SELECTED_ROW);
                } else {
                    entry.getRootPane().setBackground(Appearance.SELECTED_ROW);
                }

                this.selected.add(entry);
            }
        }

        if (entries.size() > 0) {
            // Render the last entry selected
            Proximity.render(entries.get(entries.size() - 1));
        }
    }

    public void select(DataWidget.Entry<?>... entries) {
        this.select(Arrays.asList(entries));
    }

    public void select(DataRow<?> row) {
        /* TODO if (!app.isShiftDown()) */
        for (Iterator<DataWidget.Entry<?>> itr = this.selected.iterator(); itr.hasNext(); ) {
            DataWidget.Entry<?> entry = itr.next();

            if (entry.getWidget().size() == 1) {
                (this.getRow(entry)).setBackground(Background.EMPTY);
            } else {
                entry.getRootPane().setBackground(Background.EMPTY);
            }

            itr.remove();
        }

        if (this.row != null) {
            this.row.setBackground(Background.EMPTY);
            this.row = null;
        }

        row.setBackground(Appearance.SELECTED_ROW);
        this.row = row;
        Proximity.clearPreview();
    }

    private DataRow<?> getRow(DataWidget.Entry<?> entry) {
        Parent node = entry.getRootPane();

        while (node != null && !(node instanceof DataRow<?>)) {
            node = node.getParent();
        }

        return (DataRow<?>) node;
    }
}
