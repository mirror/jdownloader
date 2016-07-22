package org.jdownloader.gui.notify.linkcrawler;

import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LinkCrawlerBubble extends AbstractNotifyWindow<LinkCrawlerBubbleContent> {

    @Override
    protected void onMouseClicked(MouseEvent m) {
        super.onMouseClicked(m);
        JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
        JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
    }

    protected void onSettings() {
        JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
        ConfigurationView.getInstance().setSelectedSubPanel(BubbleNotifyConfigPanel.class);
    }

    private final JobLinkCrawler crawler;

    public JobLinkCrawler getCrawler() {
        return crawler;
    }

    public LinkCrawlerBubble(LinkCrawlerBubbleSupport linkCrawlerBubbleSupport, JobLinkCrawler crawler) {
        super(linkCrawlerBubbleSupport, _GUI.T.balloon_new_links(), new LinkCrawlerBubbleContent(crawler));
        this.crawler = crawler;
    }

    @Override
    protected int getTimeout() {
        return 0;
    }

    public int getSuperTimeout() {
        return super.getTimeout();
    }

    private final DelayedRunnable update = new DelayedRunnable(TaskQueue.TIMINGQUEUE, 500l, 1000l) {

                                             @Override
                                             public String getID() {
                                                 return "LinkCrawlerBubble";
                                             }

                                             @Override
                                             public void delayedrun() {
                                                 delayedUpdate();
                                             }
                                         };

    private final void delayedUpdate() {
        final JobLinkCrawler jlc = getCrawler();
        final LinkCollectingJob job = jlc.getJob();
        final LinkOrigin src;
        if (job != null) {
            src = job.getOrigin().getOrigin();
        } else {
            src = null;
        }
        if (src == null) {
            setHeaderText(_GUI.T.LinkCrawlerBubble_update_header());
        } else if (src == LinkOrigin.ADD_LINKS_DIALOG) {
            setHeaderText(_GUI.T.LinkCrawlerBubble_update_header());
        } else if (src == LinkOrigin.CLIPBOARD) {
            final String sourceURL;
            if (job != null) {
                sourceURL = job.getCustomSourceUrl();
            } else {
                sourceURL = null;
            }
            if (StringUtils.isNotEmpty(sourceURL)) {
                try {
                    final URL url = new URL(sourceURL);
                    setHeaderText(_GUI.T.LinkCrawlerBubble_update_header_from_Clipboard_url(url.getHost()));
                } catch (MalformedURLException e) {
                    setHeaderText(_GUI.T.LinkCrawlerBubble_update_header_from_Clipboard());
                }
            } else {
                setHeaderText(_GUI.T.LinkCrawlerBubble_update_header_from_Clipboard());
            }
        } else {
            setHeaderText(_GUI.T.LinkCrawlerBubble_update_header());
        }
        getContentComponent().update(jlc);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                pack();
                BubbleNotify.getInstance().relayout();
            }
        }.waitForEDT();
    }

    protected void requestUpdate() {
        update.resetAndStart();
    }
}
