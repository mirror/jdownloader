package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.pluginsinc.liveheader.CLRConverter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class ConvertCLRScriptAction extends CustomizableAppAction {

    public ConvertCLRScriptAction() {
        setName(_JDT._.convert_CLR_Reconnect_to_jdownloader());
        setIconKey(IconKey.ICON_RUN);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            String clr = Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, _GUI._.ConvertCLRAction_actionPerformed_title(), _GUI._.ConvertCLRAction_actionPerformed_msg(), null, null, _GUI._.lit_continue(), null);
            String[] lh = CLRConverter.createLiveHeader(clr);
            Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, _GUI._.ConvertCLRAction_actionPerformed_finished_title(lh[0]), _GUI._.ConvertCLRAction_actionPerformed_finished_msg(), lh[1], null, _GUI._.lit_continue(), null);

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }
}
