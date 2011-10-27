package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class EditFilenameAction extends AppAction {

    private CrawledLink link;

    public EditFilenameAction(CrawledLink link) {
        this.link = link;
        setName(_GUI._.EditFilenameAction_EditFilenameAction_());
        setIconKey("edit");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
