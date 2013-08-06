package org.jdownloader.gui.notify.gui;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;

public class CFG_BUBBLE {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(BubbleNotifyConfig.class);

    }

    // Static Mappings for interface org.jdownloader.gui.notify.gui.BubbleNotifyConfig
    public static final BubbleNotifyConfig                 CFG                                              = JsonConfig.create(BubbleNotifyConfig.class);
    public static final StorageHandler<BubbleNotifyConfig> SH                                               = (StorageHandler<BubbleNotifyConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // \display9999
    /**
     * The bubbles iuse the current active screen. That means the screen that is used by the Main Window. You can set a hardcoded screen ID
     * here. Like \display0 for your main screen
     **/
    public static final StringKeyHandler                   SCREEN_ID                                        = SH.getKeyHandler("ScreenID", StringKeyHandler.class);
    // -1
    /**
     * Animation Start X Position. 0 is left screen edge -1 is right screen edge
     **/
    public static final IntegerKeyHandler                  START_X                                          = SH.getKeyHandler("StartX", IntegerKeyHandler.class);
    // -1
    /**
     * Animation Start Y Position. 0 is top screen edge -1 is bottom screen edge
     **/
    public static final IntegerKeyHandler                  START_Y                                          = SH.getKeyHandler("StartY", IntegerKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_RECONNECT_START_ENABLED         = SH.getKeyHandler("BubbleNotifyOnReconnectStartEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_UPDATE_AVAILABLE_ENABLED        = SH.getKeyHandler("BubbleNotifyOnUpdateAvailableEnabled", BooleanKeyHandler.class);
    // 0
    /**
     * Y Position of the first bubble. 0 is top screen edge -1 is bottom screen edge
     **/
    public static final IntegerKeyHandler                  ANCHOR_Y                                         = SH.getKeyHandler("AnchorY", IntegerKeyHandler.class);
    // 0
    /**
     * Animation End Y Position. 0 is top screen edge -1 is bottom screen edge
     **/
    public static final IntegerKeyHandler                  END_Y                                            = SH.getKeyHandler("EndY", IntegerKeyHandler.class);
    // -1
    /**
     * X Position of the first bubble. 0 is left screen edge -1 is right screen edge
     **/
    public static final IntegerKeyHandler                  ANCHOR_X                                         = SH.getKeyHandler("AnchorX", IntegerKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_PACKAGE_ENABLED = SH.getKeyHandler("BubbleNotifyOnNewLinkgrabberPackageEnabled", BooleanKeyHandler.class);
    // TOP_RIGHT
    /**
     * Animation Start Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor
     **/
    public static final EnumKeyHandler                     START_ANCHOR                                     = SH.getKeyHandler("StartAnchor", EnumKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED   = SH.getKeyHandler("BubbleNotifyOnNewLinkgrabberLinksEnabled", BooleanKeyHandler.class);
    // BOTTOM_RIGHT
    /**
     * Animation End Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor
     **/
    public static final EnumKeyHandler                     END_ANCHOR                                       = SH.getKeyHandler("EndAnchor", EnumKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_RECONNECT_END_ENABLED           = SH.getKeyHandler("BubbleNotifyOnReconnectEndEnabled", BooleanKeyHandler.class);
    // TOP_RIGHT
    /**
     * Position Anchor for the First Bubble. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor
     **/
    public static final EnumKeyHandler                     ANCHOR                                           = SH.getKeyHandler("Anchor", EnumKeyHandler.class);
    // -1
    /**
     * Animation End X Position. 0 is left screen edge -1 is right screen edge
     **/
    public static final IntegerKeyHandler                  END_X                                            = SH.getKeyHandler("EndX", IntegerKeyHandler.class);
}