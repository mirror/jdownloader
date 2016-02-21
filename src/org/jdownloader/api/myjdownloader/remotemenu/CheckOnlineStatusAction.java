package org.jdownloader.api.myjdownloader.remotemenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction.LinkCheckProgress;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

import jd.controlling.TaskQueue;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

public class CheckOnlineStatusAction extends AbstractMyJDSelectionAction {
    @Override
    public String getID() {
        return "checkonline";
    }

    public CheckOnlineStatusAction() {
        setIconKey(IconKey.ICON_OK);
        setName(_GUI.T.gui_table_contextmenu_check());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final List<?> children = getSelection().getChildren();
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {

                final List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(children.size());
                final LinkCheckProgress linkCheckProgress = new LinkCheckProgress();
                for (Object l : children) {
                    if (l instanceof DownloadLink || l instanceof CrawledLink) {
                        checkableLinks.add(((CheckableLink) l));
                    }
                    if (l instanceof DownloadLink) {
                        final DownloadLink link = (DownloadLink) l;
                        link.addPluginProgress(linkCheckProgress);
                    }
                }
                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);

                linkChecker.setLinkCheckHandler(new LinkCheckerHandler<CheckableLink>() {

                    @Override
                    public void linkCheckDone(CheckableLink l) {
                        if (l instanceof DownloadLink) {
                            final DownloadLink link = (DownloadLink) l;
                            link.removePluginProgress(linkCheckProgress);
                        }
                    }
                });
                linkChecker.check(checkableLinks);
                return null;
            }
        });
        DownloadsTableModel.getInstance().setAvailableColumnVisible(true);
    }

}
