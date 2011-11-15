package org.jdownloader.controlling;

import javax.swing.ImageIcon;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public enum Priority {
    LOWER(-1, _GUI._.gui_treetable_tooltip_priority_1()),
    DEFAULT(0, _GUI._.gui_treetable_tooltip_priority0()),
    HIGH(1, _GUI._.gui_treetable_tooltip_priority1()),
    HIGHER(2, _GUI._.gui_treetable_tooltip_priority2()),
    HIGHEST(3, _GUI._.gui_treetable_tooltip_priority3());

    private int id;

    public int getId() {
        return id;
    }

    private String translation;

    private Priority(int p, String translation) {
        id = p;
        this.translation = translation;
    }

    public String _() {
        return translation;
    }

    public ImageIcon loadIcon(int size) {
        return NewTheme.I().getIcon("prio_" + id, size);
    }
}
