package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.LinkPropertiesDialog;
import org.jdownloader.gui.views.downloads.propertydialogs.PackagePropertiesDialog;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.images.NewTheme;

public class EditLinkOrPackageAction extends ContextMenuAction {

    private AbstractNode contextObject;

    public EditLinkOrPackageAction(DownloadsTable downloadsTable, AbstractNode contextObject) {
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
        LinkPropertiesDialog d = new LinkPropertiesDialog(link);
        try {
            Dialog.getInstance().showDialog(d);
            link.forceFileName(d.getLinkName());
            link.setDownloadPassword(d.getDownloadPassword());
            link.setMD5Hash(d.getMd5());
            link.setSha1Hash(d.getSha1());

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    private void showFilePackageDialog(FilePackage fp) {
        try {
            PackagePropertiesDialog d = new PackagePropertiesDialog(fp);
            Dialog.getInstance().showDialog(d);

            fp.setPasswordList(d.getPasswordList());
            fp.setName(d.getPackageName());
            fp.setComment(d.getComment());
            if (!new File(d.getDownloadDirectory()).exists()) {
                try {
                    Dialog.getInstance().showConfirmDialog(0, _GUI._.EditLinkOrPackageAction_showFilePackageDialog_downloaddir_doesnotexist_(), _GUI._.EditLinkOrPackageAction_showFilePackageDialog_downloaddir_doesnotexist_msg(d.getDownloadDirectory()), NewTheme.I().getIcon("open", 32), _GUI._.EditLinkOrPackageAction_showFilePackageDialog_create(), null);
                    fp.setDownloadDirectory(d.getDownloadDirectory());
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
            } else {
                fp.setDownloadDirectory(d.getDownloadDirectory());
            }
            fp.setPostProcessing(d.isExtractEnabled());
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
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
