package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.JobLinkCrawlerStorable;

import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;

public class JobLinkCrawlerAPIStorable extends JobLinkCrawlerStorable implements Storable {

    public JobLinkCrawlerAPIStorable(/* Storable */) {

    }

    public JobLinkCrawlerAPIStorable(final LinkCrawlerJobsQueryStorable query, final JobLinkCrawler crawler) {
        setCrawling(crawler.isRunning());
        setChecking(crawler.getLinkChecker().isRunning());
        setCrawlerId(crawler.getUniqueAlltimeID().getID());
        setJobId(crawler.getJob().getUniqueAlltimeID().getID());
        if (query.isCollectorInfo()) {
            setBroken(crawler.getBrokenLinksFoundCounter());
            setUnhandled(crawler.getUnhandledLinksFoundCounter());
            setFiltered(crawler.getFilteredLinksFoundCounter());
            setCrawled(crawler.getCrawledLinksFoundCounter());
        }
    }

}
