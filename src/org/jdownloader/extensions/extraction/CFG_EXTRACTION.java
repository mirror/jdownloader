package org.jdownloader.extensions.extraction;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.storage.config.handler.StringListHandler;
import org.appwork.utils.Application;

public class CFG_EXTRACTION {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(ExtractionConfig.class, "Application.getResource(\"cfg/\" + " + ExtractionExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.extraction.ExtractionConfig
    public static final ExtractionConfig                 CFG                                                              = JsonConfig.create(Application.getResource("cfg/" + ExtractionExtension.class.getName()), ExtractionConfig.class);
    public static final StorageHandler<ExtractionConfig> SH                                                               = (StorageHandler<ExtractionConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    /**
     * Extraction Extension autoextracts sub-archives. If you do not want this, disable this option.
     **/
    public static final BooleanKeyHandler                DEEP_EXTRACTION_ENABLED                                          = SH.getKeyHandler("DeepExtractionEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Use original filedate if possible
     **/
    public static final BooleanKeyHandler                USE_ORIGINAL_FILE_DATE                                           = SH.getKeyHandler("UseOriginalFileDate", BooleanKeyHandler.class);
    // []
    /**
     * A List of passwords for automatic extraction of password protected archives.
     **/
    public static final ObjectKeyHandler                 PASSWORD_LIST                                                    = SH.getKeyHandler("PasswordList", ObjectKeyHandler.class);
    // %PACKAGENAME%
    public static final StringKeyHandler                 SUB_PATH                                                         = SH.getKeyHandler("SubPath", StringKeyHandler.class);
    // 1
    /**
     * Only use subfolders if the archive contains more than *** files
     **/
    public static final IntegerKeyHandler                SUB_PATH_FILES_TRESHHOLD                                         = SH.getKeyHandler("SubPathFilesTreshhold", IntegerKeyHandler.class);
    // false
    /**
     * Delete archive DownloadLinks after successful extraction?
     **/
    public static final BooleanKeyHandler                DELETE_ARCHIVE_DOWNLOADLINKS_AFTER_EXTRACTION                    = SH.getKeyHandler("DeleteArchiveDownloadlinksAfterExtraction", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                FRESH_INSTALL                                                    = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                GUI_ENABLED                                                      = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // false
    /**
     * Crashlog files in logs/extraction/... can be used to debug extraction
     **/
    public static final BooleanKeyHandler                KEEP_CRASHLOG_FILES_ENABLED                                      = SH.getKeyHandler("KeepCrashlogFilesEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                OVERWRITE_EXISTING_FILES_ENABLED                                 = SH.getKeyHandler("OverwriteExistingFilesEnabled", BooleanKeyHandler.class);
    // null
    /**
     * Absolute path to the folder where all archives should be extracted to
     **/
    public static final StringKeyHandler                 CUSTOM_EXTRACTION_PATH                                           = SH.getKeyHandler("CustomExtractionPath", StringKeyHandler.class);
    // 2000
    /**
     * max buffer size for write operations in kb
     **/
    public static final IntegerKeyHandler                BUFFER_SIZE                                                      = SH.getKeyHandler("BufferSize", IntegerKeyHandler.class);
    // false
    /**
     * Delete archives after successful extraction?
     **/
    public static final BooleanKeyHandler                DELETE_ARCHIVE_FILES_AFTER_EXTRACTION                            = SH.getKeyHandler("DeleteArchiveFilesAfterExtraction", BooleanKeyHandler.class);
    // [Ljava.lang.String;@1dc58fb9
    /**
     * A Blacklist is a list of regular expressions. Use a blacklist to avoid extracting certain filetypes.
     **/
    public static final StringListHandler                BLACKLIST_PATTERNS                                               = SH.getKeyHandler("BlacklistPatterns", StringListHandler.class);
    // HIGH
    public static final EnumKeyHandler                   CPUPRIORITY                                                      = SH.getKeyHandler("CPUPriority", EnumKeyHandler.class);
    // false
    public static final BooleanKeyHandler                SUBPATH_ENABLED                                                  = SH.getKeyHandler("SubpathEnabled", BooleanKeyHandler.class);
    // true
    /**
     * This option improves password find speed a lot, but may result in finding errors.
     **/
    public static final BooleanKeyHandler                PASSWORD_FIND_OPTIMIZATION_ENABLED                               = SH.getKeyHandler("PasswordFindOptimizationEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                ENABLED                                                          = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                SUBPATH_ENABLED_IF_ALL_FILES_ARE_IN_AFOLDER                      = SH.getKeyHandler("SubpathEnabledIfAllFilesAreInAFolder", BooleanKeyHandler.class);
    // true
    /**
     * Shall Extraction Extension ask you for passwords if the correct password has not been found in passwordcache?
     **/
    public static final BooleanKeyHandler                ASK_FOR_UNKNOWN_PASSWORDS_ENABLED                                = SH.getKeyHandler("AskForUnknownPasswordsEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                OLD_PWLIST_IMPORTED                                              = SH.getKeyHandler("OldPWListImported", BooleanKeyHandler.class);
    // 1024000
    /**
     * max bytes the extractor may test for finding correct password when no signature is found
     **/
    public static final IntegerKeyHandler                MAX_CHECKED_FILE_SIZE_DURING_OPTIMIZED_PASSWORD_FINDING_IN_BYTES = SH.getKeyHandler("MaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes", IntegerKeyHandler.class);
    // false
    /**
     * Info File Extension is able to create Info files for all downloaded files. Extraction Extension can remove these files
     **/
    public static final BooleanKeyHandler                DELETE_INFO_FILES_AFTER_EXTRACTION                               = SH.getKeyHandler("DeleteInfoFilesAfterExtraction", BooleanKeyHandler.class);
    // false
    /**
     * Enabled usage of custom extractionpathes
     **/
    public static final BooleanKeyHandler                CUSTOM_EXTRACTION_PATH_ENABLED                                   = SH.getKeyHandler("CustomExtractionPathEnabled", BooleanKeyHandler.class);
}