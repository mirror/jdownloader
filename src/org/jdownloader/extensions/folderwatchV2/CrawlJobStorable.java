package org.jdownloader.extensions.folderwatchV2;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.BooleanStatus;

public class CrawlJobStorable implements Storable {
    private String        filename;
    private int           chunks;
    private BooleanStatus autoConfirm;
    private boolean       addOfflineLink = true;

    public boolean isAddOfflineLink() {
        return addOfflineLink;
    }

    public void setAddOfflineLink(boolean addOfflineLink) {
        this.addOfflineLink = addOfflineLink;
    }

    public static void main(String[] args) {
        System.out.println(JSonStorage.toString(new CrawlJobStorable[] { new CrawlJobStorable() }));
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getChunks() {
        return chunks;
    }

    public void setChunks(int chunks) {
        this.chunks = chunks;
    }

    public BooleanStatus getAutoConfirm() {
        return BooleanStatus.get(autoConfirm);
    }

    public void setAutoConfirm(BooleanStatus autoConfirm) {
        this.autoConfirm = autoConfirm;
    }

    public BooleanStatus getAutoStart() {
        return BooleanStatus.get(autoStart);

    }

    public void setAutoStart(BooleanStatus autoStart) {
        this.autoStart = autoStart;
    }

    public BooleanStatus getForcedStart() {
        return BooleanStatus.get(forcedStart);
    }

    public void setForcedStart(BooleanStatus forcedStart) {
        this.forcedStart = forcedStart;
    }

    public BooleanStatus getEnabled() {
        return enabled;
    }

    public void setEnabled(BooleanStatus enabled) {
        this.enabled = enabled;
    }

    private BooleanStatus autoStart;
    private BooleanStatus forcedStart;
    private BooleanStatus enabled;
    private String        text;
    private boolean       deepAnalyseEnabled = false;
    private String        packageName;
    private Priority      priority;
    private BooleanStatus extractAfterDownload;

    public Priority getPriority() {
        return priority == null ? Priority.DEFAULT : priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public BooleanStatus getExtractAfterDownload() {
        return BooleanStatus.get(extractAfterDownload);
    }

    public void setExtractAfterDownload(BooleanStatus extractAfterDownload) {
        this.extractAfterDownload = extractAfterDownload;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public String[] getExtractPasswords() {
        return extractPasswords;
    }

    public void setExtractPasswords(String[] extractPasswords) {
        this.extractPasswords = extractPasswords;
    }

    public String getDownloadPassword() {
        return downloadPassword;
    }

    public void setDownloadPassword(String downloadPassword) {
        this.downloadPassword = downloadPassword;
    }

    public boolean isOverwritePackagizerEnabled() {
        return overwritePackagizerEnabled;
    }

    public void setOverwritePackagizerEnabled(boolean overwritePackagizerEnabled) {
        this.overwritePackagizerEnabled = overwritePackagizerEnabled;
    }

    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private String   downloadFolder;
    private String[] extractPasswords;
    private String   downloadPassword;
    private boolean  overwritePackagizerEnabled = true;

    public boolean isDeepAnalyseEnabled() {
        return deepAnalyseEnabled;
    }

    public void setDeepAnalyseEnabled(boolean deepAnalyseEnabled) {
        this.deepAnalyseEnabled = deepAnalyseEnabled;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public JobType getType() {
        return type == null ? JobType.NORMAL : type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public static enum JobType {
        NORMAL

    }

    private JobType type = JobType.NORMAL;

    public CrawlJobStorable(/* storable */) {

    }
}
