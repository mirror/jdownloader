package org.jdownloader.extensions.extraction;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.AboutConfig;
import org.jdownloader.settings.RangeValidatorMarker;

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
    @Description("Shall Extraction Extension ask you for passwords if the correct password has not been found in passwordcache?")
    boolean isAskForUnknownPasswordsEnabled();

    @AboutConfig
    void setAskForUnknownPasswordsEnabled(boolean enabled);

    @DefaultBooleanValue(true)
    @AboutConfig
    @Description("Extraction Extension autoextracts sub-archives. If you do not want this, disable this option.")
    boolean isDeepExtractionEnabled();

    @AboutConfig
    void setDeepExtractionEnabled(boolean enabled);

    @DefaultBooleanValue(false)
    boolean isDeleteArchiveFilesAfterExtraction();

    void setDeleteArchiveFilesAfterExtraction(boolean enabled);

    @Description("Info File Extension is able to create Info files for all downloaded files. Extraction Extension can remove these files")
    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isDeleteInfoFilesAfterExtraction();

    @AboutConfig
    void setDeleteInfoFilesAfterExtraction(boolean enabled);

    @DefaultBooleanValue(false)
    boolean isSubpathEnabled();

    void setSubpathEnabled(boolean enabled);

    @AboutConfig
    @DefaultStringValue("%PACKAGENAME%")
    @Description("A Blacklist is a list of regular expressions. Use a blacklist to avoid extracting certain filetypes.")
    String getSubPath();

    void setSubPath(String path);

    /**
     * Only use subpath if archive conatins more than X files
     * 
     * @return
     */

    @org.appwork.storage.config.annotations.Description("Only use subfolders if the archive contains more than *** files")
    @AboutConfig
    @DefaultIntValue(1)
    @RangeValidatorMarker(range = { 0, 30 })
    int getSubPathFilesTreshhold();

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

    @Description("A Blacklist is a list of regular expressions. Use a blacklist to avoid extracting certain filetypes.")
    @AboutConfig
    void setBlacklistPatterns(String[] patterns);
}
