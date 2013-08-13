package jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESGenericConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESService;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;

public class NinekwService implements CESService {

    @Override
    public ImageIcon getIcon(int i) {
        return NewTheme.I().getIcon("9kw", i);
    }

    @Override
    public String getDisplayName() {
        return "9kw.eu";
    }

    @Override
    public CESGenericConfigPanel createPanel() {
        CESGenericConfigPanel ret = new CESGenericConfigPanel(this) {
            private TextInput apiKey;

            {
                addHeader(getDisplayName(), NewTheme.I().getIcon("9kw", 32));
                addDescription(_GUI._.AntiCaptchaConfigPanel_onShow_description_ces());

                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI._.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://9kw.eu");

                    }
                }), "gapleft 37,spanx,pushx,growx");
                apiKey = new TextInput(CFG_9KWCAPTCHA.API_KEY);

                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI._.NinekwService_createPanel_logins_());
                addPair(_GUI._.NinekwService_createPanel_enabled(), null, new Checkbox(CFG_9KWCAPTCHA.ENABLED, apiKey));
                addPair(_GUI._.NinekwService_createPanel_apikey(), null, apiKey);

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
    // http://www.9kw.eu/hilfe.html#jdownloader-tab

}
