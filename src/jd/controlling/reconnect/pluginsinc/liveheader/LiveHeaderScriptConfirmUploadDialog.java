package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class LiveHeaderScriptConfirmUploadDialog extends LiveHeaderScriptConfirmDialog {

    public LiveHeaderScriptConfirmUploadDialog(RouterData test, String gatewayAdressHost, String name) {
        super(Dialog.STYLE_HIDE_ICON, T._.script(test.getRouterName()), new AbstractIcon("reconnect", 32), _GUI._.lit_continue(), null, test, gatewayAdressHost, name);
    }

    @Override
    public String getMessage() {
        return T._.confirm_upload_script();
    }

    public void addEditAction() {
        setLeftActions(new AppAction() {
            {
                setName(_GUI._.LiveHeaderScriptConfirmUploadDialog_show_script());
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                final InputDialog dialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL, T._.script(routerData.getRouterName()), T._.script_check(), routerData.getScript(), null, _GUI._.lit_close(), null) {

                    @Override
                    public JComponent layoutDialogContent() {
                        JComponent ret = super.layoutDialogContent();
                        bigInput.setEditable(false);
                        return ret;
                    }

                    @Override
                    public boolean isRemoteAPIEnabled() {
                        return super.isRemoteAPIEnabled();
                    }
                };

                dialog.setPreferredSize(new Dimension(700, 400));
                InputDialogInterface d = UIOManager.I().show(InputDialogInterface.class, dialog);

            }
        });
    }

}
