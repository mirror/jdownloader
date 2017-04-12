package org.jdownloader.api.captcha;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzSolverService;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class CaptchaAPIManualRemoteSolverService extends AbstractSolverService {
    private CaptchaMyJDownloaderRemoteSolverSettings config;

    public CaptchaAPIManualRemoteSolverService() {
        config = JsonConfig.create(CaptchaMyJDownloaderRemoteSolverSettings.class);
        AdvancedConfigManager.getInstance().register(config);
    }

    @Override
    public String getType() {
        return _GUI.T.CaptchaAPISolver_getName();
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, size);
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {

            // public Icon getIcon(int i) {
            // return new AbstractIcon(IconKey.ICON_myjdownloader", i);
            // }

            {
                addHeader(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, 32));
                addDescription(_GUI.T.CaptchaAPIManualRemoteSolverService_getConfigPanel_description());
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

            @Override
            public Icon getIcon() {
                return CaptchaAPIManualRemoteSolverService.this.getIcon(32);
            }

            @Override
            public String getPanelID() {
                return "CES_" + getTitle();
            }

            @Override
            public String getTitle() {
                return CaptchaAPIManualRemoteSolverService.this.getName();
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
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getName() {
        return _GUI.T.CaptchaAPISolver_gettypeName();
    }

    public static final String ID = "myjdremote";

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        ret.put(JacSolverService.ID, 30000);
        ret.put(NineKwSolverService.ID, 300000);
        ret.put(CaptchaMyJDSolverService.ID, 60000);
        ret.put(DeathByCaptchaSolverService.ID, 60000);
        ret.put(ImageTyperzSolverService.ID, 60000);
        ret.put(CheapCaptchaSolverService.ID, 60000);
        ret.put(EndCaptchaSolverService.ID, 60000);

        return ret;
    }
}
