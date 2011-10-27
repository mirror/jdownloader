package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class OpenUrlAction extends AppAction {

    public OpenUrlAction(CrawledLink link) {
        setName(_GUI._.OpenUrlAction_OpenUrlAction_());
        setIconKey("browse");
        toContextMenuAction();
    }

    public void actionPerformed(ActionEvent e) {
    }

}
