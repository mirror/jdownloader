package org.jdownloader.captcha.v2.solver.twocaptcha;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;
import jd.http.Browser;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_TWO_CAPTCHA;

public final class TwoCaptchaConfigPanel extends AbstractCaptchaSolverConfigPanel {
    /**
     *
     */
    private static final long       serialVersionUID = -6345775244208176097L;
    private ExtButton               btnUserCheck;
    private TextInput               apiKey;
    private TwoCaptchaSolverService service;

    public TwoCaptchaConfigPanel(TwoCaptchaSolverService twoCaptchaSolverService) {
        this.service = twoCaptchaSolverService;
        addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_2CAPTCHA, 32));
        addDescription(_GUI.T.AntiCaptchaConfigPanel_onShow_description_paid_service());
        add(new SettingsButton(new AppAction() {
            private static final long serialVersionUID = 546456487846L;
            {
                setName(_GUI.T.lit_open_website());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                CrossSystem.openURL("http://2captcha.com/");
            }
        }), "gapleft 37,spanx,pushx,growx");
        apiKey = new TextInput(CFG_TWO_CAPTCHA.API_KEY);
        this.addHeader(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), new AbstractIcon(IconKey.ICON_LOGINS, 32));
        // addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
        this.addDescriptionPlain(_GUI.T.captchasolver_configpanel_my_account_description(service.getName()));
        addPair(_GUI.T.captchasolver_configpanel_enabled(service.getName()), null, new Checkbox(CFG_TWO_CAPTCHA.ENABLED, apiKey));
        MigPanel toolbar = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar.add(label(_GUI.T.lit_api_key()));// , "width 135!");
        toolbar.add(apiKey, "pushx,growx");
        btnUserCheck = new ExtButton(new AppAction() {
            private static final long serialVersionUID = -103695205004891917L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserCheck());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (apiKey.getText().length() < 5) {
                    jd.gui.UserIO.getInstance().requestMessageDialog("2Captcha.com Error", "No api key.");
                } else if (!apiKey.getText().matches("^[a-f0-9]+$")) {
                    jd.gui.UserIO.getInstance().requestMessageDialog("2Captcha.com Error", "API Key is not correct!" + "\n" + "Only a-f and 0-9");
                } else {
                    try {
                        Browser br = new Browser();
                        String accountcheck = br.getPage("http://2captcha.com/res.php?action=getbalance&key=" + apiKey.getText());
                        String validcheck = br.getRegex("^([0-9.]+$)").getMatch(0);
                        if (validcheck != null) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("2Captcha.com message ", "Account OK\nCredits: " + accountcheck);
                        } else if (accountcheck.startsWith("ERROR")) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("2Captcha.com Error", "Account error\n" + accountcheck);
                        } else {
                            jd.gui.UserIO.getInstance().requestMessageDialog("2Captcha.com Error(1)", "Unknown error.");
                        }
                    } catch (IOException e9kw) {
                        jd.gui.UserIO.getInstance().requestMessageDialog("2Captcha.com Error(2) ", "No connection or unknown error.");
                    }
                }
            }
        });
        btnUserCheck.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserCheck_tooltiptext());
        toolbar.add(btnUserCheck);
        add(toolbar, "gapleft 40, spanx,pushx,growx");// , "gapleft 33,spanx,pushx,growx");
        // addPair(_GUI.T.lit_api_key(), null, toolbar);
        addPair(_GUI.T.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_TWO_CAPTCHA.FEED_BACK_SENDING_ENABLED));
        addBlackWhiteList(CFG_TWO_CAPTCHA.CFG);
    }

    @Override
    public String getPanelID() {
        return "CES_" + getTitle();
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

    @Override
    public Icon getIcon() {
        return service.getIcon(32);
    }

    @Override
    public String getTitle() {
        return "2captcha.com/rucaptcha.com";
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(true);
        return ret;
    }
}