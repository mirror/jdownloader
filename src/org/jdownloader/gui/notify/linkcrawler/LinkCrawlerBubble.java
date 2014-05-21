package org.jdownloader.gui.notify.linkcrawler;

import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
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
    
    private final WeakReference<LinkCollectorCrawler> crawler;
    
    public LinkCrawlerBubble(LinkCrawlerBubbleSupport linkCrawlerBubbleSupport, LinkCollectorCrawler crawler) {
        super(linkCrawlerBubbleSupport, _GUI._.balloon_new_links(), new LinkCrawlerBubbleContent());
        this.crawler = new WeakReference<LinkCollectorCrawler>(crawler);
    }
    
    @Override
    protected int getTimeout() {
        return 0;
    }
    
    public int getSuperTimeout() {
        return super.getTimeout();
    }
    
    protected void update() {
        LinkCollectorCrawler crwl = crawler.get();
        if (crwl != null) {
            if (crwl instanceof JobLinkCrawler) {
                JobLinkCrawler jlc = (JobLinkCrawler) crwl;
                
                LinkOrigin src = jlc.getJob().getOrigin().getOrigin();
                
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
                getContentComponent().update(jlc);
                
                pack();
                BubbleNotify.getInstance().relayout();
            }
        }
    }
    
}
