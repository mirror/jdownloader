package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class EditLinkOrPackageAction extends ContextMenuAction {

    private PackageLinkNode contextObject;

    public EditLinkOrPackageAction(DownloadsTable downloadsTable, PackageLinkNode contextObject) {
        super();

        this.contextObject = contextObject;
        this.setEnabled(contextObject != null);
        init();
    }

    public void actionPerformed(ActionEvent e) {

        if (contextObject instanceof FilePackage) {
            showFilePackageDialog((FilePackage) contextObject);
        } else {
            showDownloadLinkDialog((DownloadLink) contextObject);
        }
    }

    private void showDownloadLinkDialog(DownloadLink link) {
        Dialog.getInstance().showMessageDialog(link + "");
    }

    private void showFilePackageDialog(FilePackage fp) {
        Dialog.getInstance().showMessageDialog(fp + "");
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_prop();
    }

    @Override
    protected String getIcon() {
        return "info";
    }

}
