package org.jdownloader.gui.toolbar.action;

import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.translate._JDT;

public class ToolbarContext implements ActionContext {
    public static final String ITEM_VISIBLE_FOR_SELECTIONS = "itemVisibleForSelections";
    private boolean            visibleInDownloadTab        = true;

    public static String getTranslationForVisibleInDownloadTab() {
        return _JDT._.ToolbarContext_getTranslationForVisibleInDownloadTab();
    }

    public static String getTranslationForVisibleInLinkgrabberTab() {
        return _JDT._.ToolbarContext_getTranslationForVisibleInLinkgrabberTab();
    }

    public static String getTranslationForVisibleInAllTabs() {
        return _JDT._.ToolbarContext_getTranslationForVisibleInAllTabs();
    }

    @Customizer(link = "#getTranslationForVisibleInDownloadTab")
    public boolean isVisibleInDownloadTab() {
        return visibleInDownloadTab;
    }

    public void setVisibleInDownloadTab(boolean visibleInDownloadTab) {
        this.visibleInDownloadTab = visibleInDownloadTab;
    }

    private boolean visibleInLinkgrabberTab = true;

    @Customizer(link = "#getTranslationForVisibleInLinkgrabberTab")
    public boolean isVisibleInLinkgrabberTab() {
        return visibleInLinkgrabberTab;
    }

    public void setVisibleInLinkgrabberTab(boolean visibleInDownloadTab) {
        this.visibleInLinkgrabberTab = visibleInDownloadTab;
    }

    private boolean visibleInAllTabs = true;

    @Customizer(link = "#getTranslationForVisibleInAllTabs")
    public boolean isVisibleInAllTabs() {
        return visibleInAllTabs;
    }

    public void setVisibleInAllTabs(boolean visibleInDownloadTab) {
        this.visibleInAllTabs = visibleInDownloadTab;
    }

}
