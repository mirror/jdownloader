package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class LGMenuManagerAction extends SelectionAppAction<CrawledPackage, CrawledLink> {

    public LGMenuManagerAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LinkgrabberContextMenuManager.getInstance().openGui();

    }
}
