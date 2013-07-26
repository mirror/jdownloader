package org.jdownloader.gui.jdtrayicon;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;

public interface TrayConfig extends ExtensionConfigInterface {

    @DefaultBooleanValue(false)
    @AboutConfig
    public boolean isStartMinimizedEnabled();

    public void setStartMinimizedEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    public boolean isToogleWindowStatusWithSingleClickEnabled();

    public void setToogleWindowStatusWithSingleClickEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isToolTipEnabled();

    public void setToolTipEnabled(boolean b);

    @AboutConfig
    @DefaultEnumValue("ASK")
    public OnCloseAction getOnCloseAction();

    public void setOnCloseAction(OnCloseAction action);

    @AboutConfig
    @DefaultEnumValue("TO_TASKBAR")
    public OnMinimizeAction getOnMinimizeAction();

    public void setOnMinimizeAction(OnMinimizeAction action);

    @DefaultBooleanValue(false)
    @AboutConfig
    public boolean isGreyIconEnabled();

    public void setGreyIconEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isGnomeTrayIconTransparentEnabled();

    public void setGnomeTrayIconTransparentEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    public boolean isTrayOnlyVisibleIfWindowIsHiddenEnabled();

    public void setTrayOnlyVisibleIfWindowIsHiddenEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isBallonNotifyOnNewLinkgrabberLinksEnabled();

    public void setBallonNotifyOnNewLinkgrabberLinksEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isBallonNotifyOnNewLinkgrabberPackageEnabled();

    public void setBallonNotifyOnNewLinkgrabberPackageEnabled(boolean b);
}
