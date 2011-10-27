package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RenamePackageAction extends AppAction {

    private CrawledPackage pkg;

    public RenamePackageAction(CrawledPackage pkg) {
        this.pkg = pkg;
        setName(_GUI._.RenamePackageAction_RenamePackageAction_());
    }

    public void actionPerformed(ActionEvent e) {
    }

}
