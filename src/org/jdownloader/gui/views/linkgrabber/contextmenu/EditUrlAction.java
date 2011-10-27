package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class EditUrlAction extends AppAction {

    private CrawledLink link;

    public EditUrlAction(CrawledLink link) {
        this.link = link;
        setName(_GUI._.EditUrlAction_EditUrlAction_());
        setIconKey("browse");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
