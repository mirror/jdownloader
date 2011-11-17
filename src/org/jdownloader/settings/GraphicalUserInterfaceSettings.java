package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.Application;

public interface GraphicalUserInterfaceSettings extends ConfigInterface {

    // Static Mappings for interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings

    class ThemeValidator extends AbstractValidator<String> {

        @Override
        public void validate(String themeID) throws ValidationException {
            if (!Application.getResource("themes/" + themeID).exists()) {
                throw new ValidationException(Application.getResource("themes/" + themeID) + " must exist");
            } else if (!Application.getResource("themes/" + themeID).isDirectory()) { throw new ValidationException(Application.getResource("themes/" + themeID) + " must be a directory"); }
        }

    }

    public static final GraphicalUserInterfaceSettings                 CFG                                       = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    public static final StorageHandler<GraphicalUserInterfaceSettings> SH                                        = (StorageHandler<GraphicalUserInterfaceSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    public static final IntegerKeyHandler                              DIALOG_DEFAULT_TIMEOUT                    = SH.getKeyHandler("DialogDefaultTimeout", IntegerKeyHandler.class);
    /**
     * If enabled, the background of captchas will be removed to fit to the rest
     * of the design (transparency)
     **/
    public static final BooleanKeyHandler                              CAPTCHA_BACKGROUND_CLEANUP_ENABLED        = SH.getKeyHandler("CaptchaBackgroundCleanupEnabled", BooleanKeyHandler.class);
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_DOWN_BUTTON                     = SH.getKeyHandler("ShowMoveDownButton", BooleanKeyHandler.class);
    public static final StringKeyHandler                               ACTIVE_CONFIG_PANEL                       = SH.getKeyHandler("ActiveConfigPanel", StringKeyHandler.class);
    /**
     * Enable/disable support for system DPI settings. Default value is true.
     **/
    public static final BooleanKeyHandler                              FONT_RESPECTS_SYSTEM_DPI                  = SH.getKeyHandler("FontRespectsSystemDPI", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                              CONFIG_VIEW_VISIBLE                       = SH.getKeyHandler("ConfigViewVisible", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_VISIBLE               = SH.getKeyHandler("LinkgrabberSidebarVisible", BooleanKeyHandler.class);
    /**
     * Icon Theme ID. Make sure that ./themes/<ID>/ exists
     **/
    public static final StringKeyHandler                               THEME_ID                                  = SH.getKeyHandler("ThemeID", StringKeyHandler.class);
    /**
     * Enable/Disable the Linkgrabber Sidebar QuicktoggleButton
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_TOGGLE_BUTTON_ENABLED = SH.getKeyHandler("LinkgrabberSidebarToggleButtonEnabled", BooleanKeyHandler.class);
    /**
     * Font scale factor in percent. Default value is 100 which means no font
     * scaling.
     **/
    public static final IntegerKeyHandler                              FONT_SCALE_FACTOR                         = SH.getKeyHandler("FontScaleFactor", IntegerKeyHandler.class);
    /**
     * Paint all labels/text with or without antialias. Default value is false.
     **/
    public static final BooleanKeyHandler                              TEXT_ANTI_ALIAS_ENABLED                   = SH.getKeyHandler("TextAntiAliasEnabled", BooleanKeyHandler.class);
    /**
     * Enable/Disable the Linkgrabber Sidebar
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_ENABLED               = SH.getKeyHandler("LinkgrabberSidebarEnabled", BooleanKeyHandler.class);
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_TO_TOP_BUTTON                   = SH.getKeyHandler("ShowMoveToTopButton", BooleanKeyHandler.class);
    /**
     * Highlight Column in Downloadview if table is not in downloadsortorder
     **/
    public static final BooleanKeyHandler                              SORT_COLUMN_HIGHLIGHT_ENABLED             = SH.getKeyHandler("SortColumnHighlightEnabled", BooleanKeyHandler.class);
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_TO_BOTTOM_BUTTON                = SH.getKeyHandler("ShowMoveToBottomButton", BooleanKeyHandler.class);
    /**
     * Font to be used. Default value is default.
     **/
    public static final StringKeyHandler                               FONT_NAME                                 = SH.getKeyHandler("FontName", StringKeyHandler.class);
    /**
     * Set this to false to hide the Bottombar in the Downloadview
     **/
    public static final BooleanKeyHandler                              DOWNLOAD_VIEW_BOTTOMBAR_ENABLED           = SH.getKeyHandler("DownloadViewBottombarEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                              BALLOON_NOTIFICATION_ENABLED              = SH.getKeyHandler("BalloonNotificationEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                              LOG_VIEW_VISIBLE                          = SH.getKeyHandler("LogViewVisible", BooleanKeyHandler.class);
    public static final StringKeyHandler                               LOOK_AND_FEEL                             = SH.getKeyHandler("LookAndFeel", StringKeyHandler.class);
    /**
     * Disable animation and all animation threads. Optional value. Default
     * value is true.
     **/
    public static final BooleanKeyHandler                              ANIMATION_ENABLED                         = SH.getKeyHandler("AnimationEnabled", BooleanKeyHandler.class);
    /**
     * Captcha Dialog Image scale Faktor in %
     **/
    public static final IntegerKeyHandler                              CAPTCHA_SCALE_FACTOR                      = SH.getKeyHandler("CaptchaScaleFactor", IntegerKeyHandler.class);
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_UP_BUTTON                       = SH.getKeyHandler("ShowMoveUpButton", BooleanKeyHandler.class);
    /**
     * Enable/disable window opacity on Java 6u10 and above. A value of 'false'
     * disables window opacity which means that the window corner background
     * which is visible for non-rectangular windows disappear. Furthermore the
     * shadow for popupMenus makes use of real translucent window. Some themes
     * like SyntheticaSimple2D support translucent titlePanes if opacity is
     * disabled. The property is ignored on JRE's below 6u10. Note: It is
     * recommended to activate this feature only if your graphics hardware
     * acceleration is supported by the JVM - a value of 'false' can affect
     * application performance. Default value is false which means the
     * translucency feature is enabled
     **/
    public static final BooleanKeyHandler                              WINDOW_OPAQUE                             = SH.getKeyHandler("WindowOpaque", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              CLIPBOARD_MONITORED                       = SH.getKeyHandler("ClipboardMonitored", BooleanKeyHandler.class);

    String getActiveConfigPanel();

    @AboutConfig
    @Description("Captcha Dialog Image scale Faktor in %")
    @DefaultIntValue(100)
    @SpinnerValidator(min = 50, max = 500, step = 10)
    int getCaptchaScaleFactor();

    @DefaultIntValue(20)
    @AboutConfig
    int getDialogDefaultTimeout();

    @AboutConfig
    @Description("Font to be used. Default value is default.")
    @DefaultStringValue("default")
    @RequiresRestart
    String getFontName();

    @AboutConfig
    @Description("Font scale factor in percent. Default value is 100 which means no font scaling.")
    @DefaultIntValue(100)
    @RequiresRestart
    int getFontScaleFactor();

    @AboutConfig
    String getLookAndFeel();

    @DefaultStringValue("standard")
    @AboutConfig
    @Description("Icon Theme ID. Make sure that ./themes/<ID>/ exists")
    @ValidatorFactory(ThemeValidator.class)
    String getThemeID();

    @AboutConfig
    @Description("Disable animation and all animation threads. Optional value. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isAnimationEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isBalloonNotificationEnabled();

    @AboutConfig
    @Description("If enabled, the background of captchas will be removed to fit to the rest of the design (transparency)")
    @DefaultBooleanValue(true)
    boolean isCaptchaBackgroundCleanupEnabled();

    @DefaultBooleanValue(false)
    boolean isConfigViewVisible();

    @AboutConfig
    @Description("Set this to false to hide the Bottombar in the Downloadview")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isDownloadViewBottombarEnabled();

    @AboutConfig
    @Description("Enable/disable support for system DPI settings. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isFontRespectsSystemDPI();

    @AboutConfig
    @Description("Enable/Disable the Linkgrabber Sidebar")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarEnabled();

    @AboutConfig
    @Description("Enable/Disable the Linkgrabber Sidebar QuicktoggleButton")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarToggleButtonEnabled();

    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarVisible();

    @DefaultBooleanValue(false)
    boolean isLogViewVisible();

    @AboutConfig
    @Description("True if move button should be visible in downloadview")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isShowMoveDownButton();

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    boolean isShowMoveToBottomButton();

    @Description("True if move button should be visible in downloadview")
    @RequiresRestart
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isShowMoveToTopButton();

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    boolean isShowMoveUpButton();

    @AboutConfig
    @Description("Highlight Column in Downloadview if table is not in downloadsortorder")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isSortColumnHighlightEnabled();

    @AboutConfig
    @Description("Paint all labels/text with or without antialias. Default value is false.")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isTextAntiAliasEnabled();

    @AboutConfig
    @Description("Enable/disable window opacity on Java 6u10 and above. A value of 'false' disables window opacity which means that the window corner background which is visible for non-rectangular windows disappear. Furthermore the shadow for popupMenus makes use of real translucent window. Some themes like SyntheticaSimple2D support translucent titlePanes if opacity is disabled. The property is ignored on JRE's below 6u10. Note: It is recommended to activate this feature only if your graphics hardware acceleration is supported by the JVM - a value of 'false' can affect application performance. Default value is false which means the translucency feature is enabled")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isWindowOpaque();

    @AboutConfig
    @Description("Enable/disable Enable/disable Clipboard monitoring")
    @DefaultBooleanValue(false)
    boolean isClipboardMonitored();

    void setClipboardMonitored(boolean b);

    void setActiveConfigPanel(String name);

    void setAnimationEnabled(boolean b);

    void setBalloonNotificationEnabled(boolean b);

    void setCaptchaBackgroundCleanupEnabled(boolean b);

    void setCaptchaScaleFactor(int b);

    void setConfigViewVisible(boolean b);

    void setDialogDefaultTimeout(int value);

    void setDownloadViewBottombarEnabled(boolean b);

    void setFontName(String name);

    void setFontRespectsSystemDPI(boolean b);

    void setFontScaleFactor(int b);

    void setLinkgrabberSidebarEnabled(boolean b);

    void setLinkgrabberSidebarToggleButtonEnabled(boolean b);

    void setLinkgrabberSidebarVisible(boolean b);

    void setLogViewVisible(boolean b);

    void setLookAndFeel(String laf);

    void setShowMoveDownButton(boolean b);

    void setShowMoveToBottomButton(boolean b);

    void setShowMoveToTopButton(boolean b);

    void setShowMoveUpButton(boolean b);

    void setSortColumnHighlightEnabled(boolean b);

    void setTextAntiAliasEnabled(boolean b);

    void setThemeID(String themeID);

    void setWindowOpaque(boolean b);

}
