package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;

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

    public OpenDownloadFolderAction(AbstractNode contextObject) {
        if (contextObject != null && contextObject instanceof CrawledPackage) {
            this.pkg = (CrawledPackage) contextObject;
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
