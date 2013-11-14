package org.jdownloader.gui.notify;

import java.util.HashSet;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
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
        duration = addPair(_GUI._.ReconnectDialog_layoutDialogContent_duration(), IconKey.ICON_WAIT);

        links = addPair(_GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundlink(), IconKey.ICON_FILE);

        packages = addPair(_GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundpackages(), IconKey.ICON_PACKAGE_NEW);

        offline = addPair(_GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundoffline(), IconKey.ICON_ERROR);

        online = addPair(_GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundonline(), IconKey.ICON_OK);

        status = addPair(_GUI._.LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_status(), IconKey.ICON_RUN);
        offline.setVisible(false);
        progressCircle.setIndeterminate(true);
        progressCircle.setValue(0);

    }

    public void update() {
        duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
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
            System.out.println(jobless);
            offline.setText(offlineCnt + "");
            online.setText(onlineCnt + "");
            if (offlineCnt > 0) {
                offline.setVisible(true);
            }
            this.offlineCount = offlineCnt;
            this.onlineCount = offlineCnt;
            this.joblessCount = jobless;
            this.linksCount = jlc.getCrawledLinksFoundCounter();
            links.setText(jlc.getCrawledLinksFoundCounter() + "");
            packages.setText(dupe.size() + "");
        } else {

            links.setText(jlc.getCrawledLinksFoundCounter() + "");
        }

        if (jlc.isRunning()) {
            status.setText(_GUI._.LinkCrawlerBubbleContent_update_runnning());

        } else {
            if (LinkCollector.getInstance().getLinkChecker().isRunning()) {
                status.setText(_GUI._.LinkCrawlerBubbleContent_update_online());
            } else {
                status.setText(_GUI._.LinkCrawlerBubbleContent_update_finished());
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
}
