package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.io.File;

public class CrawlerJob {
    private String  text;
    private boolean autoExtract;
    private boolean deepAnalyse;

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
