package jd.gui.swing.jdgui;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;

public interface GraphicalUserInterfaceSettings extends ConfigInterface {
    @DefaultBooleanValue(true)
    boolean isBalloonNotificationEnabled();

    @DefaultBooleanValue(false)
    boolean isConfigViewVisible();

    void setConfigViewVisible(boolean b);

    @DefaultBooleanValue(false)
    boolean isLogViewVisible();

    void setLogViewVisible(boolean b);

    String getLookAndFeel();

    @DefaultBooleanValue(true)
    boolean isWindowDecorationEnabled();

    void setWindowDecorationEnabled(boolean b);

    @DefaultIntValue(20)
    int getDialogDefaultTimeout();

    void setDialogDefaultTimeout(int value);
}
