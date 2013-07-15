package jd.controlling.linkcrawler;

import java.util.Collection;
import java.util.HashSet;

import org.jdownloader.extensions.extraction.BooleanStatus;

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

}
