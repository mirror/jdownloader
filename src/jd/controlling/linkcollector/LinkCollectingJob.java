package jd.controlling.linkcollector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.controlling.linkcrawler.CrawledLinkModifier;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;

public class LinkCollectingJob {
    private String                jobContent;
    private String                customSourceUrl;
    private final UniqueAlltimeID uniqueAlltimeID = new UniqueAlltimeID();
    private boolean               assignJobID     = false;

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

    private final CopyOnWriteArrayList<CrawledLinkModifier> prePackagizerModifier = new CopyOnWriteArrayList<CrawledLinkModifier>();

    public List<CrawledLinkModifier> getPrePackagizerModifier() {
        return prePackagizerModifier;
    }

    public boolean addPrePackagizerModifier(CrawledLinkModifier modifier) {
        return modifier != null && prePackagizerModifier.addIfAbsent(modifier);
    }

    public boolean removePrePackagizerModifier(CrawledLinkModifier modifier) {
        return modifier != null && prePackagizerModifier.remove(modifier);
    }

    private final CopyOnWriteArrayList<CrawledLinkModifier> postPackagizerModifier = new CopyOnWriteArrayList<CrawledLinkModifier>();

    public List<CrawledLinkModifier> getPostPackagizerModifier() {
        return postPackagizerModifier;
    }

    public boolean addPostPackagizerModifier(CrawledLinkModifier modifier) {
        return modifier != null && postPackagizerModifier.addIfAbsent(modifier);
    }

    public boolean removePostPackagizerModifier(CrawledLinkModifier modifier) {
        return modifier != null && postPackagizerModifier.remove(modifier);
    }

    private boolean deepAnalyse;
    private String  crawlerPassword = null;

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
        if (StringUtils.startsWithCaseInsensitive(customSource, "http://") || StringUtils.startsWithCaseInsensitive(customSource, "https://") || StringUtils.startsWithCaseInsensitive(customSource, "ftp://")) {
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
