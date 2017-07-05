package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.parser.html.HTMLParser;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;

public class LinkCollectingJob {
    private String                jobContent;
    private String                customSourceUrl;
    private CrawledLinkModifier   crawledLinkModifierPrePackagizer = null;
    private final UniqueAlltimeID uniqueAlltimeID                  = new UniqueAlltimeID();
    private boolean               assignJobID                      = false;

    public boolean isAssignJobID() {
        return assignJobID;
    }

    public void setAssignJobID(boolean assignJobID) {
        this.assignJobID = assignJobID;
    }

    public UniqueAlltimeID getUniqueAlltimeID() {
        return uniqueAlltimeID;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ID:" + getUniqueAlltimeID());
        sb.append("|Origin:" + getOrigin().getOrigin());
        return sb.toString();
    }

    public CrawledLinkModifier getCrawledLinkModifierPrePackagizer() {
        return crawledLinkModifierPrePackagizer;
    }

    public void setCrawledLinkModifierPrePackagizer(CrawledLinkModifier crawledLinkModifierPrePackagizer) {
        this.crawledLinkModifierPrePackagizer = crawledLinkModifierPrePackagizer;
    }

    public CrawledLinkModifier getCrawledLinkModifierPostPackagizer() {
        return crawledLinkModifierPostPackagizer;
    }

    public void setCrawledLinkModifierPostPackagizer(CrawledLinkModifier crawledLinkModifierPostPackagizer) {
        this.crawledLinkModifierPostPackagizer = crawledLinkModifierPostPackagizer;
    }

    private CrawledLinkModifier crawledLinkModifierPostPackagizer = null;
    private boolean             deepAnalyse;
    private String              crawlerPassword                   = null;

    public String getCrawlerPassword() {
        return crawlerPassword;
    }

    public void setCrawlerPassword(String crawlerPassword) {
        this.crawlerPassword = crawlerPassword;
    }

    public boolean isDeepAnalyse() {
        return deepAnalyse;
    }

    public void setDeepAnalyse(boolean deepAnalyse) {
        this.deepAnalyse = deepAnalyse;
    }

    public String getCustomSourceUrl() {
        return customSourceUrl;
    }

    public void setCustomSourceUrl(String customSource) {
        final String protocol = HTMLParser.getProtocol(customSource);
        if (StringUtils.startsWithCaseInsensitive(protocol, "http") || StringUtils.startsWithCaseInsensitive(protocol, "ftp")) {
            this.customSourceUrl = customSource;
        }
    }

    public LinkCollectingJob(LinkOriginDetails origin) {
        this(origin, null);
    }

    public LinkCollectingJob(LinkOriginDetails origin, String jobContent) {
        if (origin == null) {
            throw new IllegalArgumentException("origin is null");
        }
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
