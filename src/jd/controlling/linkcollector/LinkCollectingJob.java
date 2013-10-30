package jd.controlling.linkcollector;

import java.io.File;
import java.util.Set;

import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.BooleanStatus;

public class LinkCollectingJob {
    private String   text;
    private String   customSourceUrl;
    private String   customComment;
    private Priority priority = Priority.DEFAULT;

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    private String downloadPassword;

    public String getDownloadPassword() {
        return downloadPassword;
    }

    public void setDownloadPassword(String downloadPassword) {
        this.downloadPassword = downloadPassword;
    }

    private boolean autoStart = false;

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

    private BooleanStatus autoExtract = BooleanStatus.UNSET;
    private boolean       deepAnalyse;

    public LinkCollectingJob(LinkOrigin origin) {
        this(origin, null);
    }

    public LinkCollectingJob(LinkOrigin origin, String text) {
        this.text = text;
        this.origin = origin;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BooleanStatus getAutoExtract() {
        return autoExtract;
    }

    public void setAutoExtract(BooleanStatus autoExtract) {
        this.autoExtract = autoExtract;
    }

    public boolean isDeepAnalyse() {
        return deepAnalyse;
    }

    public void setDeepAnalyse(boolean deepAnalyse) {
        this.deepAnalyse = deepAnalyse;
    }

    public Set<String> getExtractPasswords() {
        return extractPasswords;
    }

    public void setExtractPasswords(Set<String> extractPasswords) {
        this.extractPasswords = extractPasswords;
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

    private Set<String> extractPasswords;
    private File        outputFolder;
    private String      packageName;

    private LinkOrigin  origin;

    public LinkOrigin getOrigin() {
        return origin;
    }

}
