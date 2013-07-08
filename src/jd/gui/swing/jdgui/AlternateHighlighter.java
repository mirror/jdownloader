package jd.gui.swing.jdgui;

import java.awt.Color;

import javax.swing.border.Border;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;

public class AlternateHighlighter<T> extends ExtComponentRowHighlighter<T> {
    public AlternateHighlighter(Color fg, Color bg, Border border) {
        super(fg, bg, border);
    }

    public int getPriority() {
        return 10;
    }

    @Override
    public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {
        return row % 2 != 0;
    }

}