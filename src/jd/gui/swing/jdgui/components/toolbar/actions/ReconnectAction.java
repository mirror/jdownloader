package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.Reconnecter;
import jd.gui.UserIO;
import jd.nutils.JDFlags;

import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;

public class ReconnectAction extends AbstractToolbarAction {
    private static final ReconnectAction INSTANCE = new ReconnectAction();

    /**
     * get the only existing instance of ReconnectAction. This is a singleton
     * 
     * @return
     */
    public static ReconnectAction getInstance() {
        return ReconnectAction.INSTANCE;
    }

    /**
     * Create a new instance of ReconnectAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ReconnectAction() {

    }

    public void actionPerformed(ActionEvent e) {
        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, _GUI._.gui_reconnect_confirm()), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
            /* forceReconnect is running in its own thread */
            new Thread(new Runnable() {
                public void run() {
                    Reconnecter.getInstance().forceReconnect();
                }
            }).start();
        }
    }

    @Override
    public String createIconKey() {
        return "reconnect";
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getDoReconnectAction();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_reconnect_invoke_tooltip();
    }

    @Override
    protected void doInit() {
    }

}
