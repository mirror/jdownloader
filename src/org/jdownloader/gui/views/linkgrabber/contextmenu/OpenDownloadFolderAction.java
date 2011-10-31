package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class OpenDownloadFolderAction extends AppAction {

    private CrawledPackage pkg;

    public OpenDownloadFolderAction(AbstractNode contextObject) {

        if (contextObject instanceof CrawledPackage) {
            pkg = (CrawledPackage) contextObject;
        } else {
            setEnabled(false);
        }
        setName(_GUI._.OpenDownloadFolderAction_OpenDownloadFolderAction_());
        setIconKey("load");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
