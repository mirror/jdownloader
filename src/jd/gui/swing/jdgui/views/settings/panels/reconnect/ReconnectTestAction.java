package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ReconnectTestAction extends AbstractAction {
    {
        putValue(NAME, _GUI._.ReconnectTestAction());
        putValue(SMALL_ICON, NewTheme.I().getIcon("test", 20));
    }

    public void actionPerformed(ActionEvent e) {

        try {
            Dialog.getInstance().showDialog(new ReconnectDialog());
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
