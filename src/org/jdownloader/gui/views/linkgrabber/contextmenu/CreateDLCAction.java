package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class CreateDLCAction extends AppAction {

    private SelectionInfo<CrawledPackage, CrawledLink> si;

    public CreateDLCAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        setName(_GUI._.gui_table_contextmenu_dlc());
        setIconKey("dlc");
        this.si = si;
    }

    public void actionPerformed(ActionEvent e) {

        new DLCFactory().createDLCByCrawledLinks(si.getChildren());
    }

}
