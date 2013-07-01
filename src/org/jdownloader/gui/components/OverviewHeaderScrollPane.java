package org.jdownloader.gui.components;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JScrollBar;

import org.jdownloader.gui.views.components.HeaderScrollPane;

public class OverviewHeaderScrollPane extends HeaderScrollPane {

    public OverviewHeaderScrollPane(JComponent overView) {
        super(overView);
    }

    public Dimension getPreferredSize() {
        Dimension pref = getViewport().getPreferredSize();
        Insets insets = getBorder().getBorderInsets(this);
        JScrollBar hb = getHorizontalScrollBar();
        pref.height += insets.top + insets.bottom + getHeaderHeight() + (hb.isVisible() ? hb.getPreferredSize().getHeight() : 0);

        return pref;
    }

    public Dimension getMinimumSize() {
        Dimension pref = getPreferredSize();
        pref.width = 0;
        return pref;
    }
}
