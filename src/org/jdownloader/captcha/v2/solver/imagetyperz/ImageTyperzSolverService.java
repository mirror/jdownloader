package org.jdownloader.captcha.v2.solver.imagetyperz;

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
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.captcha.v2.solver.twocaptcha.TwoCaptchaSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_IMAGE_TYPERZ;

public class ImageTyperzSolverService extends AbstractSolverService implements ServicePanelExtender {
    private ImageTyperzConfigInterface config;
    private ImageTyperzCaptchaSolver   solver;

    public ImageTyperzSolverService() {
        config = JsonConfig.create(ImageTyperzConfigInterface.class);
        AdvancedConfigManager.getInstance().register(config);
        if (!Application.isHeadless()) {
            ServicePanel.getInstance().addExtender(this);
            initServicePanel(CFG_IMAGE_TYPERZ.USER_NAME, CFG_IMAGE_TYPERZ.PASSWORD, CFG_IMAGE_TYPERZ.ENABLED);
        }
    }

    @Override
    public String getType() {
        return _GUI.T.CaptchaSolver_Type_paid_online();
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon(IconKey.ICON_LOGO_IMAGE_TYPERZ, size);
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
                addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_IMAGE_TYPERZ, 32));
                addDescription(_GUI.T.AntiCaptchaConfigPanel_onShow_description_paid_service());
                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI.T.lit_open_website());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.ImageTyperz.com/");
                    }
                }), "gapleft 37,spanx,pushx,growx");
                username = new TextInput(CFG_IMAGE_TYPERZ.USER_NAME);
                password = new PasswordInput(CFG_IMAGE_TYPERZ.PASSWORD);
                this.addHeader(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI.T.captchasolver_configpanel_my_account_description(ImageTyperzSolverService.this.getName()));
                addPair(_GUI.T.captchasolver_configpanel_enabled(ImageTyperzSolverService.this.getName()), null, new Checkbox(CFG_IMAGE_TYPERZ.ENABLED, username, password));
                addPair(_GUI.T.captchabrotherhoodService_createPanel_username(), null, username);
                addPair(_GUI.T.captchabrotherhoodService_createPanel_password(), null, password);
                addPair(_GUI.T.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_IMAGE_TYPERZ.FEED_BACK_SENDING_ENABLED));
                addBlackWhiteList(CFG_IMAGE_TYPERZ.CFG);
            }

            @Override
            public void save() {
            }

            @Override
            public void updateContents() {
            }

            @Override
            public Icon getIcon() {
                return ImageTyperzSolverService.this.getIcon(32);
            }

            @Override
            public String getTitle() {
                return "ImageTyperz.com";
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
        return _GUI.T.ImageTyperzSolver_gettypeName_();
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (solver.validateLogins()) {
            services.add(new ServiceCollection<ImageTyperzCaptchaSolver>() {
                @Override
                public Icon getIcon() {
                    return DomainInfo.getInstance("ImageTyperz.com").getFavIcon();
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
                    return "ImageTyperz.com";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new ServicePanelImageTyperzTooltip(owner, solver);
                }
            });
        }
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        ret.put(JacSolverService.ID, 30000);
        // ret.put(DeathByCaptchaSolverService.ID, 60000);
        ret.put(NineKwSolverService.ID, 120000);
        // ret.put(CaptchaMyJDSolverService.ID, 60000);
        // ret.put(CheapCaptchaSolverService.ID, 60000);
        // ret.put(EndCaptchaSolverService.ID, 60000);
        ret.put(TwoCaptchaSolverService.ID, 60000);
        return ret;
    }

    public static final String ID = "imagetyperz";

    @Override
    public String getID() {
        return ID;
    }

    public void setSolver(ImageTyperzCaptchaSolver solver) {
        this.solver = solver;
    }
}
