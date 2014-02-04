package jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESGenericConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESService;
import jd.http.Browser;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSettings;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;

public class NinekwService implements CESService {
    private ExtButton          btnRegister;
    private ExtButton          btnApi;
    private ExtButton          btnPlugins;
    private ExtButton          btnFAQ;
    private ExtButton          btnSupport;
    private ExtButton          btnHelp;
    private ExtButton          btnUserhistory;
    private Captcha9kwSettings config;

    @Override
    public ImageIcon getIcon(int i) {
        return NewTheme.I().getIcon(IconKey.ICON_9KW, i);
    }

    @Override
    public String getDisplayName() {
        return "9kw.eu";
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(true);
        return ret;
    }

    public String getAPIROOT() {
        config = JsonConfig.create(Captcha9kwSettings.class);
        if (config.ishttps()) {
            return "https://www.9kw.eu/";
        } else {
            return "http://www.9kw.eu/";
        }
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
                addHeader(getDisplayName(), NewTheme.I().getIcon(IconKey.ICON_9KW, 32));
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

                MigPanel toolbar1 = new MigPanel("ins 0", "[][][][]", "[]");
                MigPanel toolbar2 = new MigPanel("ins 0", "[][][][]", "[]");
                btnRegister = new ExtButton(new AppAction() {
                    {
                        setName("Register");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/register.html");

                    }
                });
                btnApi = new ExtButton(new AppAction() {
                    {
                        setName("API management");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/userapi.html");

                    }
                });
                btnPlugins = new ExtButton(new AppAction() {
                    {
                        setName("Plugins");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/plugins.html");

                    }
                });
                btnFAQ = new ExtButton(new AppAction() {
                    {
                        setName("FAQ");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/faq.html");

                    }
                });
                btnHelp = new ExtButton(new AppAction() {
                    {
                        setName("Help");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/hilfe.html");

                    }
                });
                btnSupport = new ExtButton(new AppAction() {
                    {
                        setName("Support");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/kontakt.html");

                    }
                });
                btnUserhistory = new ExtButton(new AppAction() {
                    {
                        setName("History");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/userhistory.html");

                    }
                });
                toolbar1.add(btnRegister, "pushx,growx,sg 1,height 26!");
                toolbar1.add(btnPlugins, "pushx,growx,sg 1,height 26!");
                toolbar1.add(btnFAQ, "pushx,growx,sg 1,height 26!");
                toolbar1.add(btnHelp, "pushx,growx,sg 1,height 26!");
                add(toolbar1, "gapleft 37,spanx,pushx,growx");

                toolbar2.add(btnApi, "pushx,growx,sg 1,height 26!");
                toolbar2.add(btnUserhistory, "pushx,growx,sg 1,height 26!");
                toolbar2.add(btnSupport, "pushx,growx,sg 1,height 26!");
                add(toolbar2, "gapleft 37,spanx,pushx,growx");

                apiKey = new TextInput(CFG_9KWCAPTCHA.API_KEY);
                blacklist = new TextInput(CFG_9KWCAPTCHA.BLACKLIST);
                whitelist = new TextInput(CFG_9KWCAPTCHA.WHITELIST);
                blacklistprio = new TextInput(CFG_9KWCAPTCHA.BLACKLISTPRIO);
                whitelistprio = new TextInput(CFG_9KWCAPTCHA.WHITELISTPRIO);

                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI._.NinekwService_createPanel_logins_());

                MigPanel toolbar3 = new MigPanel("ins 0", "[][][]", "[]");
                // toolbar3.add(btnHelp, "sg 1,height 26!");
                // add(toolbar3, "gapleft 37,spanx,pushx,growx");
                toolbar3.add(label(_GUI._.NinekwService_createPanel_enabled()), "width 135!");
                toolbar3.add(new Checkbox(CFG_9KWCAPTCHA.ENABLED));
                toolbar3.add(label("Text captchas"));
                add(toolbar3, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_enabled(), null, );

                MigPanel toolbar4 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar4.add(label(_GUI._.NinekwService_createPanel_apikey()), "width 135!");
                toolbar4.add(apiKey, "pushx,growx");

                btnUserhistory = new ExtButton(new AppAction() {
                    {
                        setName("Check");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Browser br = new Browser();
                            String accountcheck = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchaguthaben&okcheck=1&apikey=" + CFG_9KWCAPTCHA.API_KEY);
                            if (accountcheck.startsWith("OK-")) {
                                jd.gui.UserIO.getInstance().requestMessageDialog("9kw message ", "Account OK");
                            } else {
                                jd.gui.UserIO.getInstance().requestMessageDialog("9kw error(1) ", "No connection or incorrect api key.");
                            }
                        } catch (IOException e9kw) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw error(2) ", "No connection or incorrect api key.");
                        }
                    }
                });
                toolbar4.add(btnUserhistory);
                add(toolbar4, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_apikey(), null, apiKey);

                // addPair(_GUI._.NinekwService_createPanel_mouse(), null, new Checkbox(CFG_9KWCAPTCHA.MOUSE));
                MigPanel toolbar5 = new MigPanel("ins 0", "[][][]", "[]");
                toolbar5.add(label(_GUI._.NinekwService_createPanel_mouse()), "width 135!");
                toolbar5.add(new Checkbox(CFG_9KWCAPTCHA.MOUSE));
                toolbar5.add(label("Mouse captchas"));
                add(toolbar5, "gapleft 33,spanx,pushx,growx");

                MigPanel toolbar6 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar6.add(label(" "), "width 135!");
                toolbar6.add(new Checkbox(CFG_9KWCAPTCHA.FEEDBACK));
                toolbar6.add(label(_GUI._.NinekwService_createPanel_feedback()));
                toolbar6.add(new Checkbox(CFG_9KWCAPTCHA.HTTPS));
                toolbar6.add(label(_GUI._.NinekwService_createPanel_https()));
                toolbar6.add(new Checkbox(CFG_9KWCAPTCHA.SELFSOLVE));
                toolbar6.add(label(_GUI._.NinekwService_createPanel_selfsolve()));
                add(toolbar6, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_feedback(), null, new Checkbox(CFG_9KWCAPTCHA.FEEDBACK));
                // addPair(_GUI._.NinekwService_createPanel_https(), null, new Checkbox(CFG_9KWCAPTCHA.HTTPS));
                // addPair(_GUI._.NinekwService_createPanel_selfsolve(), null, new Checkbox(CFG_9KWCAPTCHA.SELFSOLVE));

                MigPanel toolbar7 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar7.add(label(" "), "width 135!");
                toolbar7.add(new Checkbox(CFG_9KWCAPTCHA.CONFIRM));
                toolbar7.add(label(_GUI._.NinekwService_createPanel_confirm()));
                toolbar7.add(new Checkbox(CFG_9KWCAPTCHA.MOUSECONFIRM));
                toolbar7.add(label(_GUI._.NinekwService_createPanel_mouseconfirm()));
                add(toolbar7, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_confirm(), null, new Checkbox(CFG_9KWCAPTCHA.CONFIRM));
                // addPair(_GUI._.NinekwService_createPanel_mouseconfirm(), null, new Checkbox(CFG_9KWCAPTCHA.MOUSECONFIRM));

                MigPanel toolbar8 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar8.add(label(" "), "width 135!");
                toolbar8.add(new Spinner(CFG_9KWCAPTCHA.PRIO));
                toolbar8.add(label(_GUI._.NinekwService_createPanel_prio()));
                toolbar8.add(new Spinner(CFG_9KWCAPTCHA.HOUR));
                toolbar8.add(label(_GUI._.NinekwService_createPanel_hour()));
                add(toolbar8, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_prio(), null, new Spinner(CFG_9KWCAPTCHA.PRIO));
                // addPair(_GUI._.NinekwService_createPanel_hour(), null, new Spinner(CFG_9KWCAPTCHA.HOUR));

                MigPanel toolbar9 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar9.add(label(" "), "width 135!");
                toolbar9.add(new Spinner(CFG_9KWCAPTCHA.THREADPOOL));
                toolbar9.add(label(_GUI._.NinekwService_createPanel_threadsizepool()));
                toolbar9.add(new Spinner(CFG_9KWCAPTCHA.Timeout9kw));
                toolbar9.add(label(_GUI._.NinekwService_createPanel_9kwtimeout()));
                add(toolbar9, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_threadsizepool(), null, new Spinner(CFG_9KWCAPTCHA.THREADPOOL));
                // addPair(_GUI._.NinekwService_createPanel_9kwtimeout(), null, new Spinner(CFG_9KWCAPTCHA.Timeout9kw));

                MigPanel toolbar10 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar10.add(label(_GUI._.NinekwService_createPanel_blacklistcheck()), "width 135!");
                toolbar10.add(new Checkbox(CFG_9KWCAPTCHA.BLACKLISTCHECK));
                toolbar10.add(label(_GUI._.NinekwService_createPanel_blacklist()));
                toolbar10.add(blacklist, "pushx,growx");
                add(toolbar10, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_blacklistcheck(), null, new Checkbox(CFG_9KWCAPTCHA.BLACKLISTCHECK));
                // addPair(_GUI._.NinekwService_createPanel_blacklist(), null, blacklist);

                MigPanel toolbar11 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar11.add(label(_GUI._.NinekwService_createPanel_whitelistcheck()), "width 135!");
                toolbar11.add(new Checkbox(CFG_9KWCAPTCHA.WHITELISTCHECK));
                toolbar11.add(label(_GUI._.NinekwService_createPanel_whitelist()));
                toolbar11.add(whitelist, "pushx,growx");
                add(toolbar11, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_whitelistcheck(), null, new Checkbox(CFG_9KWCAPTCHA.WHITELISTCHECK));
                // addPair(_GUI._.NinekwService_createPanel_whitelist(), null, whitelist);

                MigPanel toolbar12 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar12.add(label(_GUI._.NinekwService_createPanel_blacklistpriocheck()), "width 135!");
                toolbar12.add(new Checkbox(CFG_9KWCAPTCHA.BLACKLISTPRIOCHECK));
                toolbar12.add(label(_GUI._.NinekwService_createPanel_blacklistprio()));
                toolbar12.add(blacklistprio, "pushx,growx");
                add(toolbar12, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_blacklistpriocheck(), null, new Checkbox(CFG_9KWCAPTCHA.BLACKLISTPRIOCHECK));
                // addPair(_GUI._.NinekwService_createPanel_blacklistprio(), null, blacklistprio);

                MigPanel toolbar13 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar13.add(label(_GUI._.NinekwService_createPanel_whitelistpriocheck()), "width 135!");
                toolbar13.add(new Checkbox(CFG_9KWCAPTCHA.WHITELISTPRIOCHECK));
                toolbar13.add(label(_GUI._.NinekwService_createPanel_whitelistprio()));
                toolbar13.add(whitelistprio, "pushx,growx");
                add(toolbar13, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_whitelistpriocheck(), null, new Checkbox(CFG_9KWCAPTCHA.WHITELISTPRIOCHECK));
                // addPair(_GUI._.NinekwService_createPanel_whitelistprio(), null, whitelistprio);
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
