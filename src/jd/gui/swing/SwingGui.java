//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jd.JDInitFlags;
import jd.controlling.JDLogger;
import jd.event.ControlListener;
import jd.gui.UserIF;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;

public abstract class SwingGui extends UserIF implements ControlListener, WindowListener, WindowStateListener, WindowFocusListener {
    protected JFrame mainFrame;

    /**
     * Invoked when a window has been opened.
     */
    public void windowOpened(final WindowEvent e) {
    }

    /**
     * Invoked when a window is in the process of being closed. The close
     * operation can be overridden at this point.
     */
    public void windowClosing(final WindowEvent e) {
    }

    /**
     * Invoked when a window has been closed.
     */
    public void windowClosed(final WindowEvent e) {
    }

    /**
     * Invoked when a window is iconified.
     */
    public void windowIconified(final WindowEvent e) {
    }

    /**
     * Invoked when a window is de-iconified.
     */
    public void windowDeiconified(final WindowEvent e) {
    }

    /**
     * Invoked when a window is activated.
     */
    public void windowActivated(final WindowEvent e) {
    }

    /**
     * Invoked when a window is de-activated.
     */
    public void windowDeactivated(final WindowEvent e) {
    }

    /**
     * Invoked when a window state is changed.
     */
    public void windowStateChanged(final WindowEvent e) {
    }

    /**
     * Invoked when the Window is set to be the focused Window, which means that
     * the Window, or one of its subcomponents, will receive keyboard events.
     * 
     */
    public void windowGainedFocus(final WindowEvent e) {
    }

    /**
     * Invoked when the Window is no longer the focused Window, which means that
     * keyboard events will no longer be delivered to the Window or any of its
     * subcomponents.
     * 
     */
    public void windowLostFocus(final WindowEvent e) {
    }

    private static final long serialVersionUID = 7164420260634468080L;

    private static SwingGui INSTANCE = null;

    public SwingGui(final String string) {
        mainFrame = new JFrame(string);
    }

    /**
     * Returns the gui's mainjframe
     * 
     * @return
     */
    public JFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Has to return the current used gui
     * 
     * @return
     */
    public static SwingGui getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the currently used GUI. IS not! thouight to be used to change gui at
     * runtime
     * 
     * @param ins
     */
    public static void setInstance(final SwingGui ins) {
        INSTANCE = ins;
    }

    abstract public void setWaiting(boolean b);

    abstract public void closeWindow();

    abstract public void setContent(SwitchPanel tabbedPanel);

    /**
     * Throws an RuntimeException if the current thread is not the edt
     */
    public static boolean checkEDT() {
        if (!JDInitFlags.SWITCH_DEBUG) return true;
        final Thread th = Thread.currentThread();

        if (!SwingUtilities.isEventDispatchThread()) {
            JDLogger.exception(new RuntimeException("EDT Violation! Runs in " + th));
            return false;
        }
        return true;
    }

    /**
     * remove a panel completly.. e.g. unloading an plugin.
     * 
     * @param view
     */
    abstract public void disposeView(SwitchPanel view);

}
