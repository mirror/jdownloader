package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteAction extends AppAction {

    private static final long                        serialVersionUID = -5721724901676405104L;

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setIconKey("delete");
        setName(_GUI._.gui_table_contextmenu_deletelist2());

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        if (si.isShiftDown() || UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_downloadlist_delete() + " (" + _GUI._.gui_downloadlist_delete_size_packagev2(si.getSelectedChildren().size()) + ")"))) {
            for (DownloadLink link : si.getSelectedChildren()) {
                link.setEnabled(false);
                link.deleteFile(true, false);
                link.getFilePackage().remove(link);
            }
        }
    }

    @Override
    public boolean isEnabled() {

        return !si.isEmpty();
    }

}