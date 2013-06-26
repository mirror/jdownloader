package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.actions.AppAction;

public class ShowInputDialogDebugAction extends AppAction {
    {
        setIconKey("dialog");
        setTooltipText(getClass().getName());
        setName(getTooltipText());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        new ConfirmDialog(0, "Confirm", "Message") {

            @Override
            public void actionPerformed(ActionEvent e) {
                new MessageDialogImpl(0, "Event " + e).show();
            }

        }.show();
        String text = new InputDialog(0, "DebugTitle", "Debug Message", "My Default Text").show().getText();
        new MessageDialogImpl(0, "Entered Text: " + text).show();
    }

}
