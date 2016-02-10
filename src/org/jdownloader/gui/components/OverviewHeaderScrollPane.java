package org.jdownloader.gui.components;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.border.Border;

import org.jdownloader.gui.views.components.HeaderScrollPane;

public class OverviewHeaderScrollPane extends HeaderScrollPane {

    public OverviewHeaderScrollPane(JComponent overView) {
        super(overView);
    }

    public Dimension getPreferredSize() {
        final Dimension pref = getViewport().getPreferredSize();
        final Border border = getBorder();
        if (border != null) {
            final Insets insets = border.getBorderInsets(this);
            // no horizontal scrollbar
            // if we would check the hsb here, we would get bad initial height informations and flickering
            final Border viewBorder = getViewportBorder();
            if (viewBorder != null) {
                final Insets viewPortBorder = viewBorder.getBorderInsets(getViewport());
                pref.height += insets.top + insets.bottom + getPrefHeaderHeight() + viewPortBorder.top + viewPortBorder.bottom;
            } else {
                pref.height += insets.top + insets.bottom + getPrefHeaderHeight();
            }
        }
        return pref;
    }

    public Dimension getMinimumSize() {
        final Dimension pref = getPreferredSize();
        pref.width = 0;
        return pref;
    }
}
