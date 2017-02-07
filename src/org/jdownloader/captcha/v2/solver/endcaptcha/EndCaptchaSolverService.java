package org.jdownloader.captcha.v2.solver.endcaptcha;

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
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzSolverService;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.captcha.v2.solver.twocaptcha.TwoCaptchaSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_END_CAPTCHA;

public class EndCaptchaSolverService extends AbstractSolverService implements ServicePanelExtender {
    private EndCaptchaConfigInterface config;
    private EndCaptchaSolver          solver;

    public EndCaptchaSolverService() {
        config = JsonConfig.create(EndCaptchaConfigInterface.class);

        AdvancedConfigManager.getInstance().register(config);

        if (!Application.isHeadless()) {
            ServicePanel.getInstance().addExtender(this);
            initServicePanel(CFG_END_CAPTCHA.USER_NAME, CFG_END_CAPTCHA.PASSWORD, CFG_END_CAPTCHA.ENABLED);
        }
    }

    @Override
    public String getType() {
        return _GUI.T.CaptchaSolver_Type_paid_online();
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon(IconKey.ICON_LOGO_ENDCAPTCHA, size);
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
                addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_ENDCAPTCHA, 32));
                addDescription(_GUI.T.AntiCaptchaConfigPanel_onShow_description_paid_service());

                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI.T.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.endCaptcha.com/");

                    }
                }), "gapleft 37,spanx,pushx,growx");
                username = new TextInput(CFG_END_CAPTCHA.USER_NAME);
                password = new PasswordInput(CFG_END_CAPTCHA.PASSWORD);

                this.addHeader(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), new AbstractIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI.T.captchasolver_configpanel_my_account_description(EndCaptchaSolverService.this.getName()));
                addPair(_GUI.T.captchasolver_configpanel_enabled(EndCaptchaSolverService.this.getName()), null, new Checkbox(CFG_END_CAPTCHA.ENABLED, username, password));
                addPair(_GUI.T.captchabrotherhoodService_createPanel_username(), null, username);
                addPair(_GUI.T.captchabrotherhoodService_createPanel_password(), null, password);

                addPair(_GUI.T.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_END_CAPTCHA.FEED_BACK_SENDING_ENABLED));

                addBlackWhiteList(CFG_END_CAPTCHA.CFG);

            }

            @Override
            public void save() {

            }

            @Override
            public void updateContents() {
            }

            @Override
            public Icon getIcon() {
                return EndCaptchaSolverService.this.getIcon(32);
            }

            @Override
            public String getTitle() {
                return "EndCaptcha.com";
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
        return _GUI.T.EndCaptchaSolver_gettypeName_();
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (solver.validateLogins()) {
            services.add(new ServiceCollection<EndCaptchaSolver>() {

                @Override
                public Icon getIcon() {
                    return EndCaptchaSolverService.this.getIcon(18);
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
                    return "EndCaptcha.com";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new EndCaptchaTooltip(owner, solver);
                }

            });
        }
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();

        ret.put(JacSolverService.ID, 30000);
        ret.put(NineKwSolverService.ID, 120000);
        ret.put(DeathByCaptchaSolverService.ID, 60000);
        ret.put(ImageTyperzSolverService.ID, 60000);
        ret.put(CheapCaptchaSolverService.ID, 60000);
        ret.put(TwoCaptchaSolverService.ID, 60000);

        return ret;
    }

    public static final String ID = "endcaptcha";

    @Override
    public String getID() {
        return ID;
    }

    public void setSolver(EndCaptchaSolver solver) {
        this.solver = solver;
    }
}
