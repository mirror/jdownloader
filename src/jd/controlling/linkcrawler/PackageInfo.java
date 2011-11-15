package jd.controlling.linkcrawler;

import java.util.HashSet;

import org.jdownloader.controlling.UniqueID;

public class PackageInfo {
    private UniqueID uniqueId              = null;
    private boolean  autoExtractionEnabled = true;

    public boolean isAutoExtractionEnabled() {
        return autoExtractionEnabled;
    }

    private boolean autoAddEnabled;

    public boolean isAutoAddEnabled() {
        return autoAddEnabled;
    }

    public void setAutoAddEnabled(boolean autoAddEnabled) {
        this.autoAddEnabled = autoAddEnabled;
    }

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    private boolean autoStartEnabled;

    public void setAutoExtractionEnabled(boolean autoExtractionEnabled) {
        this.autoExtractionEnabled = autoExtractionEnabled;
    }

    public UniqueID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UniqueID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public HashSet<String> getExtractionPasswords() {
        return extractionPasswords;
    }

    private String          name                = null;
    private String          destinationFolder   = null;
    private String          comment             = null;
    private HashSet<String> extractionPasswords = new HashSet<String>();

}
