package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;

public class CreateDLCAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> {

    public CreateDLCAction() {

        setName(_GUI._.gui_table_contextmenu_dlc());
        setIconKey("dlc");

    }

    public void actionPerformed(ActionEvent e) {

        new DLCFactory().createDLCByCrawledLinks(getSelection().getChildren());
    }

}
