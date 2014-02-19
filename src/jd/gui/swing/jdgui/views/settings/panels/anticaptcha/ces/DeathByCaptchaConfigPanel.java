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
import org.jdownloader.settings.staticreferences.CFG_DBC;

public class DeathByCaptchaConfigPanel implements CESService {

    @Override
    public ImageIcon getIcon(int i) {
        return NewTheme.I().getIcon("dbc", i);
    }

    @Override
    public String getDisplayName() {
        return "deathbycaptcha.eu";
    }

    @Override
    public CESGenericConfigPanel createPanel() {
        CESGenericConfigPanel ret = new CESGenericConfigPanel(this) {
            private TextInput     username;
            private TextInput     blacklist;
            private TextInput     whitelist;
            private PasswordInput password;

            @Override
            public String getPanelID() {
                return "CES_" + getDisplayName();
            }

            {
                addHeader(getDisplayName(), NewTheme.I().getIcon("dbc", 32));
                addDescription(_GUI._.AntiCaptchaConfigPanel_onShow_description_ces());

                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI._.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.deathbycaptcha.eu/");

                    }
                }), "gapleft 37,spanx,pushx,growx");
                username = new TextInput(CFG_DBC.USER_NAME);
                password = new PasswordInput(CFG_DBC.PASSWORD);
                blacklist = new TextInput(CFG_DBC.BLACK_LIST);
                whitelist = new TextInput(CFG_DBC.WHITE_LIST);

                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI._.dbcService_createPanel_logins_());
                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_enabled(), null, new Checkbox(CFG_DBC.ENABLED, username, password));
                addPair(_GUI._.captchabrotherhoodService_createPanel_username(), null, username);
                addPair(_GUI._.captchabrotherhoodService_createPanel_password(), null, password);

                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_DBC.FEED_BACK_SENDING_ENABLED));

                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_blacklist(), null, blacklist);
                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_whitelist(), null, whitelist);

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
        return _GUI._.DeatchbyCaptcha_Service_getDescription_tt_();
    }

}
