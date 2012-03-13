package jd.controlling.linkcollector;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcrawler.LinkCrawler;

import org.appwork.storage.config.MinTimeWeakReference;

public class LinkCollectingJob {
    private final static AtomicLong           INSTANCECOUNTER = new AtomicLong(0);
    private String                            text;
    private String                            customSourceUrl;
    private String                            customComment;
    private boolean                           autoStart       = false;
    private long                              ID              = INSTANCECOUNTER.incrementAndGet();
    private MinTimeWeakReference<LinkCrawler> linkCrawler     = null;

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public String getCustomComment() {
        return customComment;
    }

    public void setCustomComment(String customComment) {
        this.customComment = customComment;
    }

    public String getCustomSourceUrl() {
        return customSourceUrl;
    }

    public void setCustomSourceUrl(String customSource) {
        this.customSourceUrl = customSource;
    }

    private boolean autoExtract;
    private boolean deepAnalyse;

    public LinkCollectingJob() {
        this(null);
    }

    public LinkCollectingJob(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isAutoExtract() {
        return autoExtract;
    }

    public void setAutoExtract(boolean autoExtract) {
        this.autoExtract = autoExtract;
    }

    public boolean isDeepAnalyse() {
        return deepAnalyse;
    }

    public void setDeepAnalyse(boolean deepAnalyse) {
        this.deepAnalyse = deepAnalyse;
    }

    public String getExtractPassword() {
        return extractPassword;
    }

    public void setExtractPassword(String extractPassword) {
        this.extractPassword = extractPassword;
    }

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public LinkCrawler getLinkCrawler() {
        MinTimeWeakReference<LinkCrawler> lCopy = this.linkCrawler;
        if (lCopy == null) return null;
        LinkCrawler ret = lCopy.get();
        if (ret == null) {
            linkCrawler = null;
            return null;
        }
        return ret;
    }

    protected void setLinkCrawler(LinkCrawler linkCrawler) {
        if (this.linkCrawler != null) throw new IllegalStateException("LinkCrawler already set!");
        this.linkCrawler = new MinTimeWeakReference<LinkCrawler>(linkCrawler, 10000, "LinkCrawler for Job: " + ID);
    }

    private String extractPassword;
    private File   outputFolder;
    private String packageName;

}
