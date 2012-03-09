package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.CrawledPackage;

public class PermanentOfflinePackage extends CrawledPackage {
    public PermanentOfflinePackage() {
        super();
        this.setCreated(System.currentTimeMillis());
    }
}
