package org.jdownloader.controlling;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.jdownloader.settings.annotations.AboutConfig;

public interface LinkFilterSettings extends ConfigInterface {
    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<LinkFilter> getFilterList();

    void setFilterList(ArrayList<LinkFilter> list);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isBlackList();

    void setBlackList(boolean b);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberQuickSettingsVisible();

    void setLinkgrabberQuickSettingsVisible(boolean b);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberHosterQuickfilterEnabled();

    void setLinkgrabberHosterQuickfilterEnabled(boolean b);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberFiletypeQuickfilterEnabled();

    void setLinkgrabberFiletypeQuickfilterEnabled(boolean b);

    @DefaultBooleanValue(true)
    boolean isLingrabberGlobalLinkFilterEnabled();

    void setLingrabberGlobalLinkFilterEnabled(boolean b);

    @DefaultBooleanValue(false)
    boolean isLinkgrabberAddAtTop();

    void setLinkgrabberAddAtTop(boolean selected);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberAutoStartEnabled();

    void setLinkgrabberAutoStartEnabled(boolean selected);

    @DefaultBooleanValue(false)
    boolean isLinkgrabberAutoConfirmEnabled();

    void setLinkgrabberAutoConfirmEnabled(boolean selected);
}
