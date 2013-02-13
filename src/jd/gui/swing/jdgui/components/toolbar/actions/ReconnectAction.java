package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.Reconnecter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.userio.NewUIO;
import org.jdownloader.images.NewTheme;

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
        try {
            NewUIO.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.gui_reconnect_confirm(), NewTheme.I().getIcon("reconnect", 32), _GUI._.lit_yes(), _GUI._.lit_no());
            new Thread(new Runnable() {
                public void run() {
                    Reconnecter.getInstance().forceReconnect();
                }
            }).start();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
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
