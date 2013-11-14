package org.jdownloader.extensions.extraction.gui.bubble;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Timer;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class ExtractionBubble extends AbstractNotifyWindow<ExtractionBubbleContent> {

    @Override
    protected void onMouseClicked(MouseEvent m) {
        super.onMouseClicked(m);
        // JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
        // JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);

    }

    protected void onSettings() {
        JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
        ConfigurationView.getInstance().setSelectedSubPanel(BubbleNotifyConfigPanel.class);

    }

    protected long               createdTime;
    private Timer                updateTimer;
    private ExtractionController caller;

    public ExtractionBubble(final ExtractionController caller) {
        super(T._.bubble_text(), new ExtractionBubbleContent());
        this.caller = caller;
        // , _GUI._.balloon_reconnect_start_msg(), NewTheme.I().getIcon("reconnect", 32)

        updateTimer = new Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getContentComponent().update(caller, null);
            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();

    }

    @Override
    protected int getTimeout() {
        return 0;
    }

    // private void update() {
    // ReconnectBubbleContent panel = getContentComponent();
    // if (crawler instanceof JobLinkCrawler) {
    // JobLinkCrawler jlc = (JobLinkCrawler) crawler;
    //
    // LinkOrigin src = jlc.getJob().getOrigin();
    //
    // if (src == null) {
    // setHeaderText(_GUI._.LinkCrawlerBubble_update_header());
    // } else if (src == LinkOrigin.ADD_LINKS_DIALOG) {
    // setHeaderText(_GUI._.LinkCrawlerBubble_update_header());
    // } else if (src == LinkOrigin.CLIPBOARD) {
    // String txt = jlc.getJob().getText();
    // if (StringUtils.isNotEmpty(txt)) {
    // try {
    // URL url = new URL(txt);
    // setHeaderText(_GUI._.LinkCrawlerBubble_update_header_from_Clipboard_url(url.getHost()));
    // } catch (MalformedURLException e) {
    // setHeaderText(_GUI._.LinkCrawlerBubble_update_header_from_Clipboard());
    // }
    // } else {
    // setHeaderText(_GUI._.LinkCrawlerBubble_update_header_from_Clipboard());
    // }
    //
    // } else {
    // setHeaderText(_GUI._.LinkCrawlerBubble_update_header());
    // }
    // if (jlc.isRunning()) {
    //
    // panel.setText(_GUI._.LinkCrawlerBubble_update_running_linkcrawler(jlc.getCrawledLinksFoundCounter(),
    // TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - createdTime, 0)));
    // } else {
    // panel.setText(_GUI._.LinkCrawlerBubble_update_stopped_linkcrawler(jlc.getCrawledLinksFoundCounter(),
    // TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - createdTime, 0)));
    // }
    //
    // pack();
    // BubbleNotify.getInstance().relayout();
    // } else if (crawler instanceof CrawledLinkCrawler) {
    //
    // }
    // }
    @Override
    public void dispose() {
        super.dispose();
        updateTimer.stop();
    }

    public void stop() {
        System.out.println("Stop");
        startTimeout(super.getTimeout());
        getContentComponent().stop();
        updateTimer.stop();
    }

    public void refresh(final ExtractionEvent event) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                getContentComponent().update(caller, event);
                pack();
                BubbleNotify.getInstance().relayout();
            }
        };

    }

}
