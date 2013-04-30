package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class NewPackageAction extends AppAction {

    private static final long                              serialVersionUID = -8544759375428602013L;

    private final SelectionInfo<FilePackage, DownloadLink> si;

    public NewPackageAction(SelectionInfo<FilePackage, DownloadLink> si) {
        this.si = si;
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

}