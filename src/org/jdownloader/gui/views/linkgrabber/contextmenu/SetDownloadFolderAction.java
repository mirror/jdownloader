package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SetDownloadFolderAction extends AppAction {

    private ArrayList<CrawledPackage> selection;

    public SetDownloadFolderAction(ArrayList<CrawledPackage> selection) {

        this.selection = selection;
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        setIconKey("save");
        setEnabled(selection.size() > 0);
    }

    public void actionPerformed(ActionEvent e) {
    }

}
