package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.ValueValidator;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RequiresRestart;

@ValueValidator(GraphicalUserInterfaceSettingsValidator.class)
public interface GraphicalUserInterfaceSettings extends ConfigInterface {
    void setActiveConfigPanel(String name);

    String getActiveConfigPanel();

    @DefaultStringValue("standard")
    @AboutConfig
    @Description("Icon Theme ID. Make sure that ./themes/<ID>/ exists")
    String getThemeID();

    void setThemeID(String themeID);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isBalloonNotificationEnabled();

    void setBalloonNotificationEnabled(boolean b);

    @DefaultBooleanValue(false)
    boolean isConfigViewVisible();

    void setConfigViewVisible(boolean b);

    @DefaultBooleanValue(false)
    boolean isLogViewVisible();

    void setLogViewVisible(boolean b);

    @AboutConfig
    String getLookAndFeel();

    void setLookAndFeel(String laf);

    @DefaultIntValue(20)
    @AboutConfig
    int getDialogDefaultTimeout();

    void setDialogDefaultTimeout(int value);

    @Description("True if move button should be visible in downloadview")
    @RequiresRestart
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isShowMoveToTopButton();

    void setShowMoveToTopButton(boolean b);

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    boolean isShowMoveToBottomButton();

    void setShowMoveToBottomButton(boolean b);

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(true)
    boolean isShowMoveUpButton();

    void setShowMoveUpButton(boolean b);

    @AboutConfig
    @Description("True if move button should be visible in downloadview")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isShowMoveDownButton();

    void setShowMoveDownButton(boolean b);

    @AboutConfig
    @Description("Set this to false to hide the Bottombar in the Downloadview")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isDownloadViewBottombarEnabled();

    void setDownloadViewBottombarEnabled(boolean b);

    @AboutConfig
    @Description("Highlight Column in Downloadview if table is not in downloadsortorder")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isSortColumnHighlightEnabled();

    void setSortColumnHighlightEnabled(boolean b);

    @AboutConfig
    @Description("Paint all labels/text with or without antialias. Default value is false.")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isTextAntiAliasEnabled();

    void setTextAntiAliasEnabled(boolean b);

    @AboutConfig
    @Description("Enable/disable support for system DPI settings. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isFontRespectsSystemDPI();

    void setFontRespectsSystemDPI(boolean b);

    @AboutConfig
    @Description("Font scale factor in percent. Default value is 100 which means no font scaling.")
    @DefaultIntValue(100)
    @RequiresRestart
    int getFontScaleFactor();

    void setFontScaleFactor(int b);

    @AboutConfig
    @Description("Disable animation and all animation threads. Optional value. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isAnimationEnabled();

    void setAnimationEnabled(boolean b);

    @AboutConfig
    @Description("Enable/disable window opacity on Java 6u10 and above. A value of 'false' disables window opacity which means that the window corner background which is visible for non-rectangular windows disappear. Furthermore the shadow for popupMenus makes use of real translucent window. Some themes like SyntheticaSimple2D support translucent titlePanes if opacity is disabled. The property is ignored on JRE's below 6u10. Note: It is recommended to activate this feature only if your graphics hardware acceleration is supported by the JVM - a value of 'false' can affect application performance. Default value is false which means the translucency feature is enabled")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isWindowOpaque();

    void setWindowOpaque(boolean b);

}
