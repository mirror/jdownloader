package jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
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
            private TextInput blacklist;
            private TextInput whitelist;
            private TextInput blacklistprio;
            private TextInput whitelistprio;
            {
                addHeader(getDisplayName(), NewTheme.I().getIcon("9kw", 32));
                addDescription(_GUI._.AntiCaptchaConfigPanel_onShow_description_ces());

                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI._.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/");

                    }
                }), "gapleft 37,spanx,pushx,growx");
                apiKey = new TextInput(CFG_9KWCAPTCHA.API_KEY);
                blacklist = new TextInput(CFG_9KWCAPTCHA.BLACKLIST);
                whitelist = new TextInput(CFG_9KWCAPTCHA.WHITELIST);
                blacklistprio = new TextInput(CFG_9KWCAPTCHA.BLACKLISTPRIO);
                whitelistprio = new TextInput(CFG_9KWCAPTCHA.WHITELISTPRIO);

                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI._.NinekwService_createPanel_logins_());
                addPair(_GUI._.NinekwService_createPanel_enabled(), null, new Checkbox(CFG_9KWCAPTCHA.ENABLED));
                addPair(_GUI._.NinekwService_createPanel_apikey(), null, apiKey);
                addPair(_GUI._.NinekwService_createPanel_mouse(), null, new Checkbox(CFG_9KWCAPTCHA.MOUSE));
                addPair(_GUI._.NinekwService_createPanel_feedback(), null, new Checkbox(CFG_9KWCAPTCHA.FEEDBACK));
                addPair(_GUI._.NinekwService_createPanel_https(), null, new Checkbox(CFG_9KWCAPTCHA.HTTPS));
                addPair(_GUI._.NinekwService_createPanel_confirm(), null, new Checkbox(CFG_9KWCAPTCHA.CONFIRM));
                addPair(_GUI._.NinekwService_createPanel_mouseconfirm(), null, new Checkbox(CFG_9KWCAPTCHA.MOUSECONFIRM));
                addPair(_GUI._.NinekwService_createPanel_selfsolve(), null, new Checkbox(CFG_9KWCAPTCHA.SELFSOLVE));

                addPair(_GUI._.NinekwService_createPanel_prio(), null, new Spinner(CFG_9KWCAPTCHA.PRIO));
                addPair(_GUI._.NinekwService_createPanel_hour(), null, new Spinner(CFG_9KWCAPTCHA.HOUR));

                addPair(_GUI._.NinekwService_createPanel_blacklistcheck(), null, new Checkbox(CFG_9KWCAPTCHA.BLACKLISTCHECK));
                addPair(_GUI._.NinekwService_createPanel_blacklist(), null, blacklist);
                addPair(_GUI._.NinekwService_createPanel_whitelistcheck(), null, new Checkbox(CFG_9KWCAPTCHA.WHITELISTCHECK));
                addPair(_GUI._.NinekwService_createPanel_whitelist(), null, whitelist);

                addPair(_GUI._.NinekwService_createPanel_blacklistpriocheck(), null, new Checkbox(CFG_9KWCAPTCHA.BLACKLISTPRIOCHECK));
                addPair(_GUI._.NinekwService_createPanel_blacklistprio(), null, blacklistprio);
                addPair(_GUI._.NinekwService_createPanel_whitelistpriocheck(), null, new Checkbox(CFG_9KWCAPTCHA.WHITELISTPRIOCHECK));
                addPair(_GUI._.NinekwService_createPanel_whitelistprio(), null, whitelistprio);
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
        return _GUI._.NinekwService_getDescription_tt_();
    }

    // http://www.9kw.eu/hilfe.html#jdownloader-tab

}
