package org.jdownloader.captcha.v2.solver.twocaptcha;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_TWO_CAPTCHA;

import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanelExtender;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

public class TwoCaptchaSolverService extends AbstractSolverService implements ServicePanelExtender {
    private TwoCaptchaConfigInterface config;
    private TwoCaptchaSolver          solver;

    public TwoCaptchaSolverService() {
        config = JsonConfig.create(TwoCaptchaConfigInterface.class);
        AdvancedConfigManager.getInstance().register(config);
        if (!Application.isHeadless()) {
            ServicePanel.getInstance().addExtender(this);
            initServicePanel(CFG_TWO_CAPTCHA.API_KEY, CFG_TWO_CAPTCHA.ENABLED);
        }
    }

    @Override
    public String getType() {
        return _GUI.T.CaptchaSolver_Type_paid_online();
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon("logo/2captcha", size);
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {
            private TextInput apiKey;

            @Override
            public String getPanelID() {
                return "CES_" + getTitle();
            }

            {
                addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_2CAPTCHA, 32));
                addDescription(_GUI.T.AntiCaptchaConfigPanel_onShow_description_paid_service());
                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI.T.lit_open_website());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://2captcha.com/?from=1396142");
                    }
                }), "gapleft 37,spanx,pushx,growx");
                apiKey = new TextInput(CFG_TWO_CAPTCHA.API_KEY);
                this.addHeader(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), new AbstractIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI.T.captchasolver_configpanel_my_account_description(TwoCaptchaSolverService.this.getName()));
                addPair(_GUI.T.captchasolver_configpanel_enabled(TwoCaptchaSolverService.this.getName()), null, new Checkbox(CFG_TWO_CAPTCHA.ENABLED, apiKey));
                addPair(_GUI.T.lit_api_key(), null, apiKey);
                addPair(_GUI.T.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_TWO_CAPTCHA.FEED_BACK_SENDING_ENABLED));
                addBlackWhiteList(CFG_TWO_CAPTCHA.CFG);
            }

            @Override
            public void save() {
            }

            @Override
            public void updateContents() {
            }

            @Override
            public Icon getIcon() {
                return TwoCaptchaSolverService.this.getIcon(32);
            }

            @Override
            public String getTitle() {
                return "2Captcha.com";
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
        return "2captcha.com";
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (solver.validateLogins()) {
            services.add(new ServiceCollection<TwoCaptchaSolver>() {
                @Override
                public Icon getIcon() {
                    return TwoCaptchaSolverService.this.getIcon(18);
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
                    return "2Captcha.com";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new TwoCaptchaTooltip(owner, solver);
                }
            });
        }
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        // ret.put(Captcha9kwSolverClick.ID, 60000);
        // ret.put(DialogClickCaptchaSolver.ID, 60000);
        // ret.put(DialogBasicCaptchaSolver.ID, 60000);
        // ret.put(CaptchaAPISolver.ID, 60000);
        ret.put(JacSolverService.ID, 30000);
        // ret.put(DeathByCaptchaSolverService.ID, 60000);
        // ret.put(ImageTyperzSolverService.ID, 60000);
        // ret.put(Captcha9kwSolver.ID, 60000);
        // ret.put(CaptchaMyJDSolver.ID, 60000);
        // ret.put(CBSolver.ID, 60000);
        // ret.put(CheapCaptchaSolver.ID, 60000);
        return ret;
    }

    public static final String ID = "2captcha";

    @Override
    public String getID() {
        return ID;
    }

    public void setSolver(TwoCaptchaSolver solver) {
        this.solver = solver;
    }
}
