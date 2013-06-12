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

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.Timer;

import jd.gui.UserIF;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.app.gui.ActiveDialogException;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class SwingGui extends UserIF implements WindowListener, WindowStateListener, WindowFocusListener {
    protected JFrame mainFrame;

    /**
     * Invoked when a window has been opened.
     */
    public void windowOpened(final WindowEvent e) {
    }

    /**
     * Invoked when a window is in the process of being closed. The close operation can be overridden at this point.
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
     * Invoked when the Window is set to be the focused Window, which means that the Window, or one of its subcomponents, will receive
     * keyboard events.
     * 
     */
    public void windowGainedFocus(final WindowEvent e) {
    }

    /**
     * Invoked when the Window is no longer the focused Window, which means that keyboard events will no longer be delivered to the Window
     * or any of its subcomponents.
     * 
     */
    public void windowLostFocus(final WindowEvent e) {
    }

    private static SwingGui INSTANCE = null;

    public SwingGui(final String string) {
        mainFrame = new JFrame(string) {
            public void dispose() {

                super.dispose();
            }

            /**
			 * 
			 */
            private static final long serialVersionUID = -4218493713632551975L;

            public void toFront() {

                if (!isVisible()) return;
                super.toFront();
                setAlwaysOnTop(true);
                Timer disableAlwaysonTop = new Timer(1000, new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        setAlwaysOnTop(false);
                    }
                });
                disableAlwaysonTop.setInitialDelay(1000);
                disableAlwaysonTop.setRepeats(false);
                disableAlwaysonTop.start();
                repaint();
            }

            public void setVisible(boolean b) {
                if (b && !isVisible()) {
                    if (CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled() && !StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue())) {
                        String password;
                        try {
                            password = Dialog.getInstance().showInputDialog(Dialog.STYLE_PASSWORD, _GUI._.SwingGui_setVisible_password_(), _GUI._.SwingGui_setVisible_password_msg(), null, NewTheme.I().getIcon("lock", 32), null, null);
                            String internPw = CFG_GUI.PASSWORD.getValue();
                            if (!internPw.equals(password)) {

                                Dialog.getInstance().showMessageDialog(_GUI._.SwingGui_setVisible_password_wrong());
                                return;
                            }
                        } catch (DialogNoAnswerException e) {
                            return;
                        }
                    }
                }
                // if we hide a frame which is locked by an active modal dialog,
                // we get in problems. avoid this!
                if (!b) {
                    for (Window w : getOwnedWindows()) {

                        if (w instanceof JDialog) {
                            boolean mod = ((JDialog) w).isModal();
                            boolean v = w.isVisible();

                            if (mod && v) {
                                Toolkit.getDefaultToolkit().beep();
                                Log.exception(new ActiveDialogException(((JDialog) w)));
                                w.requestFocus();
                                w.toFront();
                                return;
                            }
                        }

                    }
                }

                super.setVisible(b);
            }
        };
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
     * Sets the currently used GUI. IS not! thouight to be used to change gui at runtime
     * 
     * @param ins
     */
    public static void setInstance(final SwingGui ins) {
        INSTANCE = ins;
    }

    abstract public void setWaiting(boolean b);

    public void closeWindow() {
    }

    abstract public void setContent(View view, boolean setActive);

    /**
     * remove a panel completly.. e.g. unloading an plugin.
     * 
     * @param view
     */
    abstract public void disposeView(View view);

}
