package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class CheckStatusAction extends AppAction {

    private static final long       serialVersionUID = 6821943398259956694L;
    private ArrayList<AbstractNode> links;

    public CheckStatusAction(ArrayList<AbstractNode> links) {
        setIconKey("ok");
        setName(_GUI._.gui_table_contextmenu_check());
        this.links = links;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                ArrayList<AbstractPackageChildrenNode> children = LinkTreeUtils.getSelectedChildren(links, new ArrayList<AbstractPackageChildrenNode>());
                ArrayList<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(children.size());
                for (AbstractPackageChildrenNode l : children) {
                    if (l instanceof DownloadLink) {
                        ((DownloadLink) l).setAvailableStatus(AvailableStatus.UNCHECKED);
                        checkableLinks.add(((DownloadLink) l));
                    } else if (l instanceof CrawledLink) {
                        ((CrawledLink) l).getDownloadLink().setAvailableStatus(AvailableStatus.UNCHECKED);
                        checkableLinks.add(((CrawledLink) l));
                    }
                }
                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                linkChecker.setLinkCheckHandler(new LinkCheckerHandler<CheckableLink>() {

                    @Override
                    public void linkCheckDone(CheckableLink link) {
                        if (link instanceof CrawledLink) {
                            System.out.println("refresh linkgrabber");
                            LinkGrabberTableModel.getInstance().refreshModel(true);
                        } else if (link instanceof DownloadLink) {
                            System.out.println("refresh downloadtable");
                            DownloadsTableModel.getInstance().refreshModel(true);
                        }
                    }
                });
                linkChecker.check(checkableLinks);
            }

        }, true);
    }

    @Override
    public boolean isEnabled() {
        return links != null && links.size() > 0;
    }

}