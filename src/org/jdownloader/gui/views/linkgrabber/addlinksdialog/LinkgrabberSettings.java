package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface LinkgrabberSettings extends ConfigInterface {

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

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("Set to false to hide the 'Add Downloads' Context Menu Action in Linkgrabber")
    boolean isContextMenuAddLinksActionAlwaysVisible();

    void setContextMenuAddLinksActionAlwaysVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("If true, Offline Links, that do not fit in a existing package, will be moved to a offline package.")
    boolean isOfflinePackageEnabled();

    void setOfflinePackageEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("If true, Linkcollector will create an extra package for each multipart or *.rar  archive")
    boolean isArchivePackagizerEnabled();

    void setArchivePackagizerEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @Description("If >0, there will be no packages with * or less links")
    int getVariousPackageLimit();

    void setVariousPackageLimit(int b);

    @AboutConfig
    @DefaultIntValue(-1)
    void setAddDialogXLocation(int x);

    int getAddDialogXLocation();

    @AboutConfig
    @DefaultIntValue(-1)
    void setAddDialogYLocation(int y);

    int getAddDialogYLocation();

    @AboutConfig
    @RequiresRestart
    @Description("AutoConfirm waits a delay before confirming the links. Default is 15000ms")
    @DefaultIntValue(15000)
    int getAutoConfirmDelay();

    void setAutoConfirmDelay(int delay);
}
