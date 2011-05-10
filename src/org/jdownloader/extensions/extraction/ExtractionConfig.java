package org.jdownloader.extensions.extraction;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.jdownloader.settings.AboutConfig;

public interface ExtractionConfig extends ExtensionConfigInterface {
    @DefaultBooleanValue(false)
    boolean isCustomExtractionPathEnabled();

    void setCustomExtractionPathEnabled(boolean enabled);

    String getCustomExtractionPath();

    void setCustomExtractionPath(String path);

    @DefaultBooleanValue(true)
    boolean isOverwriteExistingFilesEnabled();

    void setOverwriteExistingFilesEnabled(boolean enabled);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isAskForUnknownPasswordsEnabled();

    @AboutConfig
    void setAskForUnknownPasswordsEnabled(boolean enabled);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isDeepExtractionEnabled();

    @AboutConfig
    void setDeepExtractionEnabled(boolean enabled);

    @DefaultBooleanValue(false)
    boolean isDeleteArchiveFilesAfterExtraction();

    void setDeleteArchiveFilesAfterExtraction(boolean enabled);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isDeleteInfoFilesAfterExtraction();

    @AboutConfig
    void setDeleteInfoFilesAfterExtraction(boolean enabled);

    @DefaultBooleanValue(false)
    boolean isSubpathEnabled();

    void setSubpathEnabled(boolean enabled);

    @DefaultStringValue("%PACKAGENAME%")
    String getSubPath();

    void setSubPath(String path);

    /**
     * Only use subpath if archive conatins more than X files
     * 
     * @return
     */
    @DefaultIntValue(1)
    int getSubPathFilesTreshold();

    void setSubPathFilesTreshhold(int treshold);

    @DefaultBooleanValue(false)
    boolean isSubpathEnabledIfAllFilesAreInAFolder();

    void setSubpathEnabledIfAllFilesAreInAFolder(boolean b);

    @DefaultEnumValue("org.jdownloader.extensions.extraction.CPUPriority.HIGH")
    @AboutConfig
    CPUPriority getCPUPriority();

    @AboutConfig
    void setCPUPriority(CPUPriority priority);

    @DefaultStringArrayValue(value = {})
    @AboutConfig
    String[] getBlacklistPatterns();

    @AboutConfig
    void setBlacklistPatterns(String[] patterns);
}
