package jd.plugins.hoster;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.swing.action.BasicAction;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.images.AbstractIcon;

@HostPlugin(revision = "$Revision: 34675 $", interfaceVersion = 3, names = { "LinkCrawlerRetry" }, urls = { "" })
public class LinkCrawlerRetry extends PluginForHost {
    public LinkCrawlerRetry(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public String getHost(DownloadLink link, Account account) {
        if (link != null) {
            return Browser.getHost(link.getPluginPatternMatcher());
        } else {
            return getHost();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.FALSE;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls != null) {
            for (final DownloadLink link : urls) {
                link.setAvailableStatus(AvailableStatus.FALSE);
            }
        }
        return true;
    }

    @Override
    public void extendLinkgrabberContextMenu(final JComponent parent, final PluginView<CrawledLink> pv, final Collection<PluginView<CrawledLink>> allPvs) {
        if (pv.size() > 0) {
            parent.add(new JMenuItem(new BasicAction() {
                {
                    setName(_GUI.T.AddLinksDialog_AddLinksDialog_());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_LINKGRABBER, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    LinkCollector.getInstance().getQueue().addAsynch(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            LinkCollector.getInstance().removeChildren(pv.getChildren());
                            final StringBuilder sb = new StringBuilder();
                            for (final CrawledLink crawledLink : pv.getChildren()) {
                                if (sb.length() > 0) {
                                    sb.append("\r\n");
                                }
                                sb.append(crawledLink.getURL());
                            }
                            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.ADD_LINKS_DIALOG.getLinkOriginDetails(), sb.toString()));
                            return null;
                        }
                    });
                }
            }));
        }
    }

    @Override
    public void extendDownloadsTableContextMenu(final JComponent parent, final PluginView<DownloadLink> pv, final Collection<PluginView<DownloadLink>> views) {
        if (pv.size() > 0) {
            parent.add(new JMenuItem(new BasicAction() {
                {
                    setName(_GUI.T.AddLinksDialog_AddLinksDialog_());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_LINKGRABBER, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    DownloadController.getInstance().getQueue().addAsynch(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            DownloadController.getInstance().removeChildren(pv.getChildren());
                            final StringBuilder sb = new StringBuilder();
                            for (final DownloadLink downloadLink : pv.getChildren()) {
                                if (sb.length() > 0) {
                                    sb.append("\r\n");
                                }
                                sb.append(downloadLink.getPluginPatternMatcher());
                            }
                            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.ADD_LINKS_DIALOG.getLinkOriginDetails(), sb.toString()));
                            return null;
                        }
                    });
                }
            }));
        }
    }

    @Override
    public boolean isPremiumEnabled() {
        return false;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
