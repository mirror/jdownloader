package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;

public class CFG_LINKGRABBER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(LinkgrabberSettings.class);
    }

    // Static Mappings for interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings
    public static final LinkgrabberSettings                 CFG                                          = JsonConfig.create(LinkgrabberSettings.class);
    public static final StorageHandler<LinkgrabberSettings> SH                                           = (StorageHandler<LinkgrabberSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // true
    /**
     * If true, Linkcollector will create an extra package for each multipart or *.rar archive
     **/
    public static final BooleanKeyHandler                   ARCHIVE_PACKAGIZER_ENABLED                   = SH.getKeyHandler("ArchivePackagizerEnabled", BooleanKeyHandler.class);
    // 1
    /**
     * If >0, there will be no packages with * or less links
     **/
    public static final IntegerKeyHandler                   VARIOUS_PACKAGE_LIMIT                        = SH.getKeyHandler("VariousPackageLimit", IntegerKeyHandler.class);
    // true
    /**
     * If true, Offline Links, that do not fit in a existing package, will be moved to a offline package.
     **/
    public static final BooleanKeyHandler                   OFFLINE_PACKAGE_ENABLED                      = SH.getKeyHandler("OfflinePackageEnabled", BooleanKeyHandler.class);
    // C:\Users\Thomas\downloads
    public static final StringKeyHandler                    LATEST_DOWNLOAD_DESTINATION_FOLDER           = SH.getKeyHandler("LatestDownloadDestinationFolder", StringKeyHandler.class);
    // true
    /**
     * If true, AddLinks Dialogs will use the last used downloadfolder as defaultvalue. IF False, the Default Download Paath (settings) will be used
     **/
    public static final BooleanKeyHandler                   USE_LAST_DOWNLOAD_DESTINATION_AS_DEFAULT     = SH.getKeyHandler("UseLastDownloadDestinationAsDefault", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                   AUTO_EXTRACTION_ENABLED                      = SH.getKeyHandler("AutoExtractionEnabled", BooleanKeyHandler.class);
    // true
    /**
     * If false, The AddLinks Dialog in Linkgrabber works on the pasted text, and does not prefilter URLS any more
     **/
    public static final BooleanKeyHandler                   ADD_LINKS_PRE_PARSER_ENABLED                 = SH.getKeyHandler("AddLinksPreParserEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Set to false to hide the 'Add Downloads' Context Menu Action in Linkgrabber
     **/
    public static final BooleanKeyHandler                   CONTEXT_MENU_ADD_LINKS_ACTION_ALWAYS_VISIBLE = SH.getKeyHandler("ContextMenuAddLinksActionAlwaysVisible", BooleanKeyHandler.class);
    // []
    public static final ObjectKeyHandler                    PACKAGE_NAME_HISTORY                         = SH.getKeyHandler("PackageNameHistory", ObjectKeyHandler.class);
    // true
    /**
     * If true, Plugins will try to correct filenames to match to others. For example in splitted archives.
     **/
    public static final BooleanKeyHandler                   AUTO_FILENAME_CORRECTION_ENABLED             = SH.getKeyHandler("AutoFilenameCorrectionEnabled", BooleanKeyHandler.class);
    // [C:\Users\Thomas\downloads]
    public static final ObjectKeyHandler                    DOWNLOAD_DESTINATION_HISTORY                 = SH.getKeyHandler("DownloadDestinationHistory", ObjectKeyHandler.class);

    // true
    /**
     * Selecting Views in Linkgrabber Sidebar autoselects the matching links in the table. Set this to false to avoid this.
     **/
    public static final BooleanKeyHandler                   QUICK_VIEW_SELECTION_ENABLED                 = SH.getKeyHandler("QuickViewSelectionEnabled", BooleanKeyHandler.class);
    // 15000
    /**
     * AutoConfirm waits a delay before confirming the links. Default is 15000ms
     **/
    public static final IntegerKeyHandler                   AUTO_CONFIRM_DELAY                           = SH.getKeyHandler("AutoConfirmDelay", IntegerKeyHandler.class);

    /**
     * If set, the addlinks dialog has this text. Use it for debug reasons.
     **/
    public static final StringKeyHandler                    PRESET_DEBUG_LINKS                           = SH.getKeyHandler("PresetDebugLinks", StringKeyHandler.class);
    // true
    /**
     * If true, the Linkcollector asks the Hosterplugins to filter the packageidentifier. This helps to map corrupt filenames into the correct packages.
     **/
    public static final BooleanKeyHandler                   AUTO_PACKAGE_MATCHING_CORRECTION_ENABLED     = SH.getKeyHandler("AutoPackageMatchingCorrectionEnabled", BooleanKeyHandler.class);

}