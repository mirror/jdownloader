package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
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
    private ArrayList<AbstractNode>   selection        = null;
    private CrawledPackage            pkg              = null;
    private boolean                   retOkay          = false;
    private ArrayList<CrawledLink>    links;
    private ArrayList<CrawledPackage> packages;

    public boolean newValueSet() {
        return retOkay;
    }

    public SetDownloadFolderAction(AbstractNode node, ArrayList<AbstractNode> selectio2) {
        if (selectio2 == null) {
            selectio2 = new ArrayList<AbstractNode>();
            selectio2.add(node);
        }
        if (node != null && node instanceof CrawledPackage) {
            pkg = (CrawledPackage) node;
            this.selection = new ArrayList<AbstractNode>();
            this.selection.add(pkg);
            packages = new ArrayList<CrawledPackage>();
            packages.add(pkg);
        }
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        setIconKey("save");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        try {
            final File[] dest = Dialog.getInstance().showFileChooser("downloadFolderDialog", _GUI._.SetDownloadFolderAction_SetDownloadFolderAction_(), FileChooserSelectionMode.DIRECTORIES_ONLY, null, false, FileChooserType.SAVE_DIALOG, new File(pkg.getDownloadFolder()));
            if (!isDownloadFolderValid(dest[0])) return;
            retOkay = true;
            IOEQ.add(new Runnable() {

                public void run() {
                    for (CrawledPackage pkg : packages) {
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

        if (links != null && links.size() > 0) {
            // if user selected links and packages, all links must have the same
            // package as parent. We cannot edit different linsk from differenbt
            // packages
            if (packages != null && packages.size() > 1) return false;

        }
        return packages != null && packages.size() > 0;
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
