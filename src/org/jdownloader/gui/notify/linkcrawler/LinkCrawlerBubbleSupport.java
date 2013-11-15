package org.jdownloader.gui.notify.linkcrawler;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.Element;
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

    @Override
    public List<Element> getElements() {
        return elements;
    }

    @Override
    public void onLinkCrawlerAdded(final LinkCollectorCrawler parameter) {
        if (!CFG_BUBBLE.BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED.isEnabled()) return;

        // it is important to wait. we could miss events if we do not wait
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                LinkCrawlerBubble no = new LinkCrawlerBubble(LinkCrawlerBubbleSupport.this, parameter);
                parameter.getEventSender().addListener(no, true);

            }
        }.waitForEDT();

    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {

    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
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

}
