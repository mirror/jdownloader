package org.jdownloader.gui.views.components;

import javax.swing.Icon;

import org.appwork.swing.components.ExtMergedIcon;

public class MergedIcon extends ExtMergedIcon {

    private int gap = 5;

    public MergedIcon(Icon... icons) {
        super();
        int height = 0;
        int width = 0;
        for (Icon i : icons) {
            height = Math.max(height, i.getIconHeight());
            if (width > 0) width += gap;
            width += i.getIconWidth();
        }
        int x = 0;
        int i = 0;
        for (Icon c : icons) {
            add(c, x, (height - c.getIconHeight()) / 2, i++, null);
            x += c.getIconWidth() + gap;
        }

    }

}
