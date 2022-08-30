package jd.plugins.hoster;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
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
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "LinkCrawlerRetry" }, urls = { "" })
public class LinkCrawlerRetry extends PluginForHost {
    public LinkCrawlerRetry(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public String getHost(DownloadLink link, Account account, boolean includeSubdomain) {
        if (link != null) {
            return Browser.getHost(link.getPluginPatternMatcher(), includeSubdomain);
        } else {
            return getHost();
        }
    }

    @Override
    public boolean assignPlugin(DownloadLink link) {
        if (super.assignPlugin(link)) {
            link.setAvailableStatus(AvailableStatus.UNCHECKED);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.INTERNAL };
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) {
        final String reason = parameter.getStringProperty("reason", null);
        if ("FILE_NOT_FOUND".equals(reason)) {
            return AvailableStatus.FALSE;
        } else {
            return AvailableStatus.UNCHECKED;
        }
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls != null) {
            for (final DownloadLink link : urls) {
                link.setAvailableStatus(requestFileInformation(link));
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
                            final List<CrawledLink> links = new ArrayList<CrawledLink>(pv.getChildren());
                            LinkCollector.getInstance().removeChildren(links);
                            for (final CrawledLink crawledLink : links) {
                                final DownloadLink downloadLink = crawledLink.getDownloadLink();
                                prepareForLinkCrawler(downloadLink);
                            }
                            retry(links);
                            return null;
                        }
                    });
                }
            }));
        }
    }

    private void retry(List<CrawledLink> links) {
        final JobLinkCrawler jlc = LinkCollector.getInstance().newJobLinkCrawler(new LinkCollectingJob(LinkOrigin.ADD_LINKS_DIALOG.getLinkOriginDetails()));
        jlc.crawl(links);
    }

    protected void prepareForLinkCrawler(DownloadLink downloadLink) {
        if (downloadLink != null) {
            // allow LinkCrawler to process this link again
            downloadLink.setDefaultPlugin(null);
            // required for LinkCrawler.breakPluginForDecryptLoop
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKED);
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
                            final List<DownloadLink> downloadLinks = new ArrayList<DownloadLink>(pv.getChildren());
                            DownloadController.getInstance().removeChildren(downloadLinks);
                            final List<CrawledLink> links = new ArrayList<CrawledLink>();
                            for (final DownloadLink downloadLink : downloadLinks) {
                                prepareForLinkCrawler(downloadLink);
                                links.add(new CrawledLink(downloadLink));
                            }
                            retry(links);
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
