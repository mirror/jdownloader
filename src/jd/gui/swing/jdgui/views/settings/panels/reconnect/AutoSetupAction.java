package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.controlling.reconnect.ReconnectPluginController;

import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class AutoSetupAction extends AbstractAction {
    {
        putValue(NAME, _JDT._.reconnectmanager_wizard());
        putValue(SMALL_ICON, NewTheme.I().getIcon("wizard", 20));

    }

    public void actionPerformed(ActionEvent e) {

        try {
            ReconnectPluginController.getInstance().autoFind(new ProcessCallBack() {

                public void showDialog(Object caller, String title, String message, ImageIcon icon) {
                    try {
                        Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, title, message, icon, null, null);
                    } catch (DialogClosedException e) {
                        e.printStackTrace();
                    } catch (DialogCanceledException e) {
                        e.printStackTrace();
                    }
                }

                public void setStatusString(Object caller, String string) {
                    System.out.println(caller + " : " + string);
                }

                public void setProgress(Object caller, int percent) {
                }
            });
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

}
