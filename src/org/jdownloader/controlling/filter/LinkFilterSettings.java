package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.translate._JDT;

public interface LinkFilterSettings extends ConfigInterface {

    public static final LinkFilterSettings                 CFG                               = JsonConfig.create(LinkFilterSettings.class);
    public static final StorageHandler<LinkFilterSettings> SH                                = (StorageHandler<LinkFilterSettings>) CFG.getStorageHandler();
    // let's do this mapping here. We get a nullpointer on init if mapping is
    // wrong.
    public static final BooleanKeyHandler                  LINK_FILTER_ENABLED               = SH.getKeyHandler("LinkFilterEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  ADD_AT_TOP                        = SH.getKeyHandler("LinkgrabberAddAtTop", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  AUTO_CONFIRM_ENABLED              = SH.getKeyHandler("LinkgrabberAutoConfirmEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  AUTO_START_ENABLED                = SH.getKeyHandler("LinkgrabberAutoStartEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LG_QUICKSETTINGS_VISIBLE          = SH.getKeyHandler("LinkgrabberQuickSettingsVisible", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LG_QUICKFILTER_TYPE_VISIBLE       = SH.getKeyHandler("LinkgrabberFiletypeQuickfilterEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                  LG_QUICKFILTER_HOSTER_VISIBLE     = SH.getKeyHandler("LinkgrabberHosterQuickfilterEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LG_QUICKFILTER_EXCEPTIONS_VISIBLE = SH.getKeyHandler("LinkgrabberExceptionsQuickfilterEnabled", BooleanKeyHandler.class); ;

    class DefaultFilterList extends AbstractDefaultFactory<ArrayList<LinkgrabberFilterRule>> {

        @Override
        public ArrayList<LinkgrabberFilterRule> getDefaultValue() {

            LinkgrabberFilterRule offline = new LinkgrabberFilterRule();
            offline.setOnlineStatusFilter(new OnlineStatusFilter(OnlineStatusMatchtype.ISNOT, true, OnlineStatus.ONLINE));
            offline.setName(_JDT._.LinkFilterSettings_DefaultFilterList_getDefaultValue_());
            offline.setIconKey("error");
            offline.setAccept(true);
            offline.setEnabled(true);
            ArrayList<LinkgrabberFilterRule> ret = new ArrayList<LinkgrabberFilterRule>();
            ret.add(offline);
            return ret;
        }
    }

    @DefaultFactory(DefaultFilterList.class)
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

    @DefaultBooleanValue(true)
    boolean isLinkgrabberExceptionsQuickfilterEnabled();

    void setLinkgrabberExceptionsQuickfilterEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @org.appwork.storage.config.annotations.Description("true to show filterexceptions as quickfilters in linkgrabber sidebar")
    boolean isExceptionAsQuickfilterEnabled();

    void setExceptionAsQuickfilterEnabled(boolean b);

}
