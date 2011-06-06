package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.ValueValidator;

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
}
