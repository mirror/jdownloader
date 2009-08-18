package jd.gui.swing.components.JDTable;

import java.awt.Color;

abstract public class JDRowHighlighter {

    protected Color color;

    public JDRowHighlighter(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public abstract boolean doHighlight(Object obj);
}
