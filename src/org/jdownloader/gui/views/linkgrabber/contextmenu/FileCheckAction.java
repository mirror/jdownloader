package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;

public class FileCheckAction extends AppAction {

    private ArrayList<AbstractNode> selection;

    public FileCheckAction(ArrayList<AbstractNode> selection) {

        this.selection = selection;
        setName(_GUI._.FileCheckAction_FileCheckAction_());
        setIconKey("ok");
    }

    public void actionPerformed(ActionEvent e) {
        LinkChecker<CrawledLink> linkChecker = new LinkChecker<CrawledLink>();
        ArrayList<CrawledLink> links = LinkTreeUtils.getSelectedChildren(selection, new ArrayList<CrawledLink>());
        for (CrawledLink l : links) {
            l.getDownloadLink().setAvailableStatus(AvailableStatus.UNCHECKED);
        }
        LinkGrabberTableModel.getInstance().refreshModel(false);
        linkChecker.setLinkCheckHandler(new LinkCheckerHandler<CrawledLink>() {

            public void linkCheckDone(CrawledLink link) {
                LinkGrabberTableModel.getInstance().refreshModel(true);
            }
        });

        linkChecker.check(links);
    }

}
