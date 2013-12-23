package jd.controlling.reconnect;

import jd.controlling.reconnect.pluginsinc.liveheader.CLRConverter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.advanced.AdvancedAction;
import org.jdownloader.translate._JDT;

public class ConvertCLRAction implements AdvancedAction {

    @Override
    public String getName() {
        return _JDT._.convert_CLR_Reconnect_to_jdownloader();
    }

    @Override
    public void actionPerformed() {
        System.out.println(1);

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
