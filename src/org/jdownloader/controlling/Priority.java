package org.jdownloader.controlling;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public enum Priority {
    // Don't forget to update org.jdownloader.myjdownloader.client.bindings.PriorityStorable
    HIGHEST(3, _GUI.T.gui_treetable_tooltip_priority3()),
    HIGHER(2, _GUI.T.gui_treetable_tooltip_priority2()),
    HIGH(1, _GUI.T.gui_treetable_tooltip_priority1()),
    DEFAULT(0, _GUI.T.gui_treetable_tooltip_priority0()),
    LOW(-1, _GUI.T.gui_treetable_tooltip_priority_1()),
    LOWER(-2, _GUI.T.gui_treetable_tooltip_priority_2()),
    LOWEST(-3, _GUI.T.gui_treetable_tooltip_priority_3());

    private final int id;

    public final int getId() {
        return id;
    }

    private final String translation;

    private Priority(int p, String translation) {
        id = p;
        this.translation = translation;
    }

    public final String T() {
        return translation;
    }

    public final String getIconKey() {
        switch (this) {
        case HIGHEST:
            return IconKey.ICON_PRIO_3;
        case HIGHER:
            return IconKey.ICON_PRIO_2;
        case HIGH:
            return IconKey.ICON_PRIO_1;
        case LOW:
            return IconKey.ICON_PRIO__1;
        case LOWER:
            return IconKey.ICON_PRIO__2;
        case LOWEST:
            return IconKey.ICON_PRIO__3;
        case DEFAULT:
        default:
            return IconKey.ICON_PRIO_0;
        }
    }

    public final Icon loadIcon(int size) {
        return new AbstractIcon(getIconKey(), size);
    }

    public static Priority getPriority(int p) {
        if (p > 3) {
            p = 3;
        }
        if (p < -3) {
            p = -3;
        }
        switch (p) {
        case 3:
            return HIGHEST;
        case 2:
            return HIGHER;
        case 1:
            return HIGH;
        case -1:
            return LOW;
        case -2:
            return LOWER;
        case -3:
            return LOWEST;
        case 0:
        default:
            return DEFAULT;
        }
    }
}
