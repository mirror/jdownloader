package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.CrawledPackage;

public class OfflineCrawledPackage extends CrawledPackage {
    public OfflineCrawledPackage() {
        super();
        this.setCreated(System.currentTimeMillis());
    }
}
