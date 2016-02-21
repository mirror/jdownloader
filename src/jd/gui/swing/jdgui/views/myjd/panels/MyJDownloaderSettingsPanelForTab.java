package jd.gui.swing.jdgui.views.myjd.panels;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import org.appwork.utils.Application;
import org.jdownloader.actions.AppAction;
import org.jdownloader.api.myjdownloader.remotemenu.MenuManagerMYJDDownloadTableContext;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;

public class MyJDownloaderSettingsPanelForTab extends MyJDownloaderSettingsPanel {
    public MyJDownloaderSettingsPanelForTab() {
        super();
        if (!Application.isJared(null)) {
            // this.addDescriptionPlain(_GUI.T.MyJDownloaderSettingsPanelForTab_menus());
            this.addHeader(_GUI.T.MyJDownloaderSettingsPanelForTab_menus(), NewTheme.I().getIcon(IconKey.ICON_MENU, 32));
            this.addDescription(_GUI.T.gui_config_menumanager_desc());

            SettingsButton bt = new SettingsButton(new AppAction() {
                {
                    setName("Download List Right Click Menu");
                }

                @Override
                public void actionPerformed(ActionEvent e) {

                    MenuManagerMYJDDownloadTableContext.getInstance().openGui();
                }

            });
            this.addPair("", null, bt);
        }

    }

    @Override
    public String getTitle() {
        return _GUI.T.MyJDownloaderSettingsPanelForTab_title();
    }

    @Override
    public Icon getIcon() {
        return super.getIcon();
        // return new AbstractIcon(IconKey.ICON_LOGINS, 32);
    }

}
