package org.jdownloader.gui.notify.linkcrawler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfig;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkCrawlerBubbleContent extends AbstractBubbleContentPanel {

    private Pair             duration;
    private Pair             links;

    private Pair             offline;
    private Pair             status;

    private Pair             packages;
    private Pair             online;
    private final long       CLOSETIMEOUT = JsonConfig.create(BubbleNotifyConfig.class).getBubbleNotifyOnNewLinkgrabberLinksEndNotifyDelay();
    private final LinkOrigin origin;

    public LinkCrawlerBubbleContent(JobLinkCrawler crawler) {
        super(IconKey.ICON_LINKGRABBER);
        origin = crawler.getJob().getOrigin().getOrigin();
        layoutComponents();
        if (offline != null) {
            offline.setVisible(false);
        }
        startProgressCircle();
    }

    protected void addProgress() {
    }

    protected void layoutComponents() {
        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ANIMATED_ICON_VISIBLE.isEnabled()) {
            setLayout(new MigLayout("ins 3 3 0 3,wrap 3", "[][fill][grow,fill]", "[]"));
            String iconKey = IconKey.ICON_LINKGRABBER;
            if (origin != null) {
                switch (origin) {
                case CLIPBOARD:
                    iconKey = IconKey.ICON_CLIPBOARD;
                    break;
                case ADD_CONTAINER_ACTION:
                case DOWNLOADED_CONTAINER:
                    iconKey = IconKey.ICON_ADDCONTAINER;
                    break;
                case CNL:
                    iconKey = IconKey.ICON_LOGO_CNL;
                    break;
                case MYJD:
                    iconKey = IconKey.ICON_LOGO_MYJDOWNLOADER;
                    break;
                case ADD_LINKS_DIALOG:
                case DRAG_DROP_ACTION:
                    iconKey = IconKey.ICON_ADD;
                    break;
                default:
                    iconKey = IconKey.ICON_WAIT;
                    break;
                }
            }
            progressCircle.updatePainter(NewTheme.I().getIcon(iconKey, 20), Color.WHITE, Color.GRAY, Color.WHITE, Color.GREEN, Color.LIGHT_GRAY, Color.GREEN);
            add(progressCircle, "width 32!,height 32!,pushx,growx,pushy,growy,spany,aligny top");
        } else {
            setLayout(new MigLayout("ins 3 3 0 3,wrap 2", "[fill][grow,fill]", "[]"));
        }
        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_DURATION_VISIBLE.isEnabled()) {
            duration = addPair(duration, _GUI.T.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT);
        }
        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_LINK_COUNT_VISIBLE.isEnabled()) {
            links = addPair(links, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundlink(), IconKey.ICON_FILE);
        }
        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_PACKAGE_COUNT_VISIBLE.isEnabled()) {
            packages = addPair(packages, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundpackages(), IconKey.ICON_PACKAGE_NEW);
        }
        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_OFFLINE_COUNT_VISIBLE.isEnabled()) {
            offline = addPair(offline, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundoffline(), IconKey.ICON_ERROR);
        }
        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ONLINE_COUNT_VISIBLE.isEnabled()) {
            online = addPair(online, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundonline(), IconKey.ICON_OK);
        }
        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_STATUS_VISIBLE.isEnabled()) {
            status = addPair(status, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_status(), IconKey.ICON_RUN);
        }
    }

    private int              joblessCount = -1;
    private int              offlineCount = -1;
    private int              linksCount   = -1;
    private int              onlineCount  = -1;
    private final AtomicLong lastChange   = new AtomicLong(-1);

    public void update(final JobLinkCrawler jlc) {
        final List<CrawledLink> linklist = jlc.getCrawledLinks();
        final HashSet<CrawledPackage> dupe = new HashSet<CrawledPackage>();
        int offlineCnt = 0;
        int onlineCnt = 0;
        int jobless = 0;
        synchronized (linklist) {
            for (final CrawledLink cl : linklist) {
                dupe.add(cl.getParentNode());
                if (cl.getSourceJob() != jlc.getJob()) {
                    jobless++;
                }
                switch (cl.getLinkState()) {
                case OFFLINE:
                    offlineCnt++;
                    break;
                case ONLINE:
                    onlineCnt++;
                    break;
                }
            }
        }

        boolean changes = false;
        final int linksCnt = jlc.getCrawledLinksFoundCounter();
        changes |= onlineCount != onlineCnt;
        changes |= offlineCount != offlineCnt;
        changes |= joblessCount != jobless;
        changes |= linksCount != linksCnt;
        this.offlineCount = offlineCnt;
        this.onlineCount = onlineCnt;
        this.joblessCount = jobless;
        this.linksCount = linksCnt;
        final long lastChange;
        if (changes) {
            lastChange = System.currentTimeMillis();
            this.lastChange.set(lastChange);
        } else {
            lastChange = LinkCrawlerBubbleContent.this.lastChange.get();
        }
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                if (online != null) {
                    online.setText(onlineCount + "");
                }
                if (offlineCount > 0) {
                    if (offline != null) {
                        offline.setText(offlineCount + "");
                        offline.setVisible(true);
                    }
                    if (offlineCount >= linksCount) {
                        getWindow().setHighlightColor(LAFOptions.getInstance().getColorForErrorForeground());
                    } else {
                        getWindow().setHighlightColor(LAFOptions.getInstance().getColorForTableSortedColumnView());
                    }
                } else {
                    getWindow().setHighlightColor(null);
                }
                if (links != null) {
                    links.setText(linksCount + "");
                }
                if (packages != null) {
                    packages.setText(dupe.size() + "");
                }
                final boolean isCollecting = jlc.isCollecting();
                if (status != null) {
                    if (jlc.isRunning()) {
                        status.setText(_GUI.T.LinkCrawlerBubbleContent_update_runnning());
                    } else {
                        if (jlc.getLinkChecker().isRunning()) {
                            status.setText(_GUI.T.LinkCrawlerBubbleContent_update_online());
                        } else {
                            status.setText(_GUI.T.LinkCrawlerBubbleContent_update_finished());
                        }
                    }
                }
                if (duration != null) {
                    if (isCollecting) {
                        // still collecting
                        duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                    } else {
                        // show complete duration
                        if (lastChange == -1) {
                            duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                        } else {
                            duration.setText(TimeFormatter.formatMilliSeconds(lastChange - startTime, 0));
                        }
                    }
                }
                if (isCollecting) {
                    startProgressCircle();
                } else {
                    stopProgressCircle();
                }
            }
        }.waitForEDT();
    }

    private void stopProgressCircle() {
        if (progressCircle != null) {
            progressCircle.setIndeterminate(false);
            progressCircle.setMaximum(100);
            progressCircle.setValue(100);
        }
    }

    private void startProgressCircle() {
        if (progressCircle != null) {
            progressCircle.setIndeterminate(true);
            progressCircle.setValue(0);
        }
    }

    public boolean askForClose(LinkCollectorCrawler caller) {
        if (caller instanceof JobLinkCrawler) {
            final JobLinkCrawler jobLinkCrawler = (JobLinkCrawler) caller;
            if (jobLinkCrawler.isCollecting()) {
                return false;
            } else {
                final long lastChange = this.lastChange.get();
                if (lastChange == -1) {
                    return false;
                } else {
                    return System.currentTimeMillis() - lastChange > CLOSETIMEOUT;
                }
            }
        }
        return true;
    }

    @Override
    public void updateLayout() {
        removeAll();
        layoutComponents();
        revalidate();
        repaint();
    }

    public static void fill(ArrayList<Element> elements) {
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_DURATION_VISIBLE, _GUI.T.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_LINK_COUNT_VISIBLE, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundlink(), IconKey.ICON_FILE));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_PACKAGE_COUNT_VISIBLE, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundpackages(), IconKey.ICON_PACKAGE_NEW));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_OFFLINE_COUNT_VISIBLE, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundoffline(), IconKey.ICON_ERROR));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ONLINE_COUNT_VISIBLE, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundonline(), IconKey.ICON_OK));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_STATUS_VISIBLE, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_status(), IconKey.ICON_RUN));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ANIMATED_ICON_VISIBLE, _GUI.T.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_icon(), IconKey.ICON_FIND));
    }
}
