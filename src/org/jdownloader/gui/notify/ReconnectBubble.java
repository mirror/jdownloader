package org.jdownloader.gui.notify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Timer;

import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.controlling.reconnect.ReconnecterListener;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class ReconnectBubble extends AbstractNotifyWindow<ReconnectBubbleContent> implements ReconnecterListener {

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

    protected long createdTime;
    private Timer  updateTimer;

    public ReconnectBubble() {
        super(_GUI._.balloon_reconnect(), new ReconnectBubbleContent());
        // , _GUI._.balloon_reconnect_start_msg(), NewTheme.I().getIcon("reconnect", 32)
        Reconnecter.getInstance().getEventSender().addListener(this, true);
        updateTimer = new Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getContentComponent().update();
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

    @Override
    public void onAfterReconnect(final ReconnecterEvent event) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                BasicNotify no = null;
                ReconnectResult result = event.getResult();
                if (result == null) result = ReconnectResult.FAILED;

                getContentComponent().onResult(result);
                pack();
                BubbleNotify.getInstance().relayout();
                getContentComponent().stop();
                getContentComponent().update();
                startTimeout(ReconnectBubble.super.getTimeout());
                updateTimer.stop();
            }
        };

    }

    @Override
    public void onBeforeReconnect(ReconnecterEvent event) {
    }

}
