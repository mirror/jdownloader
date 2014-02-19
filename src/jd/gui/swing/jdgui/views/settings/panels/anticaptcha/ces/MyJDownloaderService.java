package jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESGenericConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESService;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class MyJDownloaderService implements CESService {

    @Override
    public ImageIcon getIcon(int i) {
        return NewTheme.I().getIcon("myjdownloader", i);
    }

    @Override
    public String getDisplayName() {
        return "My.JDownloader";
    }

    @Override
    public CESGenericConfigPanel createPanel() {
        CESGenericConfigPanel ret = new CESGenericConfigPanel(this) {
            @Override
            public String getPanelID() {
                return "CES_" + getDisplayName();
            }

            {
                addHeader(getDisplayName(), NewTheme.I().getIcon("myjdownloader", 32));
                addDescription(_GUI._.MyJDownloaderService_createPanel_description_());
                SettingsButton openMyJDownloader = new SettingsButton(new AppAction() {
                    {
                        setName(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_open_());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                        ConfigurationView.getInstance().setSelectedSubPanel(MyJDownloaderSettingsPanel.class);

                    }
                });
                add(openMyJDownloader, "gapleft 37,spanx,pushx,growx");
            }

            @Override
            public void save() {
            }

            @Override
            public void updateContents() {
            }

        };
        return ret;
    }

    @Override
    public String getDescription() {
        return _GUI._.MyJDownloaderService_getDescription_tt_();
    }

    // http://www.9kw.eu/hilfe.html#jdownloader-tab

}
