package org.jdownloader.gui.components;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JComponent;

import org.jdownloader.gui.views.components.HeaderScrollPane;

public class OverviewHeaderScrollPane extends HeaderScrollPane {

    public OverviewHeaderScrollPane(JComponent overView) {
        super(overView);

    }

    // @Override
    // protected int getHeaderHeight() {
    // return 20;
    // }

    public Dimension getPreferredSize() {
        Dimension pref = getViewport().getPreferredSize();
        Insets insets = getBorder().getBorderInsets(this);
        Insets viewPortBorder = getViewportBorder().getBorderInsets(getViewport());
        // no horizontal scrollbar
        // if we would check the hsb here, we would get bad initial height informations and flickering
        pref.height += insets.top + insets.bottom + getPrefHeaderHeight() + viewPortBorder.top + viewPortBorder.bottom;

        return pref;
    }

    public Dimension getMinimumSize() {
        Dimension pref = getPreferredSize();
        pref.width = 0;
        return pref;
    }
}
