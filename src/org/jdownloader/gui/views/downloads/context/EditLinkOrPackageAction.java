package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

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
        // Dialog.getInstance().showDialog(new LinkPropertiesDialog(link));
    }

    private void showFilePackageDialog(FilePackage fp) {
        // try {
        // Dialog.getInstance().showDialog(new PackagePropertiesDialog(fp));
        // } catch (DialogClosedException e) {
        // e.printStackTrace();
        // } catch (DialogCanceledException e) {
        // e.printStackTrace();
        // }
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
