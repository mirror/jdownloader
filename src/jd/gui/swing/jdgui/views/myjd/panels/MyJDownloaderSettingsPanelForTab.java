package jd.gui.swing.jdgui.views.myjd.panels;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class MyJDownloaderSettingsPanelForTab extends MyJDownloaderSettingsPanel {
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
