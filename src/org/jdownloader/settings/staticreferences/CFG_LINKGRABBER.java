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
    public static final StorageHandler<LinkgrabberSettings> SH                                           = (StorageHandler<LinkgrabberSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.archivepackagizerenabled
    // = true
    /**
     * If true, Linkcollector will create an extra package for each multipart or
     * *.rar archive
     **/
    public static final BooleanKeyHandler                   ARCHIVE_PACKAGIZER_ENABLED                   = SH.getKeyHandler("ArchivePackagizerEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.variouspackagelimit
    // = 1
    /**
     * If >0, there will be no packages with * or less links
     **/
    public static final IntegerKeyHandler                   VARIOUS_PACKAGE_LIMIT                        = SH.getKeyHandler("VariousPackageLimit", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.offlinepackageenabled
    // = true
    /**
     * If true, Offline Links, that do not fit in a existing package, will be
     * moved to a offline package.
     **/
    public static final BooleanKeyHandler                   OFFLINE_PACKAGE_ENABLED                      = SH.getKeyHandler("OfflinePackageEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.latestdownloaddestinationfolder
    // = null
    public static final StringKeyHandler                    LATEST_DOWNLOAD_DESTINATION_FOLDER           = SH.getKeyHandler("LatestDownloadDestinationFolder", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.uselastdownloaddestinationasdefault
    // = true
    /**
     * If true, AddLinks Dialogs will use the last used downloadfolder as
     * defaultvalue. IF False, the Default Download Paath (settings) will be
     * used
     **/
    public static final BooleanKeyHandler                   USE_LAST_DOWNLOAD_DESTINATION_AS_DEFAULT     = SH.getKeyHandler("UseLastDownloadDestinationAsDefault", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.autoextractionenabled
    // = true
    public static final BooleanKeyHandler                   AUTO_EXTRACTION_ENABLED                      = SH.getKeyHandler("AutoExtractionEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.addlinkspreparserenabled
    // = true
    /**
     * If false, The AddLinks Dialog in Linkgrabber works on the pasted text,
     * and does not prefilter URLS any more
     **/
    public static final BooleanKeyHandler                   ADD_LINKS_PRE_PARSER_ENABLED                 = SH.getKeyHandler("AddLinksPreParserEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.contextmenuaddlinksactionalwaysvisible
    // = true
    /**
     * Set to false to hide the 'Add Downloads' Context Menu Action in
     * Linkgrabber
     **/
    public static final BooleanKeyHandler                   CONTEXT_MENU_ADD_LINKS_ACTION_ALWAYS_VISIBLE = SH.getKeyHandler("ContextMenuAddLinksActionAlwaysVisible", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.packagenamehistory
    // = []
    public static final ObjectKeyHandler                    PACKAGE_NAME_HISTORY                         = SH.getKeyHandler("PackageNameHistory", ObjectKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.downloaddestinationhistory
    // = []
    public static final ObjectKeyHandler                    DOWNLOAD_DESTINATION_HISTORY                 = SH.getKeyHandler("DownloadDestinationHistory", ObjectKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.adddialogxlocation
    // = -1
    public static final IntegerKeyHandler                   ADD_DIALOG_XLOCATION                         = SH.getKeyHandler("AddDialogXLocation", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.quickviewselectionenabled
    // = true
    /**
     * Selecting Views in Linkgrabber Sidebar autoselects the matching links in
     * the table. Set this to false to avoid this.
     **/
    public static final BooleanKeyHandler                   QUICK_VIEW_SELECTION_ENABLED                 = SH.getKeyHandler("QuickViewSelectionEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.autoconfirmdelay
    // = 15000
    /**
     * AutoConfirm waits a delay before confirming the links. Default is 15000ms
     **/
    public static final IntegerKeyHandler                   AUTO_CONFIRM_DELAY                           = SH.getKeyHandler("AutoConfirmDelay", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.presetdebuglinks
    // = null
    /**
     * If set, the addlinks dialog has this text. Use it for debug reasons.
     **/
    public static final StringKeyHandler                    PRESET_DEBUG_LINKS                           = SH.getKeyHandler("PresetDebugLinks", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.adddialogylocation
    // = -1
    public static final IntegerKeyHandler                   ADD_DIALOG_YLOCATION                         = SH.getKeyHandler("AddDialogYLocation", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.adddialogwidth
    // = 600
    public static final IntegerKeyHandler                   ADD_DIALOG_WIDTH                             = SH.getKeyHandler("AddDialogWidth", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings.adddialogheight
    // = -1
    public static final IntegerKeyHandler                   ADD_DIALOG_HEIGHT                            = SH.getKeyHandler("AddDialogHeight", IntegerKeyHandler.class);
}
