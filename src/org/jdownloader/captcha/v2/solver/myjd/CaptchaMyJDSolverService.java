package org.jdownloader.captcha.v2.solver.myjd;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanelExtender;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.Application;
import org.jdownloader.actions.AppAction;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderListener;
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzSolverService;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class CaptchaMyJDSolverService extends AbstractSolverService implements ServicePanelExtender, MyJDownloaderListener {
    private CaptchaMyJDSolverConfig config;
    private CaptchaMyJDSolver       solver;
    public static final String      ID = "myjd";

    public CaptchaMyJDSolverService() {
        config = JsonConfig.create(CaptchaMyJDSolverConfig.class);
        AdvancedConfigManager.getInstance().register(config);
        if (!Application.isHeadless()) {
            SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                public void run() {
                    ServicePanel.getInstance().addExtender(CaptchaMyJDSolverService.this);
                    initServicePanel(config._getStorageHandler().getKeyHandler("Enabled"));
                    MyJDownloaderController.getInstance().getEventSender().addListener(CaptchaMyJDSolverService.this);
                    ServicePanel.getInstance().requestUpdate(true);
                }
            });
        }
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (solver.isMyJDownloaderAccountValid()) {
            services.add(new ServiceCollection<CaptchaMyJDSolver>() {
                /**
                 *
                 */
                private static final long serialVersionUID = 5569965026755271172L;

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new ServicePanelMyJDCESTooltip(owner, solver);
                }

                @Override
                public Icon getIcon() {
                    return new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, 18);
                }

                @Override
                protected long getLastActiveTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                protected String getName() {
                    return CaptchaMyJDSolverService.this.getName();
                }

                @Override
                public boolean isEnabled() {
                    return config.isEnabled();
                }
            });
        }
    }

    @Override
    public CaptchaMyJDSolverConfig getConfig() {
        return config;
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {
            // public Icon getIcon(int i) {
            // return new AbstractIcon(IconKey.ICON_myjdownloader", i);
            // }
            {
                addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, 32));
                addDescription(_GUI.T.MyJDownloaderService_createPanel_description_2());
                SettingsButton openMyJDownloader = new SettingsButton(new AppAction() {
                    {
                        setName(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_open_());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                        ConfigurationView.getInstance().setSelectedSubPanel(MyJDownloaderSettingsPanel.class);
                    }
                });
                add(openMyJDownloader, "gapleft 37,spanx,pushx,growx");
                addBlackWhiteList(config);
            }

            public String getDescription() {
                return _GUI.T.MyJDownloaderService_getDescription_tt_();
            }

            @Override
            public Icon getIcon() {
                return CaptchaMyJDSolverService.this.getIcon(32);
            }

            @Override
            public String getPanelID() {
                return "CES_" + getTitle();
            }

            @Override
            public String getTitle() {
                return "My.JDownloader";
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
    public Icon getIcon(int size) {
        return new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, size);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getName() {
        return _GUI.T.CaptchaMyJDSolver_gettypeName();
    }

    @Override
    public String getType() {
        return _GUI.T.CaptchaMyJDSolver_getName();
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        // ret.put(DialogClickCaptchaSolver.ID, 60000);
        // ret.put(DialogBasicCaptchaSolver.ID, 60000);
        // ret.put(CaptchaAPISolver.ID, 60000);
        ret.put(JacSolverService.ID, 30000);
        ret.put(NineKwSolverService.ID, 60000);
        // ret.put(CaptchaMyJDSolver.ID, 60000);
        ret.put(DeathByCaptchaSolverService.ID, 60000);
        ret.put(CheapCaptchaSolverService.ID, 60000);
        ret.put(ImageTyperzSolverService.ID, 60000);
        ret.put(EndCaptchaSolverService.ID, 60000);
        return ret;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public void onMyJDownloaderConnectionStatusChanged(MyJDownloaderConnectionStatus status, int connections) {
        if (!Application.isHeadless()) {
            ServicePanel.getInstance().requestUpdate(true);
        }
    }

    public void setSolver(CaptchaMyJDSolver captchaMyJDSolver) {
        this.solver = captchaMyJDSolver;
    }
}
