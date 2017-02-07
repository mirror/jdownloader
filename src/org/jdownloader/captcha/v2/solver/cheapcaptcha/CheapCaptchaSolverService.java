package org.jdownloader.captcha.v2.solver.cheapcaptcha;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanelExtender;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzSolverService;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.captcha.v2.solver.twocaptcha.TwoCaptchaSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_CHEAP_CAPTCHA;

public class CheapCaptchaSolverService extends AbstractSolverService implements ServicePanelExtender {
    private CheapCaptchaConfigInterface config;
    private CheapCaptchaSolver          solver;

    public CheapCaptchaSolverService() {
        config = JsonConfig.create(CheapCaptchaConfigInterface.class);

        AdvancedConfigManager.getInstance().register(config);

        if (!Application.isHeadless()) {
            ServicePanel.getInstance().addExtender(this);
            initServicePanel(CFG_CHEAP_CAPTCHA.USER_NAME, CFG_CHEAP_CAPTCHA.PASSWORD, CFG_CHEAP_CAPTCHA.ENABLED);
        }
    }

    @Override
    public String getType() {
        return _GUI.T.CaptchaSolver_Type_paid_online();
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon("logo/cheapCaptcha", size);
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {
            private TextInput     username;

            private PasswordInput password;

            @Override
            public String getPanelID() {
                return "CES_" + getTitle();
            }

            {
                addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_CHEAPCAPTCHA, 32));
                addDescription(_GUI.T.AntiCaptchaConfigPanel_onShow_description_paid_service());

                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI.T.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.cheapCaptcha.com/");

                    }
                }), "gapleft 37,spanx,pushx,growx");
                username = new TextInput(CFG_CHEAP_CAPTCHA.USER_NAME);
                password = new PasswordInput(CFG_CHEAP_CAPTCHA.PASSWORD);

                this.addHeader(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), new AbstractIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI.T.captchasolver_configpanel_my_account_description(CheapCaptchaSolverService.this.getName()));
                addPair(_GUI.T.captchasolver_configpanel_enabled(CheapCaptchaSolverService.this.getName()), null, new Checkbox(CFG_CHEAP_CAPTCHA.ENABLED, username, password));
                addPair(_GUI.T.captchabrotherhoodService_createPanel_username(), null, username);
                addPair(_GUI.T.captchabrotherhoodService_createPanel_password(), null, password);

                addPair(_GUI.T.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_CHEAP_CAPTCHA.FEED_BACK_SENDING_ENABLED));

                addBlackWhiteList(CFG_CHEAP_CAPTCHA.CFG);

            }

            @Override
            public void save() {

            }

            @Override
            public void updateContents() {
            }

            @Override
            public Icon getIcon() {
                return CheapCaptchaSolverService.this.getIcon(32);
            }

            @Override
            public String getTitle() {
                return "CheapCaptcha.com";
            }

        };
        return ret;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getName() {
        return _GUI.T.CheapCaptchaSolver_gettypeName_();
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (solver.validateLogins()) {
            services.add(new ServiceCollection<CheapCaptchaSolver>() {

                @Override
                public Icon getIcon() {
                    return CheapCaptchaSolverService.this.getIcon(18);
                }

                @Override
                public boolean isEnabled() {
                    return config.isEnabled();
                }

                @Override
                protected long getLastActiveTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                protected String getName() {
                    return "CheapCaptcha.com";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new CheapCaptchaTooltip(owner, solver);
                }

            });
        }
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();

        ret.put(JacSolverService.ID, 30000);
        ret.put(DeathByCaptchaSolverService.ID, 60000);
        ret.put(ImageTyperzSolverService.ID, 60000);
        ret.put(EndCaptchaSolverService.ID, 60000);
        ret.put(TwoCaptchaSolverService.ID, 60000);
        ret.put(NineKwSolverService.ID, 120000);

        return ret;
    }

    public static final String ID = "cheapcaptcha";

    @Override
    public String getID() {
        return ID;
    }

    public void setSolver(CheapCaptchaSolver solver) {
        this.solver = solver;
    }
}
