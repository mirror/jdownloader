package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.actions.ResetAction;

public class ResetPopupAction extends SelectionAppAction<CrawledPackage, CrawledLink> {
    /**
     * 
     */
    private static final long                          serialVersionUID = 841782078416257540L;
    private SelectionInfo<CrawledPackage, CrawledLink> selection;

    public ResetPopupAction(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        super(selection);
        setName(_GUI._.ResetPopupAction_ResetPopupAction_());
        setIconKey("reset");
        this.selection = selection;
    }

    public void actionPerformed(ActionEvent e) {
        new ResetAction(selection).actionPerformed(e);
    }

}
