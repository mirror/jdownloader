package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SetDownloadFolderAction extends AppAction {

    /**
     * 
     */
    private static final long         serialVersionUID = -6632019767606316873L;
    private ArrayList<CrawledPackage> selection        = null;
    private CrawledPackage            pkg              = null;

    public SetDownloadFolderAction(AbstractNode node, ArrayList<CrawledPackage> selection) {
        if (node != null && node instanceof CrawledPackage) {
            pkg = (CrawledPackage) node;
        }
        this.selection = selection;
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        setIconKey("save");
    }

    public SetDownloadFolderAction(AbstractNode node) {
        if (node != null && node instanceof CrawledPackage) {
            pkg = (CrawledPackage) node;
            this.selection = new ArrayList<CrawledPackage>();
            this.selection.add(pkg);
        }
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        setIconKey("save");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final File[] dest = Dialog.getInstance().showFileChooser("downloadFolderDialog", _GUI._.SetDownloadFolderAction_SetDownloadFolderAction_(), FileChooserSelectionMode.DIRECTORIES_ONLY, null, false, FileChooserType.SAVE_DIALOG, new File(pkg.getDownloadFolder()));
            if (!isDownloadFolderValid(dest[0])) return;
            IOEQ.add(new Runnable() {

                public void run() {
                    for (CrawledPackage pkg : selection) {
                        pkg.setDownloadFolder(dest[0].getAbsolutePath());
                    }
                    LinkCollector.getInstance().refreshData();
                }

            });
        } catch (DialogNoAnswerException e1) {
        }

    }

    @Override
    public boolean isEnabled() {
        return pkg != null && selection != null && selection.size() > 0;
    }

    /**
     * checks if the given file is valid as a downloadfolder, this means it must
     * be an existing folder or at least its parent folder must exist
     * 
     * @param file
     * @return
     */
    public static boolean isDownloadFolderValid(File file) {
        if (file == null || file.isFile()) return false;
        if (file.isDirectory()) return true;
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory() && parent.exists()) return true;
        return false;
    }

}
