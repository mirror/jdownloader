package jd.controlling.linkcollector;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.event.queue.QueueAction;

public class LinkCollectingInformation {

    private final JobLinkCrawler jobLinkCrawler;

    public boolean isAborted() {
        return jobLinkCrawler.isAborted();
    }

    public LinkCollectingInformation(final JobLinkCrawler jobLinkCrawler) {
        this.jobLinkCrawler = jobLinkCrawler;
    }

    public JobLinkCrawler getLinkCrawler() {
        return jobLinkCrawler;
    }

    public LinkChecker<CrawledLink> getLinkChecker() {
        return jobLinkCrawler.getLinkChecker();
    }

    protected void enqueu(QueueAction<?, ?> queueAction, CrawledLink link) {
        jobLinkCrawler.enqueuOrDequeue(link, true);
    }

    protected void dequeu(QueueAction<?, ?> queueAction, CrawledLink link) {
        jobLinkCrawler.enqueuOrDequeue(link, false);
    }

}
