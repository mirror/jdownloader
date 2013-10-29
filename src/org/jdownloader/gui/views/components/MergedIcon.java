package org.jdownloader.gui.views.components;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class MergedIcon implements Icon {

    private int    gap = 5;
    private Icon[] icons;
    private int    height;
    private int    width;

    public MergedIcon(Icon... icons) {
        this.icons = icons;
        height = 0;
        width = 0;
        for (Icon i : icons) {
            height = Math.max(height, i.getIconHeight());
            if (width > 0) width += gap;
            width += i.getIconWidth();
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        for (Icon i : icons) {
            g.translate(0, (height - i.getIconHeight()) / 2);
            i.paintIcon(c, g, x, y);
            g.translate(0, -(height - i.getIconHeight()) / 2);
            g.translate(i.getIconWidth() + gap, 0);

        }
        g.translate(-(width + gap), 0);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

}
