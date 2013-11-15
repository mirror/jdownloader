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

    public static final BooleanKeyHandler                BUBBLE_CONTENT_EXTRACT_TO_FOLDER_VISIBLE                         = SH.getKeyHandler("BubbleContentExtractToFolderVisible", BooleanKeyHandler.class);

    /**
     * Extraction Extension autoextracts sub-archives. If you do not want this, disable this option.
     **/
    public static final BooleanKeyHandler                DEEP_EXTRACTION_ENABLED                                          = SH.getKeyHandler("DeepExtractionEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                BUBBLE_CONTENT_CIRCLE_PROGRESS_VISIBLE                           = SH.getKeyHandler("BubbleContentCircleProgressVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                BUBBLE_CONTENT_DURATION_VISIBLE                                  = SH.getKeyHandler("BubbleContentDurationVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                BUBBLE_CONTENT_CURRENT_FILE_VISIBLE                              = SH.getKeyHandler("BubbleContentCurrentFileVisible", BooleanKeyHandler.class);

    /**
     * Only use subfolders if the archive ROOT contains at least *** files
     **/
    public static final IntegerKeyHandler                SUB_PATH_MIN_FILES_TRESHHOLD                                     = SH.getKeyHandler("SubPathMinFilesTreshhold", IntegerKeyHandler.class);

    /**
     * Only use subfolders if the archive ROOT contains at least *** folders
     **/
    public static final IntegerKeyHandler                SUB_PATH_MIN_FOLDERS_TRESHHOLD                                   = SH.getKeyHandler("SubPathMinFoldersTreshhold", IntegerKeyHandler.class);

    /**
     * Delete archive DownloadLinks after successful extraction?
     **/
    public static final BooleanKeyHandler                DELETE_ARCHIVE_DOWNLOADLINKS_AFTER_EXTRACTION                    = SH.getKeyHandler("DeleteArchiveDownloadlinksAfterExtraction", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                FRESH_INSTALL                                                    = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                BUBBLE_CONTENT_STATUS_VISIBLE                                    = SH.getKeyHandler("BubbleContentStatusVisible", BooleanKeyHandler.class);

    /**
     * Absolute path to the folder where all archives should be extracted to
     **/
    public static final StringKeyHandler                 CUSTOM_EXTRACTION_PATH                                           = SH.getKeyHandler("CustomExtractionPath", StringKeyHandler.class);

    public static final BooleanKeyHandler                BUBBLE_CONTENT_ARCHIVENAME_VISIBLE                               = SH.getKeyHandler("BubbleContentArchivenameVisible", BooleanKeyHandler.class);

    /**
     * Only use subfolders if the archive ROOT contains at least *** folders or folders
     **/
    public static final IntegerKeyHandler                SUB_PATH_MIN_FILES_OR_FOLDERS_TRESHHOLD                          = SH.getKeyHandler("SubPathMinFilesOrFoldersTreshhold", IntegerKeyHandler.class);

    /**
     * Delete archives after successful extraction?
     **/
    public static final BooleanKeyHandler                DELETE_ARCHIVE_FILES_AFTER_EXTRACTION                            = SH.getKeyHandler("DeleteArchiveFilesAfterExtraction", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                OVERWRITE_EXISTING_FILES_ENABLED                                 = SH.getKeyHandler("OverwriteExistingFilesEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                SUBPATH_ENABLED                                                  = SH.getKeyHandler("SubpathEnabled", BooleanKeyHandler.class);

    /**
     * This option improves password find speed a lot, but may result in finding errors.
     **/
    public static final BooleanKeyHandler                PASSWORD_FIND_OPTIMIZATION_ENABLED                               = SH.getKeyHandler("PasswordFindOptimizationEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                GUI_ENABLED                                                      = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                   CPUPRIORITY                                                      = SH.getKeyHandler("CPUPriority", EnumKeyHandler.class);

    /**
     * A Blacklist is a list of regular expressions. Use a blacklist to avoid extracting certain filetypes.
     **/
    public static final StringListHandler                BLACKLIST_PATTERNS                                               = SH.getKeyHandler("BlacklistPatterns", StringListHandler.class);

    public static final BooleanKeyHandler                ENABLED                                                          = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    /**
     * max bytes the extractor may test for finding correct password when no signature is found
     **/
    public static final IntegerKeyHandler                MAX_CHECKED_FILE_SIZE_DURING_OPTIMIZED_PASSWORD_FINDING_IN_BYTES = SH.getKeyHandler("MaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes", IntegerKeyHandler.class);

    /**
     * Info File Extension is able to create Info files for all downloaded files. Extraction Extension can remove these files
     **/
    public static final BooleanKeyHandler                DELETE_INFO_FILES_AFTER_EXTRACTION                               = SH.getKeyHandler("DeleteInfoFilesAfterExtraction", BooleanKeyHandler.class);

    /**
     * Shall Extraction Extension ask you for passwords if the correct password has not been found in passwordcache?
     **/
    public static final BooleanKeyHandler                ASK_FOR_UNKNOWN_PASSWORDS_ENABLED                                = SH.getKeyHandler("AskForUnknownPasswordsEnabled", BooleanKeyHandler.class);

    /**
     * Enabled usage of custom extractionpathes
     **/
    public static final BooleanKeyHandler                CUSTOM_EXTRACTION_PATH_ENABLED                                   = SH.getKeyHandler("CustomExtractionPathEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                OLD_PWLIST_IMPORTED                                              = SH.getKeyHandler("OldPWListImported", BooleanKeyHandler.class);

    /**
     * Show Bubbles for Extration Jobs
     **/
    public static final BooleanKeyHandler                BUBBLE_ENABLED_IF_ARCHIVE_EXTRACTION_IS_IN_PROGRESS              = SH.getKeyHandler("BubbleEnabledIfArchiveExtractionIsInProgress", BooleanKeyHandler.class);

    /**
     * Use original filedate if possible
     **/
    public static final BooleanKeyHandler                USE_ORIGINAL_FILE_DATE                                           = SH.getKeyHandler("UseOriginalFileDate", BooleanKeyHandler.class);

    /**
     * A List of passwords for automatic extraction of password protected archives.
     **/
    public static final ObjectKeyHandler                 PASSWORD_LIST                                                    = SH.getKeyHandler("PasswordList", ObjectKeyHandler.class);

    public static final StringKeyHandler                 SUB_PATH                                                         = SH.getKeyHandler("SubPath", StringKeyHandler.class);

    /**
     * max buffer size for write operations in kb
     **/
    public static final IntegerKeyHandler                BUFFER_SIZE                                                      = SH.getKeyHandler("BufferSize", IntegerKeyHandler.class);

    /**
     * Extract Log files in logs/extraction/...
     **/
    public static final BooleanKeyHandler                WRITE_EXTRACTION_LOG_ENABLED                                     = SH.getKeyHandler("WriteExtractionLogEnabled", BooleanKeyHandler.class);
}