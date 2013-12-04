package org.jdownloader.gui.notify.linkcrawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;

public class LinkCrawlerBubbleContent extends AbstractBubbleContentPanel {

    private Pair duration;
    private Pair links;

    private Pair offline;
    private Pair status;

    private int  joblessCount;
    private int  offlineCount;
    private int  linksCount;
    private long lastChange;
    private int  onlineCount;
    private Pair packages;
    private Pair online;

    public LinkCrawlerBubbleContent() {

        super("linkgrabber");
        layoutComponents();
        offline.setVisible(false);
        progressCircle.setIndeterminate(true);
        progressCircle.setValue(0);

    }

    protected void addProgress() {

    }

    protected void layoutComponents() {

        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ANIMATED_ICON_VISIBLE.isEnabled()) {
            setLayout(new MigLayout("ins 3 3 0 3,wrap 3", "[][fill][grow,fill]", "[]"));
            add(progressCircle, "width 32!,height 32!,pushx,growx,pushy,growy,spany,aligny top");
        } else {
            setLayout(new MigLayout("ins 3 3 0 3,wrap 2", "[fill][grow,fill]", "[]"));
        }

        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_DURATION_VISIBLE.isEnabled()) {
            duration = addPair(duration, _GUI._.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT);
        }

        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_LINK_COUNT_VISIBLE.isEnabled()) {
            links = addPair(links, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundlink(), IconKey.ICON_FILE);
        }

        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_PACKAGE_COUNT_VISIBLE.isEnabled()) {
            packages = addPair(packages, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundpackages(), IconKey.ICON_PACKAGE_NEW);
        }

        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_OFFLINE_COUNT_VISIBLE.isEnabled()) {
            offline = addPair(offline, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundoffline(), IconKey.ICON_ERROR);
        }

        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ONLINE_COUNT_VISIBLE.isEnabled()) {
            online = addPair(online, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundonline(), IconKey.ICON_OK);
        }

        if (CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_STATUS_VISIBLE.isEnabled()) {
            status = addPair(status, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_status(), IconKey.ICON_RUN);
        }
    }

    public void update() {
        if (duration != null) duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
    }

    public void update(JobLinkCrawler jlc) {
        update();

        if (jlc.getCrawledLinksFoundCounter() < 500000) {

            List<CrawledLink> linklist = jlc.getCrawledLinks();

            HashSet<CrawledPackage> dupe = new HashSet<CrawledPackage>();
            int offlineCnt = 0;
            int onlineCnt = 0;
            int jobless = 0;
            synchronized (linklist) {
                for (CrawledLink cl : linklist) {
                    dupe.add(cl.getParentNode());
                    if (cl.getSourceJob() != jlc.getJob()) {
                        jobless++;
                    }

                    DownloadLink dl = cl.getDownloadLink();
                    if (dl != null) {
                        AvailableStatus status = dl.getAvailableStatus();
                        switch (status) {
                        case FALSE:
                            offlineCnt++;
                            break;
                        case TRUE:
                            onlineCnt++;
                            break;
                        }
                    }
                }
            }

            boolean changes = false;
            changes |= onlineCount != onlineCnt;
            changes |= offlineCount != offlineCnt;
            changes |= joblessCount != jobless;
            changes |= linksCount != jlc.getCrawledLinksFoundCounter();

            if (changes) {
                lastChange = System.currentTimeMillis();
            }

            if (offline != null) offline.setText(offlineCnt + "");
            if (online != null) online.setText(onlineCnt + "");
            if (offlineCnt > 0) {
                if (offline != null) offline.setVisible(true);
            }
            this.offlineCount = offlineCnt;
            this.onlineCount = offlineCnt;
            this.joblessCount = jobless;
            this.linksCount = jlc.getCrawledLinksFoundCounter();
            if (links != null) links.setText(jlc.getCrawledLinksFoundCounter() + "");
            if (packages != null) packages.setText(dupe.size() + "");
        } else {

            if (links != null) links.setText(jlc.getCrawledLinksFoundCounter() + "");
        }

        if (jlc.isRunning()) {
            if (status != null) status.setText(_GUI._.LinkCrawlerBubbleContent_update_runnning());

        } else {
            if (LinkCollector.getInstance().getLinkChecker().isRunning()) {
                if (status != null) status.setText(_GUI._.LinkCrawlerBubbleContent_update_online());
            } else {
                if (status != null) status.setText(_GUI._.LinkCrawlerBubbleContent_update_finished());
            }
        }
    }

    public boolean askForClose(LinkCollectorCrawler caller) {
        if (caller instanceof JobLinkCrawler) {
            // if (true) return false;
            if (caller.isRunning()) return false;
            if (!caller.isRunning() && !LinkCollector.getInstance().getLinkChecker().isRunning()) return true;
            return System.currentTimeMillis() - lastChange > 10000;

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
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_DURATION_VISIBLE, _GUI._.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_LINK_COUNT_VISIBLE, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundlink(), IconKey.ICON_FILE));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_PACKAGE_COUNT_VISIBLE, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundpackages(), IconKey.ICON_PACKAGE_NEW));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_OFFLINE_COUNT_VISIBLE, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundoffline(), IconKey.ICON_ERROR));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ONLINE_COUNT_VISIBLE, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundonline(), IconKey.ICON_OK));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_STATUS_VISIBLE, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_status(), IconKey.ICON_RUN));
        elements.add(new Element(CFG_BUBBLE.CRAWLER_BUBBLE_CONTENT_ANIMATED_ICON_VISIBLE, _GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_icon(), IconKey.ICON_FIND));
    }
}
