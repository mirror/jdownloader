package jd.controlling.linkcollector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;

import org.appwork.scheduler.DelayedRunnable;

public abstract class LinkCollectorHighlightListener implements LinkCollectorListener {
    private final long                       cleanupTIMEOUT = 30000;
    private HashMap<LinkCollectingJob, Long> newJobMap      = new HashMap<LinkCollectingJob, Long>();
    private DelayedRunnable                  delayedCleanup = new DelayedRunnable(IOEQ.TIMINGQUEUE, 5000l, 60000l) {

                                                                @Override
                                                                public void delayedrun() {
                                                                    boolean restartCleanup = false;
                                                                    synchronized (newJobMap) {
                                                                        Iterator<Entry<LinkCollectingJob, Long>> it = newJobMap.entrySet().iterator();
                                                                        while (it.hasNext()) {
                                                                            Entry<LinkCollectingJob, Long> next = it.next();
                                                                            if (System.currentTimeMillis() - next.getValue() > cleanupTIMEOUT) {
                                                                                it.remove();
                                                                            }
                                                                        }
                                                                        restartCleanup = newJobMap.size() > 0;
                                                                    }
                                                                    if (restartCleanup) delayedCleanup.resetAndStart();
                                                                }

                                                            };

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentModified(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        if (LinkCollector.getInstance().getPackages().size() == 0) {
            delayedCleanup.resetAndStart();
        }
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        doHightLight(parameter);
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
        doHightLight(parameter);
    }

    protected void doHightLight(CrawledLink parameter) {
        if (isThisListenerEnabled()) {
            LinkCollectingInformation sourceJob = parameter.getCollectingInfo();
            LinkCrawler lc = null;
            if (sourceJob == null || ((lc = sourceJob.getLinkCrawler()) != null && lc.isRunning())) { return; }
            if (LinkCollector.getInstance().getLinkChecker().isRunning()) {
                /*
                 * LinkChecker from LinkCollector still running, we wait till its finished!
                 */
                return;
            }
            boolean doHighlight = false;
            synchronized (newJobMap) {
                if (newJobMap.put(parameter.getSourceJob(), System.currentTimeMillis()) == null) {
                    delayedCleanup.resetAndStart();
                    doHighlight = true;
                }
            }
            if (doHighlight) {
                onHighLight(parameter);
            }
        }
    }

    abstract public void onHighLight(CrawledLink parameter);

    abstract public boolean isThisListenerEnabled();

}
