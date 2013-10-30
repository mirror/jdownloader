package org.jdownloader.gui.notify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Timer;

import jd.controlling.linkcollector.LinkCollector.CrawledLinkCrawler;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.event.LinkCollectorCrawlerListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LinkCrawlerBubble extends AbstractNotifyWindow<LinkCrawlerBubbleContent> implements LinkCollectorCrawlerListener {

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

    private LinkCollectorCrawler crawler;
    protected long               createdTime;
    private boolean              registered = false;

    public LinkCrawlerBubble(LinkCollectorCrawler parameter) {
        super(_GUI._.balloon_new_links(), new LinkCrawlerBubbleContent());
        this.crawler = parameter;

    }

    @Override
    protected int getTimeout() {
        return 0;
    }

    private void update() {
        LinkCrawlerBubbleContent panel = getContentComponent();
        if (crawler instanceof JobLinkCrawler) {
            JobLinkCrawler jlc = (JobLinkCrawler) crawler;

            LinkOrigin src = jlc.getJob().getOrigin();

            if (src == null) {
                setHeaderText(_GUI._.LinkCrawlerBubble_update_header());
            } else if (src == LinkOrigin.ADD_LINKS_DIALOG) {
                setHeaderText(_GUI._.LinkCrawlerBubble_update_header());
            } else if (src == LinkOrigin.CLIPBOARD) {
                String txt = jlc.getJob().getText();
                if (StringUtils.isNotEmpty(txt)) {
                    try {
                        URL url = new URL(txt);
                        setHeaderText(_GUI._.LinkCrawlerBubble_update_header_from_Clipboard_url(url.getHost()));
                    } catch (MalformedURLException e) {
                        setHeaderText(_GUI._.LinkCrawlerBubble_update_header_from_Clipboard());
                    }
                } else {
                    setHeaderText(_GUI._.LinkCrawlerBubble_update_header_from_Clipboard());
                }

            } else {
                setHeaderText(_GUI._.LinkCrawlerBubble_update_header());
            }
            if (jlc.isRunning()) {

                panel.setText(_GUI._.LinkCrawlerBubble_update_running_linkcrawler(jlc.getCrawledLinksFoundCounter(), TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - createdTime, 0)));
            } else {
                panel.setText(_GUI._.LinkCrawlerBubble_update_stopped_linkcrawler(jlc.getCrawledLinksFoundCounter(), TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - createdTime, 0)));
            }

            pack();
            BubbleNotify.getInstance().relayout();
        } else if (crawler instanceof CrawledLinkCrawler) {

        }
    }

    @Override
    public void onProcessingCrawlerPlugin(final LinkCollectorCrawler caller, CrawledLink parameter) {

        register(caller);

    }

    protected void register(final LinkCollectorCrawler caller) {
        if (registered) return;
        registered = true;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!isVisible() && !isClosed()) {

                    BubbleNotify.getInstance().show(LinkCrawlerBubble.this);
                    createdTime = System.currentTimeMillis();
                    final Timer t = new Timer(1000, new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (!isVisible() || isClosed()) {
                                getContentComponent().crawlerStopped();
                                ((Timer) e.getSource()).stop();
                                return;

                            }
                            update();
                            if (!caller.isRunning()) {
                                getContentComponent().crawlerStopped();
                                ((Timer) e.getSource()).stop();
                                startTimeout(LinkCrawlerBubble.super.getTimeout());

                                return;

                            }

                        }

                    });
                    t.setInitialDelay(0);
                    t.setRepeats(true);
                    t.start();

                }
            }
        };
    }

    @Override
    public void onProcessingHosterPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
        register(caller);
    }

    @Override
    public void onProcessingContainerPlugin(LinkCollectorCrawler caller, CrawledLink parameter) {
        register(caller);
    }

}
