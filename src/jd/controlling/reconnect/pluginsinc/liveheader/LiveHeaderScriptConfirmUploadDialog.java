package jd.controlling.reconnect.pluginsinc.liveheader;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class LiveHeaderScriptConfirmUploadDialog extends LiveHeaderScriptConfirmDialog {

    public LiveHeaderScriptConfirmUploadDialog(RouterData test, String gatewayAdressHost, String name) {
        super(Dialog.STYLE_HIDE_ICON, T.T.script(test.getRouterName()), new AbstractIcon("reconnect", 32), _GUI.T.lit_continue(), null, test, gatewayAdressHost, getRouterName(name));
    }

    private static String getRouterName(String name) {
        if (StringUtils.isEmpty(name)) {
            return _GUI.T.unknown_router_name();
        }
        return name;
    }

    @Override
    public String getMessage() {
        return T.T.confirm_upload_script();
    }
    //
    // public void addEditAction() {
    // setLeftActions(new AppAction() {
    // {
    // setName(_GUI.T.LiveHeaderScriptConfirmUploadDialog_show_script());
    // }
    //
    // @Override
    // public void actionPerformed(ActionEvent e) {
    //
    // final InputDialog dialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL,
    // T.T.script(getRouterName(routerData.getRouterName())), T.T.script_check(), routerData.getScript(), null, _GUI.T.lit_close(), null) {
    //
    // @Override
    // public JComponent layoutDialogContent() {
    // JComponent ret = super.layoutDialogContent();
    // bigInput.setEditable(false);
    // return ret;
    // }
    //
    // @Override
    // public boolean isRemoteAPIEnabled() {
    // return super.isRemoteAPIEnabled();
    // }
    // };
    //
    // dialog.setPreferredSize(new Dimension(700, 400));
    // InputDialogInterface d = UIOManager.I().show(InputDialogInterface.class, dialog);
    //
    // }
    // });
    // }

}
