package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.jdownloader.translate._JDT;

public interface LinkFilterSettings extends ConfigInterface {

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

    @DefaultBooleanValue(true)
    boolean isLinkgrabberExceptionsQuickfilterEnabled();

    void setLinkgrabberExceptionsQuickfilterEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("true to show filterexceptions as quickfilters in linkgrabber sidebar")
    boolean isExceptionAsQuickfilterEnabled();

    void setExceptionAsQuickfilterEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("show a restore button for filtered links")
    boolean isRestoreButtonEnabled();

    void setRestoreButtonEnabled(boolean b);

}
