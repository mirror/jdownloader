package jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESGenericConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESService;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHABROTHERHOOD;

public class CaptchaBrotherhoodService implements CESService {

    @Override
    public ImageIcon getIcon(int i) {
        return NewTheme.I().getIcon("cbh", i);
    }

    @Override
    public String getDisplayName() {
        return "captchabrotherhood.com";
    }

    @Override
    public CESGenericConfigPanel createPanel() {
        CESGenericConfigPanel ret = new CESGenericConfigPanel(this) {

            private TextInput     userName;
            private PasswordInput passWord;

            {
                addHeader(getDisplayName(), NewTheme.I().getIcon("cbh", 32));
                addDescription(_GUI._.AntiCaptchaConfigPanel_onShow_description_ces());

                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI._.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.captchabrotherhood.com/");

                    }
                }), "gapleft 37,spanx,pushx,growx");

                userName = new TextInput(CFG_CAPTCHABROTHERHOOD.USER);
                passWord = new PasswordInput(CFG_CAPTCHABROTHERHOOD.PASS);
                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI._.captchabrotherhoodService_createPanel_logins_());

                addPair(_GUI._.captchabrotherhoodService_createPanel_enabled(), null, new Checkbox(CFG_CAPTCHABROTHERHOOD.ENABLED, userName, passWord));
                addPair(_GUI._.captchabrotherhoodService_createPanel_username(), null, userName);
                addPair(_GUI._.captchabrotherhoodService_createPanel_password(), null, passWord);

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
        return _GUI._.CaptchaBrotherhoodService_getDescription_tt_();
    }

}
