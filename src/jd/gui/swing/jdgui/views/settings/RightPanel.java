package jd.gui.swing.jdgui.views.settings;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Scrollable;

import org.appwork.swing.MigPanel;

public class RightPanel extends MigPanel implements Scrollable {

    public RightPanel() {
        super("ins 0", "[grow,fill]", "[]");
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        final Container viewport = getParent();
        return viewport.getHeight() > getMinimumSize().height;
    }

}
