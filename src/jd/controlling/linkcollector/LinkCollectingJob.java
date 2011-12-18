package jd.controlling.linkcollector;

import java.io.File;

public class LinkCollectingJob {
    private String text;
    private String customSourceUrl;
    private String customComment;

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

    private String extractPassword;
    private File   outputFolder;
    private String packageName;

}
