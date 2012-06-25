package org.jdownloader.extensions.extraction;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultEnumValue;

public interface ArchiveSettings extends ConfigInterface {
    public static enum BooleanStatus {
        UNSET,
        TRUE,
        FALSE
    }

    public String getPassword();

    public void setPassword(String password);

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
}
