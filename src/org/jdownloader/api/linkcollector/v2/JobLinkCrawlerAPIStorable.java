package org.jdownloader.api.linkcollector.v2;

import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.JobLinkCrawlerStorable;

public class JobLinkCrawlerAPIStorable extends JobLinkCrawlerStorable implements Storable {

    public JobLinkCrawlerAPIStorable(/* Storable */) {

    }

    public JobLinkCrawlerAPIStorable(final LinkCrawlerJobsQueryStorable query, final JobLinkCrawler job) {
        setCrawling(job.isRunning());
        setChecking(job.getLinkChecker().isRunning());
        setJobId(job.getUniqueAlltimeID().getID());
        if (query.isCollectorInfo()) {
            setBroken(job.getBrokenLinksFoundCounter());
            setUnhandled(job.getUnhandledLinksFoundCounter());
            setFiltered(job.getFilteredLinksFoundCounter());
            setCrawled(job.getCrawledLinksFoundCounter());
        }
    }

}
