package org.jdownloader.gui.notify.linkcrawler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcollector.event.LinkCollectorCrawlerListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;

public class LinkCrawlerBubbleSupport extends AbstractBubbleSupport implements LinkCollectorListener {

    private ArrayList<Element> elements;

    public LinkCrawlerBubbleSupport() {
        super(_GUI._.plugins_optional_JDLightTray_ballon_newlinks3(), CFG_BUBBLE.BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED);
        elements = new ArrayList<Element>();
        LinkCrawlerBubbleContent.fill(elements);
        LinkCollector.getInstance().getEventsender().addListener(this, true);
    }

    private class LinkCrawlerBubbleWrapper implements AbstractNotifyWindowFactory, LinkCollectorCrawlerListener {

        private final WeakReference<LinkCollectorCrawler> crawler;
        private final AtomicBoolean                       registered = new AtomicBoolean(false);
        private volatile LinkCrawlerBubble                bubble     = null;

        private LinkCrawlerBubbleWrapper(LinkCollectorCrawler crawler) {
            this.crawler = new WeakReference<LinkCollectorCrawler>(crawler);
        }

        private void close() {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (bubble != null) {
                        BubbleNotify.getInstance().hide(bubble);
                        bubble = null;
                    }
                }
            };
        }

        @Override
        public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
            LinkCollectorCrawler crwl = crawler.get();
            if (crwl != null) {
                final LinkCrawlerBubble finalBubble = new LinkCrawlerBubble(LinkCrawlerBubbleSupport.this, crwl);
                final Timer t = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (finalBubble.isClosed() || finalBubble.isDisposed()) {
                            finalBubble.getContentComponent().stop();
                            ((Timer) e.getSource()).stop();
                            bubble = null;
                        }
                        if (finalBubble.isVisible()) {
                            finalBubble.update();
                        }
                        if (finalBubble.getContentComponent().askForClose(crawler.get())) {
                            finalBubble.getContentComponent().stop();
                            ((Timer) e.getSource()).stop();
                            finalBubble.hideBubble(finalBubble.getSuperTimeout());
                            bubble = null;
                        }
                    }

                });
                t.setInitialDelay(0);
                t.setRepeats(true);
                t.start();
                bubble = finalBubble;
            }
            return bubble;
        }

        private void register() {
            if (registered.compareAndSet(false, true)) {
                LinkCollectorCrawler crwl = crawler.get();
                if (crwl != null) {
                    crwl.getEventSender().removeListener(this);
                    show(this);
                }
            }
        }

        @Override
        public void onProcessingCrawlerPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
            register();
        }

        @Override
        public void onProcessingHosterPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
            register();
        }

        @Override
        public void onProcessingContainerPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
            register();
        }

    }

    private final WeakHashMap<LinkCollectorCrawler, LinkCrawlerBubbleWrapper> map = new WeakHashMap<LinkCollectorCrawler, LinkCrawlerBubbleWrapper>();

    @Override
    public List<Element> getElements() {
        return elements;
    }

    @Override
    public void onLinkCrawlerAdded(final LinkCollectorCrawler crawler) {
        if (isEnabled()) {
            synchronized (map) {
                LinkCrawlerBubbleWrapper wrapper = new LinkCrawlerBubbleWrapper(crawler);
                crawler.getEventSender().addListener(wrapper, true);
                map.put(crawler, wrapper);
            }
        }
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {

    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
        // Bubbles stop and cleanup themself by polling the askForClose method
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
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
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }

}
