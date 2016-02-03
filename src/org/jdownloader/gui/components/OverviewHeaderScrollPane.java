package org.jdownloader.gui.components;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollBar;

import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.updatev2.gui.LAFOptions;

public class OverviewHeaderScrollPane extends HeaderScrollPane {

    public OverviewHeaderScrollPane(JComponent overView) {
        super(overView);

        int[] b = LAFOptions.getInstance().getCfg().getPanelHeaderBorder();
        if (b == null || b.length != 4) {
            b = new int[] { 0, 0, 0, 0 };
        }
        if (b[0] <= 0 && b[1] <= 0 && b[2] <= 0 && b[3] <= 0) {
            return;
        }
        setBorder(BorderFactory.createMatteBorder(b[0], b[1], b[2], b[3], LAFOptions.getInstance().getColorForPanelHeaderLine()));
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
