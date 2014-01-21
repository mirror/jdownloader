package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.CrawledLinkModifier;

public class LinkCollectingJob {
    private String              jobContent;
    private String              customSourceUrl;
    private CrawledLinkModifier crawledLinkModifier = null;
    private boolean             deepAnalyse;

    public CrawledLinkModifier getCrawledLinkModifier() {
        return crawledLinkModifier;
    }

    public boolean isDeepAnalyse() {
        return deepAnalyse;
    }

    public void setDeepAnalyse(boolean deepAnalyse) {
        this.deepAnalyse = deepAnalyse;
    }

    public void setCrawledLinkModifier(CrawledLinkModifier crawledLinkModifier) {
        this.crawledLinkModifier = crawledLinkModifier;
    }

    public String getCustomSourceUrl() {
        return customSourceUrl;
    }

    public void setCustomSourceUrl(String customSource) {
        this.customSourceUrl = customSource;
    }

    public LinkCollectingJob(LinkOriginDetails origin) {
        this(origin, null);
    }

    public LinkCollectingJob(LinkOriginDetails origin, String jobContent) {
        this.jobContent = jobContent;
        this.origin = origin;
    }

    public String getText() {
        return jobContent;
    }

    public void setText(String text) {
        this.jobContent = text;
    }

    private final LinkOriginDetails origin;

    public LinkOriginDetails getOrigin() {
        return origin;
    }

}
