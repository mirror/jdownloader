package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.CrawledLink;

public interface PackagizerInterface {

    public void runByFile(CrawledLink link);

    public void runByUrl(CrawledLink link);

}
