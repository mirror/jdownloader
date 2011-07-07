package org.jdownloader.extensions.extraction;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RangeValidatorMarker;

public interface ExtractionConfig extends ExtensionConfigInterface {
    @DefaultStringArrayValue(value = {})
    @AboutConfig
    String[] getBlacklistPatterns();

    @DefaultEnumValue("org.jdownloader.extensions.extraction.CPUPriority.HIGH")
    @AboutConfig
    CPUPriority getCPUPriority();

    @AboutConfig
    @Description("Absolute path to the folder where all archives should be extracted to")
    String getCustomExtractionPath();

    @AboutConfig
    @DefaultStringValue("%PACKAGENAME%")
    @Description("A Blacklist is a list of regular expressions. Use a blacklist to avoid extracting certain filetypes.")
    String getSubPath();

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

    @DefaultBooleanValue(true)
    @AboutConfig
    @Description("Shall Extraction Extension ask you for passwords if the correct password has not been found in passwordcache?")
    boolean isAskForUnknownPasswordsEnabled();

    @Description("Enabled usage of custom extractionpathes")
    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isCustomExtractionPathEnabled();

    @DefaultBooleanValue(true)
    @AboutConfig
    @Description("Extraction Extension autoextracts sub-archives. If you do not want this, disable this option.")
    boolean isDeepExtractionEnabled();

    @Description("Delete archives after successfull extraction?")
    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isDeleteArchiveFilesAfterExtraction();

    @Description("Info File Extension is able to create Info files for all downloaded files. Extraction Extension can remove these files")
    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isDeleteInfoFilesAfterExtraction();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverwriteExistingFilesEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isSubpathEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isSubpathEnabledIfAllFilesAreInAFolder();

    @AboutConfig
    void setAskForUnknownPasswordsEnabled(boolean enabled);

    @Description("A Blacklist is a list of regular expressions. Use a blacklist to avoid extracting certain filetypes.")
    @AboutConfig
    void setBlacklistPatterns(String[] patterns);

    @AboutConfig
    void setCPUPriority(CPUPriority priority);

    void setCustomExtractionPath(String path);

    void setCustomExtractionPathEnabled(boolean enabled);

    @AboutConfig
    void setDeepExtractionEnabled(boolean enabled);

    void setDeleteArchiveFilesAfterExtraction(boolean enabled);

    @AboutConfig
    void setDeleteInfoFilesAfterExtraction(boolean enabled);

    void setOverwriteExistingFilesEnabled(boolean enabled);

    void setSubPath(String path);

    void setSubpathEnabled(boolean enabled);

    void setSubpathEnabledIfAllFilesAreInAFolder(boolean b);

    void setSubPathFilesTreshhold(int treshold);
}
