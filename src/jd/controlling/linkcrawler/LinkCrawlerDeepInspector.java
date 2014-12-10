package jd.controlling.linkcrawler;

import java.util.List;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

public interface LinkCrawlerDeepInspector {

    public List<CrawledLink> deepInspect(LinkCrawler lc, Browser br, URLConnectionAdapter urlConnection, final CrawledLink link) throws Exception;
}
