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

import org.jdownloader.gui.views.linkgrabber.addlinksdialog.CrawlerJob;

public class LinkCollector extends PackageController<CrawledPackageInfo, CrawledLinkInfo> implements LinkCheckerHandler<CrawledLinkInfo> {

    private static LinkCollector         INSTANCE    = new LinkCollector();
    private CrawledPackageInfo           dummy       = new CrawledPackageInfo();
    private LinkChecker<CrawledLinkInfo> linkChecker = null;
    private boolean                      doLinkCheck = false;

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private LinkCollector() {
        linkChecker = new LinkChecker<CrawledLinkInfo>();
        linkChecker.setLinkCheckHandler(this);
        doLinkCheck = true;
    }

    @Override
    protected void _controllerParentlessLinks(List<CrawledLinkInfo> links) {
    }

    @Override
    protected void _controllerPackageNodeRemoved(CrawledPackageInfo pkg) {
    }

    @Override
    protected void _controllerStructureChanged() {
    }

    @Override
    protected void _controllerPackageNodeAdded(CrawledPackageInfo pkg) {
    }

    public LinkCrawler addCrawlerJob(final CrawlerJob job) {
        if (job == null) throw new IllegalArgumentException("job is null");
        LinkCrawler lc = new LinkCrawler();
        if (doLinkCheck) {
            lc.setFinalLinkHandler(new LinkCrawlerFinalLinkHandler() {
                public void handleFinalLink(CrawledLinkInfo link) {

                    linkChecker.check(link);
                }
            });
        } else {
            lc.setFinalLinkHandler(new LinkCrawlerFinalLinkHandler() {
                public void handleFinalLink(CrawledLinkInfo link) {

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
        System.out.println("checked: " + link.getName() + ":" + link.gethPlugin().getHost());
        testAdd(link);
    }

    private void testAdd(CrawledLinkInfo link) {
        ArrayList<CrawledLinkInfo> add = new ArrayList<CrawledLinkInfo>(1);
        add.add(link);
        LinkCollector.this.addmoveChildren(dummy, add, -1);
    }

}
