package org.jdownloader.gui.notify.gui;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;

public class CFG_BUBBLE {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(BubbleNotifyConfig.class);

    }

    // Static Mappings for interface org.jdownloader.gui.notify.gui.BubbleNotifyConfig
    public static final BubbleNotifyConfig                 CFG                                              = JsonConfig.create(BubbleNotifyConfig.class);
    public static final StorageHandler<BubbleNotifyConfig> SH                                               = (StorageHandler<BubbleNotifyConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED   = SH.getKeyHandler("BubbleNotifyOnNewLinkgrabberLinksEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  FRESH_INSTALL                                    = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                  GUI_ENABLED                                      = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                  ENABLED                                          = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_PACKAGE_ENABLED = SH.getKeyHandler("BubbleNotifyOnNewLinkgrabberPackageEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_RECONNECT_START_ENABLED         = SH.getKeyHandler("BubbleNotifyOnReconnectStartEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_UPDATE_AVAILABLE_ENABLED        = SH.getKeyHandler("BubbleNotifyOnUpdateAvailableEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_RECONNECT_END_ENABLED           = SH.getKeyHandler("BubbleNotifyOnReconnectEndEnabled", BooleanKeyHandler.class);
}