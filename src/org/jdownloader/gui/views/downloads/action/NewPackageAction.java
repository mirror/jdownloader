package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.contextmenu.NewPackageDialog;

public class NewPackageAction extends SelectionAppAction<FilePackage, DownloadLink> implements CachableInterface {

    private static final long serialVersionUID = -8544759375428602013L;

    public NewPackageAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setIconKey("package_new");
        setName(_GUI._.gui_table_contextmenu_newpackage());
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {

            final NewPackageDialog d = new NewPackageDialog(getSelection());
            Dialog.getInstance().showDialog(d);
            final String name = d.getName();

            if (name == null | name.trim().length() == 0) return;

            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    FilePackage newPackage = FilePackage.getInstance();
                    newPackage.setName(name);
                    String f = d.getDownloadFolder();
                    newPackage.setDownloadDirectory(f);
                    //

                    DownloadController.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0, true);

                    return null;
                }

            });
        } catch (DialogNoAnswerException e1) {
        }

    }

    @Override
    public void setData(String data) {
    }

}