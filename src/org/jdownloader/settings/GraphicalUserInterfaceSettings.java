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

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isWindowDecorationEnabled();

    void setWindowDecorationEnabled(boolean b);

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
}
