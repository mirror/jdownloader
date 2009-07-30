package jd.gui.swing;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

import javax.swing.JFrame;

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
    public void windowOpened(WindowEvent e) {
    }

    /**
     * Invoked when a window is in the process of being closed. The close
     * operation can be overridden at this point.
     */
    public void windowClosing(WindowEvent e) {
    }

    /**
     * Invoked when a window has been closed.
     */
    public void windowClosed(WindowEvent e) {
    }

    /**
     * Invoked when a window is iconified.
     */
    public void windowIconified(WindowEvent e) {
    }

    /**
     * Invoked when a window is de-iconified.
     */
    public void windowDeiconified(WindowEvent e) {
    }

    /**
     * Invoked when a window is activated.
     */
    public void windowActivated(WindowEvent e) {
    }

    /**
     * Invoked when a window is de-activated.
     */
    public void windowDeactivated(WindowEvent e) {
    }

    /**
     * Invoked when a window state is changed.
     */
    public void windowStateChanged(WindowEvent e) {
    }

    /**
     * Invoked when the Window is set to be the focused Window, which means that
     * the Window, or one of its subcomponents, will receive keyboard events.
     * 
     */
    public void windowGainedFocus(WindowEvent e) {
    }

    /**
     * Invoked when the Window is no longer the focused Window, which means that
     * keyboard events will no longer be delivered to the Window or any of its
     * subcomponents.
     * 
     */
    public void windowLostFocus(WindowEvent e) {
    }

    private static final long serialVersionUID = 7164420260634468080L;

    private static SwingGui INSTANCE = null;

    public SwingGui(String string) {

        mainFrame = new JFrame(string);

    }
/**
 * Returns the gui's mainjframe
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
    public static void setInstance(SwingGui ins) {
        INSTANCE = ins;
    }

    // TODO
    abstract public void setWaiting(boolean b);

    abstract public void closeWindow();

    abstract public void setContent(SwitchPanel tabbedPanel);

    /**
     * Throws an RuntimeException if the current thread is not the edt
     */
    public static void checkEDT() {
        if (!JDInitFlags.SWITCH_DEBUG) return;
        Thread th = Thread.currentThread();
        String name = th.toString();
        if (!name.contains("EventQueue")) {
            JDLogger.exception(new RuntimeException("EDT Violation! Runs in " + th));
        }

    }

    /**
     * remove a panel completly.. e.g. unloading an plugin.
     * 
     * @param view
     */
    abstract public void disposeView(SwitchPanel view);

}
