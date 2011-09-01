package jd.controlling.linkcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.IOEQ;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLinkInfo;
import jd.controlling.linkcrawler.CrawledPackageInfo;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.CrawlerJob;

public class LinkCollector extends PackageController<CrawledPackageInfo, CrawledLinkInfo> implements LinkCheckerHandler<CrawledLinkInfo> {

    private transient Eventsender<LinkCollectorListener, LinkCollectorEvent> broadcaster = new Eventsender<LinkCollectorListener, LinkCollectorEvent>() {

                                                                                             @Override
                                                                                             protected void fireEvent(final LinkCollectorListener listener, final LinkCollectorEvent event) {
                                                                                                 listener.onLinkCollectorEvent(event);
                                                                                             };
                                                                                         };

    private static LinkCollector                                             INSTANCE    = new LinkCollector();
    private LinkChecker<CrawledLinkInfo>                                     linkChecker = null;
    private AtomicInteger                                                    collStarts  = new AtomicInteger(0);
    private AtomicInteger                                                    collStops   = new AtomicInteger(0);

    private class dupeCheck {
        int counter = 1;
    }

    /**
     * NOTE: only access this inside the IOEQ
     */
    private HashMap<String, dupeCheck> dupeMap = new HashMap<String, dupeCheck>();

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private LinkCollector() {
        linkChecker = new LinkChecker<CrawledLinkInfo>();
        linkChecker.setLinkCheckHandler(this);
    }

    public boolean isRunning() {
        return collStarts.get() > collStops.get();
    }

    public void addListener(final LinkCollectorListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(final LinkCollectorListener l) {
        broadcaster.removeListener(l);
    }

    @Override
    protected void _controllerParentlessLinks(List<CrawledLinkInfo> links) {
        for (CrawledLinkInfo link : links) {
            /* update dupeMap */
            dupeCheck dupes = dupeMap.get(link.getURL());
            if (dupes != null) {
                dupes.counter--;
                if (dupes.counter <= 0) {
                    dupeMap.remove(link.getURL());
                }
            }
        }
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
            lc.setHandler(new LinkCrawlerHandler() {
                public void handleFinalLink(CrawledLinkInfo link) {
                    link.setSourceJob(job);
                    linkChecker.check(link);
                }

                public void linkCrawlerStarted() {
                    checkRunningState(true);
                }

                public void linkCrawlerStopped() {
                    checkRunningState(false);
                }
            });
        } else {
            lc.setHandler(new LinkCrawlerHandler() {
                public void handleFinalLink(CrawledLinkInfo link) {
                    link.setSourceJob(job);
                    addCrawledLink(link);
                }

                public void linkCrawlerStarted() {
                    checkRunningState(true);
                }

                public void linkCrawlerStopped() {
                    checkRunningState(false);
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
        addCrawledLink(link);
    }

    public void linkCheckStarted() {
        checkRunningState(true);
    }

    public void linkCheckStopped() {
        checkRunningState(false);
    }

    private void addCrawledLink(final CrawledLinkInfo link) {
        if (link.getParentNode() != null) {

        } else {
            /* try to find good matching package or create new one */
            final String packageName = LinknameCleaner.cleanFileName(link.getName());
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    /* update dupeCheck map */
                    if (!link.isDupeAllow() && dupeMap.containsKey(link.getURL())) {
                        System.out.println("dupe detected:" + link.getURL());
                        return null;
                    } else {
                        dupeCheck dupes = dupeMap.get(link.getURL());
                        if (dupes == null) {
                            dupes = new dupeCheck();
                            /* counter is already 1 here */
                            dupeMap.put(link.getURL(), dupes);
                        } else {
                            /* increase counter */
                            dupes.counter++;
                        }
                    }
                    String name = packageName;
                    int bestMatch = 0;
                    CrawledPackageInfo bestPackage = null;
                    boolean readL = readLock();
                    try {
                        for (CrawledPackageInfo pkg : packages) {
                            int sim = LinknameCleaner.comparepackages(pkg.getAutoPackageName(), name);
                            if (sim > bestMatch) {
                                bestMatch = sim;
                                bestPackage = pkg;
                            }
                        }
                    } finally {
                        readUnlock(readL);
                    }
                    if (bestMatch < 99 || bestPackage == null) {
                        /* create new Package */
                        bestPackage = new CrawledPackageInfo();
                    } else {
                        /* rename existing one */
                        name = getSimString(bestPackage.getAutoPackageName(), name);
                    }
                    bestPackage.setAutoPackageName(name);
                    /* add link to LinkCollector */
                    List<CrawledLinkInfo> add = new ArrayList<CrawledLinkInfo>(1);
                    add.add(link);
                    LinkCollector.this.addmoveChildren(bestPackage, add, -1);
                    return null;
                }
            });
        }
    }

    private String getSimString(String a, String b) {
        String aa = a.toLowerCase();
        String bb = b.toLowerCase();
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < Math.min(aa.length(), bb.length()); i++) {
            if (aa.charAt(i) == bb.charAt(i)) {
                ret.append(a.charAt(i));
            }
        }
        return ret.toString();
    }

    public void clear() {
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                ArrayList<CrawledPackageInfo> clearList = null;
                writeLock();
                try {
                    clearList = new ArrayList<CrawledPackageInfo>(packages);
                } finally {
                    writeUnlock();
                }
                for (CrawledPackageInfo pkg : clearList) {
                    LinkCollector.this.removePackage(pkg);
                }
                return null;
            }
        });
    }

    private void checkRunningState(boolean start) {
        if (start) {
            this.collStarts.incrementAndGet();
        } else {
            this.collStops.incrementAndGet();
        }
        if (this.collStarts.get() == this.collStops.get()) {
            broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.COLLECTOR_STOP));
        } else {
            broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.COLLECTOR_START));
        }
    }

}
