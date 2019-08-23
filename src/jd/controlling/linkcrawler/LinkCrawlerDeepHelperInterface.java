package jd.controlling.linkcrawler;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

public interface LinkCrawlerDeepHelperInterface {
    public URLConnectionAdapter openConnection(Browser br, CrawledLink source) throws Exception;
}
