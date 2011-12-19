package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.os.CrossSystem;
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
        CrossSystem.openFile(path);
    }

    @Override
    public boolean isEnabled() {
        if (pkg != null && (path = new File(pkg.getDownloadFolder())).exists() && path.isDirectory()) {
            if (CrossSystem.isOpenFileSupported()) return true;
        }
        return false;
    }
}
