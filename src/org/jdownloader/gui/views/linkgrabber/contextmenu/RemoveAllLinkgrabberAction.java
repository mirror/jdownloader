package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.actions.ClearLinkgrabberAction;

public class RemoveAllLinkgrabberAction extends SelectionAppAction<CrawledPackage, CrawledLink> {
    /**
     * 
     */
    private static final long serialVersionUID = 841782078416257540L;

    public RemoveAllLinkgrabberAction(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        super(selection);
        setName(_GUI._.RemoveAllLinkgrabberAction_RemoveAllLinkgrabberAction_object_());
        setIconKey("clear");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        new ClearLinkgrabberAction().actionPerformed(e);
    }

    @Override
    public boolean isEnabled() {
        return LinkCollector.getInstance().size() > 0 && super.isEnabled();
    }

}
