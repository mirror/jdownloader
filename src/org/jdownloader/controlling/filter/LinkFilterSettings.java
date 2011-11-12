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
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.translate._JDT;

public interface LinkFilterSettings extends ConfigInterface {

    public static final LinkFilterSettings                 CFG                                        = JsonConfig.create(LinkFilterSettings.class);
    public static final StorageHandler<LinkFilterSettings> SH                                         = (StorageHandler<LinkFilterSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    public static final BooleanKeyHandler                  LINKGRABBER_ADD_AT_TOP                     = SH.getKeyHandler("LinkgrabberAddAtTop", BooleanKeyHandler.class);
    /**
     * true to show filterexceptions as quickfilters in linkgrabber sidebar
     **/
    public static final BooleanKeyHandler                  EXCEPTION_AS_QUICKFILTER_ENABLED           = SH.getKeyHandler("ExceptionAsQuickfilterEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LINKGRABBER_QUICK_SETTINGS_VISIBLE         = SH.getKeyHandler("LinkgrabberQuickSettingsVisible", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LINK_FILTER_ENABLED                        = SH.getKeyHandler("LinkFilterEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED   = SH.getKeyHandler("LinkgrabberFiletypeQuickfilterEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LINKGRABBER_AUTO_START_ENABLED             = SH.getKeyHandler("LinkgrabberAutoStartEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LINKGRABBER_HOSTER_QUICKFILTER_ENABLED     = SH.getKeyHandler("LinkgrabberHosterQuickfilterEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  RULECONDITIONS_REGEX_ENABLED               = SH.getKeyHandler("RuleconditionsRegexEnabled", BooleanKeyHandler.class);
    public static final ObjectKeyHandler                   FILTER_LIST                                = SH.getKeyHandler("FilterList", ObjectKeyHandler.class);
    public static final BooleanKeyHandler                  LINKGRABBER_AUTO_CONFIRM_ENABLED           = SH.getKeyHandler("LinkgrabberAutoConfirmEnabled", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                  LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED = SH.getKeyHandler("LinkgrabberExceptionsQuickfilterEnabled", BooleanKeyHandler.class);

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
