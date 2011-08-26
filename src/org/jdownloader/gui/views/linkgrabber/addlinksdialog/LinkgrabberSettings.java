package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RequiresRestart;

public interface LinkgrabberSettings extends ConfigInterface {

    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<String> getDownloadDestinationHistory();

    void setDownloadDestinationHistory(ArrayList<String> value);

    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<String> getPackageNameHistory();

    void setPackageNameHistory(ArrayList<String> value);

    @DefaultBooleanValue(true)
    boolean isAutoExtractionEnabled();

    void setAutoExtractionEnabled(boolean b);

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
}
