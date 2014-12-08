package org.jdownloader.gui.toolbar.action;

import javax.swing.AbstractButton;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public abstract class AbstractToolBarAction extends CustomizableAppAction implements ActionContext, GUIListener {
    @Override
    public void onKeyModifier(int parameter) {
    }

    public AbstractToolBarAction() {
        super();

        GUIEventSender.getInstance().addListener(this, true);
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {

        if (isVisibleInAllTabs()) {
            setVisible(true);
            return;
        }

        if (isVisibleInDownloadTab() && newView instanceof DownloadsView) {
            setVisible(true);
            return;
        }
        if (isVisibleInLinkgrabberTab() && newView instanceof LinkGrabberView) {
            setVisible(true);
            return;
        }

        setVisible(false);

    }

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

    protected String createMnemonic() {
        return "-";
    }

    public String getIconKey() {
        if (StringUtils.isEmpty(super.getIconKey())) {
            return IconKey.ICON_QUESTION;
        }
        if (MenuItemData.isEmptyValue(super.getIconKey())) {
            return IconKey.ICON_QUESTION;
        }
        return iconKey;
    }

    public Object getValue(String key) {
        if (LARGE_ICON_KEY == (key)) {
            return NewTheme.I().getIcon(getIconKey(), 24);
        }
        if (SMALL_ICON == (key)) {
            return NewTheme.I().getIcon(getIconKey(), 18);
        }
        if (MNEMONIC_KEY == key || DISPLAYED_MNEMONIC_INDEX_KEY == key) {
            Object ret = super.getValue(key);
            if (ret == null) {
                if (getName() == null) {
                    setName(createTooltip());
                }
                setMnemonic(createMnemonic());
            }
            return super.getValue(key);
        }
        if (SHORT_DESCRIPTION == key) {
            return createTooltip();
        }
        return super.getValue(key);
    }

    protected abstract String createTooltip();

    public AbstractButton createButton() {
        return null;
    }
}
