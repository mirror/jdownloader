package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.AddLinksDialog;
import org.jdownloader.images.NewTheme;

public class AddLinksAction extends AppAction {
    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("add", 16));
        putValue(NAME, _GUI._.AddLinksAction_());

    }

    public void actionPerformed(ActionEvent e) {

        AddLinksDialog dialog = new AddLinksDialog();

        try {
            Dialog.getInstance().showDialog(dialog);

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

}
