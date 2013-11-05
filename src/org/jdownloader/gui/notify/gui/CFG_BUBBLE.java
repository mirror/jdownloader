package org.jdownloader.gui.notify.gui;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.LongKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;

public class CFG_BUBBLE {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(BubbleNotifyConfig.class);

    }

    // Static Mappings for interface org.jdownloader.gui.notify.gui.BubbleNotifyConfig
    public static final BubbleNotifyConfig                 CFG                                            = JsonConfig.create(BubbleNotifyConfig.class);
    public static final StorageHandler<BubbleNotifyConfig> SH                                             = (StorageHandler<BubbleNotifyConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final IntegerKeyHandler                  FADE_ANIMATION_DURATION                        = SH.getKeyHandler("FadeAnimationDuration", IntegerKeyHandler.class);

    /**
     * The bubbles iuse the current active screen. That means the screen that is used by the Main Window. You can set a hardcoded screen ID
     * here. Like \display0 for your main screen
     **/
    public static final StringKeyHandler                   SCREEN_ID                                      = SH.getKeyHandler("ScreenID", StringKeyHandler.class);

    /**
     * Animation End Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor
     **/
    public static final EnumKeyHandler                     ANIMATION_END_POSITION_ANCHOR                  = SH.getKeyHandler("AnimationEndPositionAnchor", EnumKeyHandler.class);

    public static final EnumKeyHandler                     BUBBLE_NOTIFY_ENABLED_STATE                    = SH.getKeyHandler("BubbleNotifyEnabledState", EnumKeyHandler.class);

    public static final LongKeyHandler                     DOWNLOAD_START_END_NOTIFY_DELAY                = SH.getKeyHandler("DownloadStartEndNotifyDelay", LongKeyHandler.class);

    /**
     * Position Anchor for the First Bubble. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor
     **/
    public static final EnumKeyHandler                     FINAL_POSITION_ANCHOR                          = SH.getKeyHandler("FinalPositionAnchor", EnumKeyHandler.class);

    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_RECONNECT_START_ENABLED       = SH.getKeyHandler("BubbleNotifyOnReconnectStartEnabled", BooleanKeyHandler.class);

    /**
     * Animation Start Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor
     **/
    public static final EnumKeyHandler                     ANIMATION_START_POSITION_ANCHOR                = SH.getKeyHandler("AnimationStartPositionAnchor", EnumKeyHandler.class);

    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_UPDATE_AVAILABLE_ENABLED      = SH.getKeyHandler("BubbleNotifyOnUpdateAvailableEnabled", BooleanKeyHandler.class);

    public static final IntegerKeyHandler                  DEFAULT_TIMEOUT                                = SH.getKeyHandler("DefaultTimeout", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ENABLED_DURING_SILENT_MODE       = SH.getKeyHandler("BubbleNotifyEnabledDuringSilentMode", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_START_STOP_DOWNLOADS_ENABLED     = SH.getKeyHandler("BubbleNotifyStartStopDownloadsEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_START_PAUSE_STOP_ENABLED         = SH.getKeyHandler("BubbleNotifyStartPauseStopEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED = SH.getKeyHandler("BubbleNotifyOnNewLinkgrabberLinksEnabled", BooleanKeyHandler.class);

    /**
     * Animation End Y Position. 0 is top screen edge -1 is bottom screen edge
     **/
    public static final IntegerKeyHandler                  ANIMATION_END_POSITION_Y                       = SH.getKeyHandler("AnimationEndPositionY", IntegerKeyHandler.class);

    /**
     * Animation End X Position. 0 is left screen edge -1 is right screen edge
     **/
    public static final IntegerKeyHandler                  ANIMATION_END_POSITION_X                       = SH.getKeyHandler("AnimationEndPositionX", IntegerKeyHandler.class);

    /**
     * X Position of the first bubble. 0 is left screen edge -1 is right screen edge
     **/
    public static final IntegerKeyHandler                  FINAL_POSITION_X                               = SH.getKeyHandler("FinalPositionX", IntegerKeyHandler.class);

    /**
     * Animation Start Y Position. 0 is top screen edge -1 is bottom screen edge
     **/
    public static final IntegerKeyHandler                  ANIMATION_START_POSITION_Y                     = SH.getKeyHandler("AnimationStartPositionY", IntegerKeyHandler.class);

    /**
     * Animation Start X Position. 0 is left screen edge -1 is right screen edge
     **/
    public static final IntegerKeyHandler                  ANIMATION_START_POSITION_X                     = SH.getKeyHandler("AnimationStartPositionX", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                  BUBBLE_NOTIFY_ON_CAPTCHA_IN_BACKGROUND_ENABLED = SH.getKeyHandler("BubbleNotifyOnCaptchaInBackgroundEnabled", BooleanKeyHandler.class);

    /**
     * Y Position of the first bubble. 0 is top screen edge -1 is bottom screen edge
     **/
    public static final IntegerKeyHandler                  FINAL_POSITION_Y                               = SH.getKeyHandler("FinalPositionY", IntegerKeyHandler.class);
}