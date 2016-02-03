package jd.gui.swing.jdgui.views.myjd.panels;

import javax.swing.Icon;

import org.jdownloader.gui.translate._GUI;

import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;

public class MyJDownloaderSettingsPanelForTab extends MyJDownloaderSettingsPanel {
    public MyJDownloaderSettingsPanelForTab() {
        super();

    }

    @Override
    public String getTitle() {
        return _GUI._.MyJDownloaderSettingsPanelForTab_title();
    }

    @Override
    public Icon getIcon() {
        return super.getIcon();
        // return new AbstractIcon(IconKey.ICON_LOGINS, 32);
    }

}
