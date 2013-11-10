package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class AddContainerContextMenuAction extends CustomizableSelectionAppAction<CrawledPackage, CrawledLink> {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddContainerContextMenuAction() {
        super();
        addContextSetup(new TableContext(true, false));

        setName(_GUI._.AddLinksToLinkgrabberAction());
        setIconKey("add");
        setTooltipText(_GUI._.AddLinksAction_AddLinksAction_tt());

    }

    public void actionPerformed(ActionEvent e) {
        new AddLinksAction().actionPerformed(e);
    }

}