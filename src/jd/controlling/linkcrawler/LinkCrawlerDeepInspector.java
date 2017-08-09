package jd.controlling.linkcrawler;

import java.util.List;

import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerGeneration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;

public interface LinkCrawlerDeepInspector {
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection);

    public List<CrawledLink> deepInspect(LinkCrawler lc, final LinkCrawlerGeneration generation, Browser br, URLConnectionAdapter urlConnection, final CrawledLink link) throws Exception;
}
