package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class OpenDownloadFolderAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = -1123905158192679571L;
    private CrawledPackage    pkg;
    private File              path             = null;

    public OpenDownloadFolderAction(AbstractNode editing) {

        this(editing instanceof CrawledPackage ? (CrawledPackage) editing : ((CrawledLink) editing).getParentNode());

    }

    public OpenDownloadFolderAction(CrawledPackage editing) {
        this(editing, null);
    }

    public OpenDownloadFolderAction(AbstractNode contextObject, ArrayList<AbstractNode> selection) {

        if (contextObject instanceof CrawledLink) {
            pkg = ((CrawledLink) contextObject).getParentNode();

        } else {
            pkg = (CrawledPackage) contextObject;
        }

        setName(_GUI._.OpenDownloadFolderAction_OpenDownloadFolderAction_());
        setIconKey("load");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        final ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.OpenDownloadFolderAction_actionPerformed_object_(pkg.getName()), _GUI._.OpenDownloadFolderAction_actionPerformed_save_(), null);
        if (CrossSystem.isOpenFileSupported()) {
            d.setLeftActions(new AppAction() {
                {
                    setName(_GUI._.OpenDownloadFolderAction_actionPerformed_button_());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(d.getSelection()[0] == null ? path : d.getSelection()[0]);
                }

            });
        }
        d.setPreSelection(path);
        d.setFileSelectionMode(FileChooserSelectionMode.DIRECTORIES_ONLY);

        try {
            Dialog.getInstance().showDialog(d);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

    @Override
    public boolean isEnabled() {
        if (pkg != null && (path = new File(pkg.getDownloadFolder())).exists() && path.isDirectory()) {
            if (CrossSystem.isOpenFileSupported()) return true;
        }
        return false;
    }
}
