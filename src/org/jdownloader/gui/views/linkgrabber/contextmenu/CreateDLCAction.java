package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class CreateDLCAction extends SelectionAppAction<CrawledPackage, CrawledLink> {

    public CreateDLCAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.gui_table_contextmenu_dlc());
        setIconKey("dlc");

    }

    public void actionPerformed(ActionEvent e) {

        new DLCFactory().createDLCByCrawledLinks(getSelection().getChildren());
    }

}
