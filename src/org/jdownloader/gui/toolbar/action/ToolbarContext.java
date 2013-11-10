package org.jdownloader.gui.toolbar.action;

import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;

public class ToolbarContext implements ActionContext {
    public static final String ITEM_VISIBLE_FOR_SELECTIONS = "itemVisibleForSelections";
    private boolean            visibleInDownloadTab        = true;

    @Customizer(name = "Visible in Download Tab")
    public boolean isVisibleInDownloadTab() {
        return visibleInDownloadTab;
    }

    public void setVisibleInDownloadTab(boolean visibleInDownloadTab) {
        this.visibleInDownloadTab = visibleInDownloadTab;
    }

    private boolean visibleInLinkgrabberTab = true;

    @Customizer(name = "Visible in Linkgrabber Tab")
    public boolean isVisibleInLinkgrabberTab() {
        return visibleInLinkgrabberTab;
    }

    public void setVisibleInLinkgrabberTab(boolean visibleInDownloadTab) {
        this.visibleInLinkgrabberTab = visibleInDownloadTab;
    }

    private boolean visibleInAllTabs = true;

    @Customizer(name = "Visible in All Tab")
    public boolean isVisibleInAllTabs() {
        return visibleInAllTabs;
    }

    public void setVisibleInAllTabs(boolean visibleInDownloadTab) {
        this.visibleInAllTabs = visibleInDownloadTab;
    }

}
