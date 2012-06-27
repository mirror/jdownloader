package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;

public class ArchiveInfoStorable implements Storable {

    private ArchiveInfo info;

    public ArchiveInfoStorable(ArchiveInfo info) {
        this.info = info;
    }

    public ArchiveInfoStorable(/* Storable */) {
        this.info = new ArchiveInfo();
    }

    public void setExtractionPasswords(List<String> extractionPasswords) {
        info.getExtractionPasswords().addAll(extractionPasswords);
    }

    public List<String> getExtractionPasswords() {
        return new ArrayList<String>(info.getExtractionPasswords());
    }

    public BooleanStatus getAutoExtract() {
        return info.getAutoExtract();
    }

    public void setAutoExtract(BooleanStatus overwriteFiles) {
        info.setAutoExtract(overwriteFiles);
    }

    public ArchiveInfo _getArchiveInfo() {
        return info;
    }
}
