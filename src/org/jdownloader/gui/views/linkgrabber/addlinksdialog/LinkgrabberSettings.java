package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;

public interface LinkgrabberSettings extends ConfigInterface {

    // Static Mappings for interface
    // org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings
    public static final LinkgrabberSettings                 CFG                                      = JsonConfig.create(LinkgrabberSettings.class);
    public static final StorageHandler<LinkgrabberSettings> SH                                       = (StorageHandler<LinkgrabberSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    /**
     * If set, the addlinks dialog has this text. Use it for debug reasons.
     **/
    public static final StringKeyHandler                    PRESET_DEBUG_LINKS                       = SH.getKeyHandler("PresetDebugLinks", StringKeyHandler.class);
    public static final ObjectKeyHandler                    PACKAGE_NAME_HISTORY                     = SH.getKeyHandler("PackageNameHistory", ObjectKeyHandler.class);
    public static final StringKeyHandler                    LATEST_DOWNLOAD_DESTINATION_FOLDER       = SH.getKeyHandler("LatestDownloadDestinationFolder", StringKeyHandler.class);
    /**
     * If true, AddLinks Dialogs will use the last used downloadfolder as
     * defaultvalue. IF False, the Default Download Paath (settings) will be
     * used
     **/
    public static final BooleanKeyHandler                   USE_LAST_DOWNLOAD_DESTINATION_AS_DEFAULT = SH.getKeyHandler("UseLastDownloadDestinationAsDefault", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                   AUTO_EXTRACTION_ENABLED                  = SH.getKeyHandler("AutoExtractionEnabled", BooleanKeyHandler.class);
    /**
     * If false, The AddLinks Dialog in Linkgrabber works on the pasted text,
     * and does not prefilter URLS any more
     **/
    public static final BooleanKeyHandler                   ADD_LINKS_PRE_PARSER_ENABLED             = SH.getKeyHandler("AddLinksPreParserEnabled", BooleanKeyHandler.class);
    public static final ObjectKeyHandler                    DOWNLOAD_DESTINATION_HISTORY             = SH.getKeyHandler("DownloadDestinationHistory", ObjectKeyHandler.class);
    /**
     * Selecting Views in Linkgrabber Sidebar autoselects the matching links in
     * the table. Set this to false to avoid this.
     **/
    public static final BooleanKeyHandler                   QUICK_VIEW_SELECTION_ENABLED             = SH.getKeyHandler("QuickViewSelectionEnabled", BooleanKeyHandler.class);

    @DefaultJsonObject("[]")
    @AboutConfig
    ArrayList<DownloadPath> getDownloadDestinationHistory();

    void setDownloadDestinationHistory(ArrayList<DownloadPath> value);

    @DefaultJsonObject("[]")
    @AboutConfig
    ArrayList<PackageHistoryEntry> getPackageNameHistory();

    void setPackageNameHistory(ArrayList<PackageHistoryEntry> value);

    @DefaultBooleanValue(true)
    boolean isAutoExtractionEnabled();

    void setAutoExtractionEnabled(boolean b);

    @DefaultIntValue(600)
    int getAddDialogWidth();

    void setAddDialogWidth(int width);

    @DefaultIntValue(-1)
    int getAddDialogHeight();

    void setAddDialogHeight(int height);

    void setLatestDownloadDestinationFolder(String absolutePath);

    String getLatestDownloadDestinationFolder();

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("If true, AddLinks Dialogs will use the last used downloadfolder as defaultvalue. IF False, the Default Download Paath (settings) will be used")
    boolean isUseLastDownloadDestinationAsDefault();

    void setUseLastDownloadDestinationAsDefault(boolean b);

    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    @Description("If false, The AddLinks Dialog in Linkgrabber works on the pasted text, and does not prefilter URLS any more")
    boolean isAddLinksPreParserEnabled();

    void setAddLinksPreParserEnabled(boolean b);

    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    @Description("Selecting Views in Linkgrabber Sidebar autoselects the matching links in the table. Set this to false to avoid this.")
    boolean isQuickViewSelectionEnabled();

    void setQuickViewSelectionEnabled(boolean b);

    @Description("If set, the addlinks dialog has this text. Use it for debug reasons.")
    @AboutConfig
    String getPresetDebugLinks();

    void setPresetDebugLinks(String text);
}
