package jd.controlling.linkcollector;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLinkInfo;
import jd.controlling.linkcrawler.CrawledPackageInfo;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFinalLinkHandler;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.Eventsender;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.CrawlerJob;

public class LinkCollector extends PackageController<CrawledPackageInfo, CrawledLinkInfo> implements LinkCheckerHandler<CrawledLinkInfo> {

    private transient Eventsender<LinkCollectorListener, LinkCollectorEvent> broadcaster = new Eventsender<LinkCollectorListener, LinkCollectorEvent>() {

                                                                                             @Override
                                                                                             protected void fireEvent(final LinkCollectorListener listener, final LinkCollectorEvent event) {
                                                                                                 listener.onLinkCollectorEvent(event);
                                                                                             };
                                                                                         };

    private static LinkCollector                                             INSTANCE    = new LinkCollector();
    private CrawledPackageInfo                                               dummy       = new CrawledPackageInfo();
    private LinkChecker<CrawledLinkInfo>                                     linkChecker = null;

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private LinkCollector() {
        linkChecker = new LinkChecker<CrawledLinkInfo>();
        linkChecker.setLinkCheckHandler(this);
    }

    public void addListener(final LinkCollectorListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(final LinkCollectorListener l) {
        broadcaster.removeListener(l);
    }

    @Override
    protected void _controllerParentlessLinks(List<CrawledLinkInfo> links) {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, links));
    }

    @Override
    protected void _controllerPackageNodeRemoved(CrawledPackageInfo pkg) {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, pkg));
    }

    @Override
    protected void _controllerStructureChanged() {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
    }

    @Override
    protected void _controllerPackageNodeAdded(CrawledPackageInfo pkg) {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
    }

    public LinkCrawler addCrawlerJob(final CrawlerJob job) {
        if (job == null) throw new IllegalArgumentException("job is null");
        LinkCrawler lc = new LinkCrawler();
        if (JsonConfig.create(LinkCollectorConfig.class).getDoLinkCheck()) {
            lc.setFinalLinkHandler(new LinkCrawlerFinalLinkHandler() {
                public void handleFinalLink(CrawledLinkInfo link) {
                    link.setSourceJob(job);
                    linkChecker.check(link);
                }
            });
        } else {
            lc.setFinalLinkHandler(new LinkCrawlerFinalLinkHandler() {
                public void handleFinalLink(CrawledLinkInfo link) {
                    link.setSourceJob(job);
                    testAdd(link);
                }
            });
        }
        if (job.isDeepAnalyse()) {
            lc.enqueueDeep(job.getText(), null);
        } else {
            lc.enqueueNormal(job.getText(), null);
        }
        /*
         * we don't want to keep reference on text during the whole link
         * grabbing/checking/collecting way
         */
        job.setText(null);
        return lc;
    }

    public void linkCheckDone(CrawledLinkInfo link) {
        testAdd(link);
    }

    private void testAdd(CrawledLinkInfo link) {
        ArrayList<CrawledLinkInfo> add = new ArrayList<CrawledLinkInfo>(1);
        add.add(link);
        LinkCollector.this.addmoveChildren(dummy, add, -1);
    }

}
