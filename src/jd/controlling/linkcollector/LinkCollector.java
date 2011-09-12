package jd.controlling.linkcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.IOEQ;
import jd.controlling.UniqueID;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.CrawlerJob;

public class LinkCollector extends PackageController<CrawledPackage, CrawledLink> implements LinkCheckerHandler<CrawledLink> {

    private transient Eventsender<LinkCollectorListener, LinkCollectorEvent> broadcaster = new Eventsender<LinkCollectorListener, LinkCollectorEvent>() {

                                                                                             @Override
                                                                                             protected void fireEvent(final LinkCollectorListener listener, final LinkCollectorEvent event) {
                                                                                                 listener.onLinkCollectorEvent(event);
                                                                                             };
                                                                                         };

    private static LinkCollector                                             INSTANCE    = new LinkCollector();
    private LinkChecker<CrawledLink>                                         linkChecker = null;
    private AtomicInteger                                                    collStarts  = new AtomicInteger(0);
    private AtomicInteger                                                    collStops   = new AtomicInteger(0);

    private class dupeCheck {
        int counter = 1;
    }

    /**
     * NOTE: only access these fields inside the IOEQ
     */
    private HashMap<String, dupeCheck>        dupeMap           = new HashMap<String, dupeCheck>();
    private HashMap<UniqueID, CrawledPackage> packageMap        = new HashMap<UniqueID, CrawledPackage>();
    private HashMap<CrawledPackage, UniqueID> packageMapReverse = new HashMap<CrawledPackage, UniqueID>();

    private LinkCollectorConfig               config;

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private LinkCollector() {
        linkChecker = new LinkChecker<CrawledLink>();
        linkChecker.setLinkCheckHandler(this);
        config = JsonConfig.create(LinkCollectorConfig.class);
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
    protected void _controllerParentlessLinks(List<CrawledLink> links) {
        for (CrawledLink link : links) {
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
    protected void _controllerPackageNodeRemoved(CrawledPackage pkg) {
        /* update packageMap */
        UniqueID id = packageMapReverse.remove(pkg);
        if (id != null) packageMap.remove(id);
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, pkg));
    }

    @Override
    protected void _controllerStructureChanged() {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
    }

    @Override
    protected void _controllerPackageNodeAdded(CrawledPackage pkg) {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
    }

    public LinkCrawler addCrawlerJob(final CrawlerJob job) {
        if (job == null) throw new IllegalArgumentException("job is null");
        LinkCrawler lc = new LinkCrawler();
        if (config.getDoLinkCheck()) {
            lc.setHandler(new LinkCrawlerHandler() {
                public void handleFinalLink(CrawledLink link) {
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
                public void handleFinalLink(CrawledLink link) {
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

    public void linkCheckDone(CrawledLink link) {
        addCrawledLink(link);
    }

    public void linkCheckStarted() {
        checkRunningState(true);
    }

    public void linkCheckStopped() {
        checkRunningState(false);
    }

    /**
     * NOTE: use only inside the IOEQ
     */
    private boolean dupeCheck(CrawledLink link) {
        if (!link.isDupeAllow() && dupeMap.containsKey(link.getURL())) {
            return false;
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
        return true;
    }

    private void addCrawledLink(final CrawledLink link) {
        final UniqueID wanted = link.getDownloadLink().getFilePackage().getUniqueID();
        if (wanted != null) {
            /* custom package was set, try to find it or create new one */
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    /* update dupeCheck map */
                    if (dupeCheck(link) == false) return null;
                    CrawledPackage match = packageMap.get(wanted);
                    if (match == null) {
                        match = new CrawledPackage();
                        match.setAllowAutoPackage(false);
                        packageMap.put(wanted, match);
                        packageMapReverse.put(match, wanted);
                        /*
                         * forward name from FilePackage Instance to
                         * CrawledPackageInfo
                         */
                        match.setAutoPackageName(link.getDownloadLink().getFilePackage().getName());
                        match.setCreated(link.getCreated());
                    }
                    List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                    add.add(link);
                    LinkCollector.this.addmoveChildren(match, add, -1);
                    return null;
                }
            });
        } else {
            /* try to find good matching package or create new one */
            final String packageName = LinknameCleaner.cleanFileName(link.getName());
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    /* update dupeCheck map */
                    if (dupeCheck(link) == false) return null;
                    String name = packageName;
                    int bestMatch = 0;
                    CrawledPackage bestPackage = null;
                    boolean readL = readLock();
                    try {
                        for (CrawledPackage pkg : packages) {
                            if (pkg.isAllowAutoPackage() == false) continue;
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
                        bestPackage = new CrawledPackage();
                        bestPackage.setCreated(link.getCreated());
                    } else {
                        /* rename existing one */
                        name = getSimString(bestPackage.getAutoPackageName(), name);
                    }
                    bestPackage.setAutoPackageName(name);
                    /* add link to LinkCollector */
                    List<CrawledLink> add = new ArrayList<CrawledLink>(1);
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
        int maxL = Math.min(aa.length(), bb.length());
        StringBuilder ret = new StringBuilder(maxL);
        for (int i = 0; i < maxL; i++) {
            if (aa.charAt(i) == bb.charAt(i)) {
                ret.append(a.charAt(i));
            }
        }
        return ret.toString();
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
