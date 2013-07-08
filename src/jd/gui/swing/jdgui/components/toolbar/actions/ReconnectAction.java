package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectDialog;

import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ReconnectAction extends ToolBarAction {

    public ReconnectAction(SelectionInfo<?, ?> selection) {

        setIconKey("reconnect");
        setAccelerator(KeyEvent.VK_R);

    }

    public void actionPerformed(ActionEvent e) {

        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.gui_reconnect_confirm(), NewTheme.I().getIcon("reconnect", 32), _GUI._.lit_yes(), _GUI._.lit_no())) {

            try {
                Dialog.getInstance().showDialog(new ReconnectDialog() {
                    protected boolean startReconnectAndWait(LogSource logger) throws ReconnectException, InterruptedException {
                        return Reconnecter.getInstance().forceReconnect();
                    }
                });
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
            //
            // new Thread(new Runnable() {
            // public void run() {
            // Reconnecter.getInstance().forceReconnect();
            // }
            // }).start();
        }

    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_reconnect_invoke_tooltip();
    }

}
