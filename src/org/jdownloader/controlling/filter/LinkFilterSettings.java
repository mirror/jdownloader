package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;

public interface LinkFilterSettings extends ConfigInterface {

    public static final LinkFilterSettings                 CFG                 = JsonConfig.create(LinkFilterSettings.class);
    public static final StorageHandler<LinkFilterSettings> SH                  = (StorageHandler<LinkFilterSettings>) CFG.getStorageHandler();
    // let's do this mapping here. We get a nullpointer on init if mapping is
    // wrong.
    public static final KeyHandler                         LINK_FILTER_ENABLED = CFG.getStorageHandler().getKeyHandler("LinkFilterEnabled");

    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<LinkgrabberFilterRule> getFilterList();

    void setFilterList(ArrayList<LinkgrabberFilterRule> list);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberQuickSettingsVisible();

    void setLinkgrabberQuickSettingsVisible(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isLinkFilterEnabled();

    void setLinkFilterEnabled(boolean b);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberHosterQuickfilterEnabled();

    void setLinkgrabberHosterQuickfilterEnabled(boolean b);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberFiletypeQuickfilterEnabled();

    void setLinkgrabberFiletypeQuickfilterEnabled(boolean b);

    @DefaultBooleanValue(false)
    boolean isLinkgrabberAddAtTop();

    void setLinkgrabberAddAtTop(boolean selected);

    @DefaultBooleanValue(true)
    boolean isLinkgrabberAutoStartEnabled();

    void setLinkgrabberAutoStartEnabled(boolean selected);

    @DefaultBooleanValue(false)
    boolean isLinkgrabberAutoConfirmEnabled();

    void setLinkgrabberAutoConfirmEnabled(boolean selected);

    @DefaultBooleanValue(false)
    boolean isRuleconditionsRegexEnabled();

    void setRuleconditionsRegexEnabled(boolean b);
}
