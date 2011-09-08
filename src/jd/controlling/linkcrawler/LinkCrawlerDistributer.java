package jd.controlling.linkcrawler;

import jd.plugins.DownloadLink;

public interface LinkCrawlerDistributer {

    public void distribute(DownloadLink... links);
}
