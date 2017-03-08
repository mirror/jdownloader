package org.jdownloader.captcha.v2.solver.antiCaptchaCom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.Application;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_ANTICAPTCHA_COM;

import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanelExtender;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

public class AntiCaptchaComSolverService extends AbstractSolverService implements ServicePanelExtender {
    private AntiCaptchaComConfigInterface config;
    private AntiCaptchaComSolver          solver;

    public AntiCaptchaComSolverService() {
        config = JsonConfig.create(AntiCaptchaComConfigInterface.class);
        AdvancedConfigManager.getInstance().register(config);
        if (!Application.isHeadless()) {
            ServicePanel.getInstance().addExtender(this);
            initServicePanel(CFG_ANTICAPTCHA_COM.API_KEY, CFG_ANTICAPTCHA_COM.ENABLED);
        }
    }

    @Override
    public String getType() {
        return _GUI.T.AntiCaptchaCom_getName_();
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon(IconKey.ICON_LOGO_ANTICAPTCHA, size);
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        return new AntiCaptchaComConfigPanel(this);
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getName() {
        return "anti-captcha.com";
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (solver.validateLogins()) {
            services.add(new ServiceCollection<AntiCaptchaComSolver>() {
                private static final long serialVersionUID = -2069081821971909269L;

                @Override
                public Icon getIcon() {
                    return AntiCaptchaComSolverService.this.getIcon(18);
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
                    return "anti-captcha.com";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new AntiCaptchaComTooltip(owner, solver);
                }
            });
        }
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        ret.put(JacSolverService.ID, 30000);
        ret.put(NineKwSolverService.ID, 120000);
        // ret.put(CaptchaMyJDSolverService.ID, 60000);
        // ret.put(DeathByCaptchaSolverService.ID, 60000);
        // ret.put(ImageTyperzSolverService.ID, 60000);
        // ret.put(CheapCaptchaSolverService.ID, 60000);
        // ret.put(EndCaptchaSolverService.ID, 60000);
        return ret;
    }

    public static final String ID = "AntiCaptcha";

    @Override
    public String getID() {
        return ID;
    }

    public void setSolver(AntiCaptchaComSolver solver) {
        this.solver = solver;
    }
}
