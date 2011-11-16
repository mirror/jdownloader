package jd.controlling.linkcrawler;

import java.util.HashSet;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueID;

public class PackageInfo {
    private UniqueID uniqueId              = null;
    private boolean  autoExtractionEnabled = true;

    public boolean isAutoExtractionEnabled() {
        return autoExtractionEnabled;
    }

    public void setAutoExtractionEnabled(boolean autoExtractionEnabled) {
        this.autoExtractionEnabled = autoExtractionEnabled;
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

    private HashSet<String> extractionPasswords = new HashSet<String>();
    private String          name                = null;
    private String          destinationFolder   = null;
    private String          comment             = null;

    /**
     * Returns a packageID or null, of no id specific values are set. if this
     * method returns a value !=null, it should get an own package, which is not
     * part of autopackaging.
     * 
     * @return
     */
    public String createPackageID() {
        StringBuilder sb = new StringBuilder();
        if (getUniqueId() != null) {
            sb.append(getUniqueId().toString());
        }
        if (!StringUtils.isEmpty(getDestinationFolder())) sb.append(getDestinationFolder());
        if (!StringUtils.isEmpty(getName())) sb.append(getName());
        return sb.length() == 0 ? null : sb.toString();
    }

}
