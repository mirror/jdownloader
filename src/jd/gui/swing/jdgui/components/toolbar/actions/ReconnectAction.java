package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.Reconnecter;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ReconnectAction extends ToolBarAction {

    public ReconnectAction(SelectionInfo<?, ?> selection) {

        setIconKey("reconnect");

    }

    public void actionPerformed(ActionEvent e) {

        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.gui_reconnect_confirm(), NewTheme.I().getIcon("reconnect", 32), _GUI._.lit_yes(), _GUI._.lit_no())) {
            new Thread(new Runnable() {
                public void run() {
                    Reconnecter.getInstance().forceReconnect();
                }
            }).start();
        }

    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getDoReconnectAction();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_reconnect_invoke_tooltip();
    }

}
