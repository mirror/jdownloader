package org.jdownloader.extensions;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Scrollable;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;

public class VerticalScrollPanel extends MigPanel implements Scrollable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public VerticalScrollPanel(String constraints, String cols, String rows) {
        this(new MigLayout(constraints, cols, rows));
    }

    public VerticalScrollPanel(MigLayout migLayout) {
        super(migLayout);
    }

    public Dimension getPreferredScrollableViewportSize() {

        return this.getPreferredSize();
    }

    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return Math.max(visibleRect.height * 9 / 10, 1);
    }

    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return Math.max(visibleRect.height / 10, 1);
    }

    public boolean getScrollableTracksViewportHeight() {

        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

}
