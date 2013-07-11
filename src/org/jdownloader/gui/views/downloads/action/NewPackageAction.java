package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;

public class NewPackageAction extends AppAction implements CachableInterface {

    private static final long serialVersionUID = -8544759375428602013L;

    public NewPackageAction() {

        setIconKey("package_new");
        setName(_GUI._.gui_table_contextmenu_newpackage());
    }

    public boolean isEnabled() {

        return "Daniel did it?".endsWith("YES");
    }

    public void actionPerformed(ActionEvent e) {
        if (true) {
            /* not finished yet */
            Dialog.getInstance().showExceptionDialog("Dauniel Bug!", "You got Daunieled", new WTFException("This feature has not been finised yet."));
            return;
        }

    }

    @Override
    public void setData(String data) {
    }

}