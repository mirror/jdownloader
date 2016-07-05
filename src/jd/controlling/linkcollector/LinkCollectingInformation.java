package jd.controlling.linkcollector;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcrawler.CrawledLink;

public class LinkCollectingInformation {

    private final JobLinkCrawler jobLinkCrawler;

    public long getCollectingID() {
        return jobLinkCrawler.getCollectingID();
    }

    public boolean isCollectingIDValid() {
        return getCollectingID() == getLinkCrawler().getLinkCollector().getCollectingID();
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

}
