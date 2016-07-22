package org.jdownloader.gui.notify.linkcrawler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcollector.event.LinkCollectorCrawlerListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfig;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;

public class LinkCrawlerBubbleSupport extends AbstractBubbleSupport implements LinkCollectorListener {

    private final ArrayList<Element>                              elements         = new ArrayList<Element>();
    private final BubbleNotifyConfig.LINKGRABBER_BUBBLE_NOTIFY_ON notifyOn         = JsonConfig.create(BubbleNotifyConfig.class).getBubbleNotifyOnNewLinkgrabberLinksOn();
    private final boolean                                         registerOnPlugin = BubbleNotifyConfig.LINKGRABBER_BUBBLE_NOTIFY_ON.PLUGIN.equals(notifyOn);

    public LinkCrawlerBubbleSupport() {
        super(_GUI.T.plugins_optional_JDLightTray_ballon_newlinks3(), CFG_BUBBLE.BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED);
        LinkCrawlerBubbleContent.fill(elements);
        LinkCollector.getInstance().getEventsender().addListener(this, true);
    }

    private class LinkCrawlerBubbleWrapper implements AbstractNotifyWindowFactory, LinkCollectorCrawlerListener {

        private volatile JobLinkCrawler    crawler    = null;
        private volatile LinkCrawlerBubble bubble     = null;
        private final AtomicBoolean        registered = new AtomicBoolean(false);

        private LinkCrawlerBubbleWrapper(JobLinkCrawler crawler) {
            this.crawler = crawler;
        }

        @Override
        public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
            if (bubble == null && crawler != null) {
                final LinkCrawlerBubble finalBubble = new LinkCrawlerBubble(LinkCrawlerBubbleSupport.this, crawler);
                final Timer t = new Timer(1000, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (finalBubble.isClosed() || finalBubble.isDisposed()) {
                            crawler = null;
                            bubble = null;
                            finalBubble.getContentComponent().stop();
                            ((Timer) e.getSource()).stop();
                        } else {
                            if (finalBubble.isVisible()) {
                                finalBubble.requestUpdate();
                            }
                            if (finalBubble.getContentComponent().askForClose(finalBubble.getCrawler())) {
                                crawler = null;
                                bubble = null;
                                finalBubble.getContentComponent().stop();
                                ((Timer) e.getSource()).stop();
                                finalBubble.hideBubble(finalBubble.getTimeout());
                            }
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
                crawler.getEventSender().removeListener(this);
                show(this);
            }
        }

        @Override
        public void onProcessingCrawlerPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
            if (registerOnPlugin) {
                register();
            }
        }

        @Override
        public void onProcessingHosterPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
            register();
        }

        @Override
        public void onProcessingContainerPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
            if (registerOnPlugin) {
                register();
            }
        }

    }

    @Override
    public List<Element> getElements() {
        return elements;
    }

    private final WeakHashMap<LinkCollectorCrawler, LinkCrawlerBubbleWrapper> map = new WeakHashMap<LinkCollectorCrawler, LinkCrawlerBubbleWrapper>();

    @Override
    public void onLinkCrawlerAdded(final LinkCollectorCrawler crawler) {
        if (isEnabled() && crawler instanceof JobLinkCrawler) {
            final LinkCrawlerBubbleWrapper wrapper = new LinkCrawlerBubbleWrapper((JobLinkCrawler) crawler);
            if (BubbleNotifyConfig.LINKGRABBER_BUBBLE_NOTIFY_ON.ALWAYS.equals(notifyOn)) {
                wrapper.register();
            } else {
                crawler.getEventSender().addListener(wrapper);
                synchronized (map) {
                    map.put(crawler, wrapper);
                }
            }
        }
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {

    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
        final LinkCrawlerBubbleWrapper wrapper;
        synchronized (map) {
            wrapper = map.remove(crawler);
        }
        if (wrapper != null) {
            crawler.getEventSender().removeListener(wrapper);
        }
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

    @Override
    public void onLinkCrawlerFinished() {
    }

}
