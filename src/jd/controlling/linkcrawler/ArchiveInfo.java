package jd.controlling.linkcrawler;

import java.util.Collection;
import java.util.HashSet;

import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;

public class ArchiveInfo {
    public ArchiveInfo() {

    }

    private HashSet<String> extractionPasswords = new HashSet<String>();
    private BooleanStatus   autoExtract         = BooleanStatus.UNSET;

    public void setExtractionPasswords(HashSet<String> extractionPasswords) {
        this.extractionPasswords = extractionPasswords;
    }

    public Collection<String> getExtractionPasswords() {

        return extractionPasswords;
    }

    // private String extractPath;
    // public String getExtractPath() {
    // return extractPath;
    // }
    //
    // public void setExtractPath(String extractPath) {
    // this.extractPath=extractPath;
    // }

    // public BooleanStatus getOverwriteFiles() {
    // return null;
    // }
    //
    // public void setOverwriteFiles(BooleanStatus overwriteFiles) {
    // }

    public BooleanStatus getAutoExtract() {
        return autoExtract;
    }

    public void setAutoExtract(BooleanStatus overwriteFiles) {
        autoExtract = overwriteFiles;
    }

    public ArchiveInfo migrate(ArchiveInfo ai) {
        if (ai == null) return this;
        if (autoExtract == BooleanStatus.UNSET) autoExtract = ai.getAutoExtract();
        getExtractionPasswords().addAll(ai.getExtractionPasswords());
        return this;
    }

    // public BooleanStatus getRemoveFilesAfterExtraction() {
    // return null;
    // }
    //
    // public void setRemoveFilesAfterExtraction(BooleanStatus overwriteFiles) {
    // }
    //
    // public BooleanStatus getRemoveDownloadLinksAfterExtraction() {
    // return null;
    // }
    //
    // public void setRemoveDownloadLinksAfterExtraction(BooleanStatus overwriteFiles) {
    // }
}
