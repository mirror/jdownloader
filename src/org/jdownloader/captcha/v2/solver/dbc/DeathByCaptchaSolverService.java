package org.jdownloader.captcha.v2.solver.dbc;

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
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_DBC;

public class DeathByCaptchaSolverService extends AbstractSolverService implements ServicePanelExtender {
    private DeathByCaptchaSettings config;
    private DeathByCaptchaSolver   solver;

    public DeathByCaptchaSolverService() {
        config = JsonConfig.create(DeathByCaptchaSettings.class);

        AdvancedConfigManager.getInstance().register(config);

        if (!Application.isHeadless()) {
            ServicePanel.getInstance().addExtender(this);
            initServicePanel(CFG_DBC.USER_NAME, CFG_DBC.PASSWORD, CFG_DBC.ENABLED);
        }
    }

    @Override
    public String getType() {
        return _GUI._.DeathByCaptchaSolver_getName_();
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_DBC, size);
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {
            private TextInput     username;
            private TextInput     blacklist;
            private TextInput     whitelist;
            private PasswordInput password;

            @Override
            public String getPanelID() {
                return "CES_" + getTitle();
            }

            {
                addHeader(getTitle(), NewTheme.I().getIcon(ID, 32));
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

            @Override
            public Icon getIcon() {
                return DeathByCaptchaSolverService.this.getIcon(32);
            }

            @Override
            public String getTitle() {
                return "deathbycaptcha.eu";
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
        return _GUI._.DeathByCaptchaSolver_gettypeName_();
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (solver.validateLogins()) {
            services.add(new ServiceCollection<DeathByCaptchaSolver>() {

                @Override
                public Icon getIcon() {
                    return DomainInfo.getInstance("deathbycaptcha.eu").getFavIcon();
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
                    return "deathbycaptcha.eu";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new ServicePanelDBCTooltip(owner, solver);
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
        // ret.put(Captcha9kwSolver.ID, 60000);
        // ret.put(CaptchaMyJDSolver.ID, 60000);
        // ret.put(CBSolver.ID, 60000);
        // ret.put(DeathByCaptchaSolver.ID, 60000);

        return ret;
    }

    public static final String ID = "dbc";

    @Override
    public String getID() {
        return ID;
    }

    public void setSolver(DeathByCaptchaSolver deathByCaptchaSolver) {
        this.solver = deathByCaptchaSolver;
    }
}
