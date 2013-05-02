package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class LGMenuManagerAction extends AppAction {
    private SelectionInfo<CrawledPackage, CrawledLink> si;

    public LGMenuManagerAction(SelectionInfo<CrawledPackage, CrawledLink> si) {

        this.si = si;
        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LinkgrabberContextMenuManager.getInstance().openGui();

    }
}
