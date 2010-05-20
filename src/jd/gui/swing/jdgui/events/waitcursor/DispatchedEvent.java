package jd.gui.swing.jdgui.events.waitcursor;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.MenuComponent;
import java.awt.MenuContainer;

import javax.swing.SwingUtilities;

/**
 * @author Based on <a
 *         href="http://www.javaspecialists.eu/archive/Issue075.html">The Java
 *         Specialists' Newsletter</a>
 */
public class DispatchedEvent {

    private static final Cursor WAIT_CURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    private final Object mutex = new Object();
    private final Object source;
    private Component parent;
    private Cursor lastCursor;

    public DispatchedEvent(Object source) {
        this.source = source;
    }

    public void setCursor() {
        synchronized (mutex) {
            parent = findVisibleParent();
            if (parent != null) {
                lastCursor = (parent.isCursorSet() ? parent.getCursor() : null);
                parent.setCursor(WAIT_CURSOR);
            }
        }
    }

    public boolean resetCursor() {
        synchronized (mutex) {
            if (parent != null) {
                parent.setCursor(lastCursor);
                parent = null;
                return true;
            }
            return false;
        }
    }

    private Component findVisibleParent() {
        Component result = null;
        if (source instanceof Component) {
            result = SwingUtilities.getRoot((Component) source);
        } else if (source instanceof MenuComponent) {
            MenuContainer mParent = ((MenuComponent) source).getParent();
            if (mParent instanceof Component) {
                result = SwingUtilities.getRoot((Component) mParent);
            }
        }
        if (result != null && result.isVisible()) {
            return result;
        } else {
            return null;
        }
    }

}
