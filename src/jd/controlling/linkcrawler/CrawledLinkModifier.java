package jd.controlling.linkcrawler;

import java.util.List;

public interface CrawledLinkModifier {
    public boolean modifyCrawledLink(CrawledLink link);

    public List<CrawledLinkModifier> getSubCrawledLinkModifier(CrawledLink link);
}
