package jd.controlling.linkcrawler;

import java.util.EventListener;

public interface LinkCrawlerListener extends EventListener {

    public void onLinkCrawlerEvent(LinkCrawlerEvent event);
}
