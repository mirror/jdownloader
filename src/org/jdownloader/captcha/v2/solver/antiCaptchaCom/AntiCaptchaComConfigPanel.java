package org.jdownloader.captcha.v2.solver.antiCaptchaCom;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_ANTICAPTCHA_COM;
import org.jdownloader.statistics.StatsManager;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;
import jd.http.Browser;

public final class AntiCaptchaComConfigPanel extends AbstractCaptchaSolverConfigPanel {
    /**
     *
     */
    private static final long           serialVersionUID = -6345775244208176097L;
    private ExtButton                   btnUserCheck;
    private TextInput                   apiKey;
    private AntiCaptchaComSolverService service;

    public AntiCaptchaComConfigPanel(AntiCaptchaComSolverService AntiCaptchaComSolverService) {
        this.service = AntiCaptchaComSolverService;
        addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_ANTICAPTCHA, 32));
        addDescription(_GUI.T.AntiCaptchaConfigPanel_onShow_description_paid_service());
        add(new SettingsButton(new AppAction() {
            private static final long serialVersionUID = 546456487846L;
            {
                setName(_GUI.T.lit_open_website());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                StatsManager.I().openAfflink("anti-captcha.com", "http://getcaptchasolution.com/pue5rd7req", "ConfigPanel");
            }
        }), "gapleft 37,spanx,pushx,growx");
        apiKey = new TextInput(CFG_ANTICAPTCHA_COM.API_KEY);
        this.addHeader(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), new AbstractIcon(IconKey.ICON_LOGINS, 32));
        // addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
        this.addDescriptionPlain(_GUI.T.captchasolver_configpanel_my_account_description(service.getName()));
        addPair(_GUI.T.captchasolver_configpanel_enabled(service.getName()), null, new Checkbox(CFG_ANTICAPTCHA_COM.ENABLED, apiKey));
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
                    jd.gui.UserIO.getInstance().requestMessageDialog("anti-captcha.com Error", "No api key.");
                } else if (!apiKey.getText().matches("^[a-f0-9]+$")) {
                    jd.gui.UserIO.getInstance().requestMessageDialog("anti-captcha.com Error", "API Key is not correct!" + "\n" + "Only a-f and 0-9");
                } else {
                    try {
                        Browser br = new Browser();
                        HashMap<String, Object> dataMap = new HashMap<String, Object>();
                        dataMap.put("clientKey", apiKey.getText());
                        String json = br.postPageRaw("https://api.anti-captcha.com/getBalance", JSonStorage.serializeToJson(dataMap));
                        HashMap<String, Object> response = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
                        if (new Integer(0).equals(response.get("errorId"))) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("anti-captcha.com message ", "Account OK\n Balance: $" + response.get("balance"));
                        } else {
                            jd.gui.UserIO.getInstance().requestMessageDialog("anti-captcha.com Error", "Account error\n" + response.get("errorDescription"));
                        }
                    } catch (IOException e9kw) {
                        jd.gui.UserIO.getInstance().requestMessageDialog("anti-captcha.com Error(2) ", "No connection or unknown error.");
                    }
                }
            }
        });
        btnUserCheck.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserCheck_tooltiptext());
        toolbar.add(btnUserCheck);
        add(toolbar, "gapleft 40, spanx,pushx,growx");// , "gapleft 33,spanx,pushx,growx");
        // addPair(_GUI.T.lit_api_key(), null, toolbar);
        addPair(_GUI.T.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_ANTICAPTCHA_COM.FEED_BACK_SENDING_ENABLED));
        addBlackWhiteList(CFG_ANTICAPTCHA_COM.CFG);
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
        return "anti-captcha.com";
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(true);
        return ret;
    }
}