package org.jdownloader.extensions.extraction;

import java.util.HashSet;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultEnumValue;

public interface ArchiveSettings extends ConfigInterface {
    public static enum BooleanStatus {
        UNSET,
        TRUE,
        FALSE;

        public Boolean getBoolean() {
            switch (this) {
            case FALSE:
                return Boolean.FALSE;
            case TRUE:
                return Boolean.TRUE;
            default:
                return null;
            }
        }
    }

    public HashSet<String> getPasswords();

    public void setPasswords(HashSet<String> passwords);

    public void setFinalPassword(String password);

    public String getFinalPassword();

    public String getExtractPath();

    public void setExtractPath(String extractPath);

    @DefaultEnumValue("UNSET")
    public BooleanStatus getOverwriteFiles();

    public void setOverwriteFiles(BooleanStatus overwriteFiles);

    @DefaultEnumValue("UNSET")
    public BooleanStatus getAutoExtract();

    public void setAutoExtract(BooleanStatus overwriteFiles);

    @DefaultEnumValue("UNSET")
    public BooleanStatus getRemoveFilesAfterExtraction();

    public void setRemoveFilesAfterExtraction(BooleanStatus overwriteFiles);

    @DefaultEnumValue("UNSET")
    public BooleanStatus getRemoveDownloadLinksAfterExtraction();

    public void setRemoveDownloadLinksAfterExtraction(BooleanStatus overwriteFiles);

    public void setExtractionInfo(ExtractionInfo extractionInfo);

    public ExtractionInfo getExtractionInfo();

    public java.util.List<ArchiveItem> getArchiveItems();

    public void setArchiveItems(java.util.List<ArchiveItem> files);
}
