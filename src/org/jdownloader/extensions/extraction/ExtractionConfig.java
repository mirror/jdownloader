package org.jdownloader.extensions.extraction;

import java.util.ArrayList;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface ExtractionConfig extends ExtensionConfigInterface {
    @DefaultStringArrayValue(value = {})
    @AboutConfig
    @Description("A Blacklist is a list of regular expressions. Use a blacklist to avoid extracting certain filetypes.")
    String[] getBlacklistPatterns();

    @DefaultEnumValue("HIGH")
    @AboutConfig
    CPUPriority getCPUPriority();

    @AboutConfig
    @Description("Absolute path to the folder where all archives should be extracted to")
    String getCustomExtractionPath();

    @AboutConfig
    @DefaultStringValue("%PACKAGENAME%")
    String getSubPath();

    @DefaultJsonObject("[]")
    @AboutConfig
    @Description("A List of passwords for automatic extraction of password protected archives.")
    ArrayList<String> getPasswordList();

    void setPasswordList(ArrayList<String> list);

    @DefaultBooleanValue(false)
    boolean isOldPWListImported();

    void setOldPWListImported(boolean b);

    /**
     * Only use subpath if archive conatins more than X files
     * 
     * @return
     */

    @org.appwork.storage.config.annotations.Description("Only use subfolders if the archive contains more than *** files")
    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 0, max = 30)
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

    @Description("Delete archive DownloadLinks after successfull extraction?")
    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isDeleteArchiveDownloadlinksAfterExtraction();

    void setDeleteArchiveDownloadlinksAfterExtraction(boolean b);

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
    @DefaultBooleanValue(true)
    @Description("Use original filedate if possible")
    boolean isUseOriginalFileDate();

    void setAskForUnknownPasswordsEnabled(boolean enabled);

    void setBlacklistPatterns(String[] patterns);

    void setCPUPriority(CPUPriority priority);

    void setCustomExtractionPath(String path);

    void setCustomExtractionPathEnabled(boolean enabled);

    void setDeepExtractionEnabled(boolean enabled);

    void setDeleteArchiveFilesAfterExtraction(boolean enabled);

    void setDeleteInfoFilesAfterExtraction(boolean enabled);

    void setOverwriteExistingFilesEnabled(boolean enabled);

    void setSubPath(String path);

    void setSubpathEnabled(boolean enabled);

    void setSubpathEnabledIfAllFilesAreInAFolder(boolean enabled);

    void setSubPathFilesTreshhold(int treshold);

    @AboutConfig
    @Description("max kbytes the extractor may test for finding correct password when no signature is found")
    @SpinnerValidator(min = 10, max = Integer.MAX_VALUE)
    @DefaultIntValue(1000)
    int getMaxPasswordCheckSize();

    void setMaxPasswordCheckSize(int size);

    @AboutConfig
    @Description("max buffer size for write operations in kb")
    @SpinnerValidator(min = 100, max = 102400)
    @DefaultIntValue(2000)
    int getBufferSize();

    void setBufferSize(int buffer);

    void setUseOriginalFileDate(boolean enabled);

    @DefaultBooleanValue(true)
    @AboutConfig
    @Description("This option improves password find speed a lot, but may result in finding errors.")
    boolean isPasswordFindOptimizationEnabled();

    void setPasswordFindOptimizationEnabled(boolean b);
}