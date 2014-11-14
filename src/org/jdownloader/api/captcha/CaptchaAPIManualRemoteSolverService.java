package org.jdownloader.api.captcha;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.captchabrotherhood.CBSolverService;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.captcha.v2.solver.service.DialogSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class CaptchaAPIManualRemoteSolverService extends AbstractSolverService {
    private CaptchaMyJDownloaderRemoteSolverConfig config;

    public CaptchaAPIManualRemoteSolverService() {
        config = JsonConfig.create(CaptchaMyJDownloaderRemoteSolverConfig.class);
        AdvancedConfigManager.getInstance().register(config);
    }

    @Override
    public String getType() {
        return _GUI._.CaptchaAPISolver_getName();
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_MYJDOWNLOADER, size);
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        return null;
    }

    @Override
    public boolean hasConfigPanel() {
        return false;
    }

    @Override
    public String getName() {
        return _GUI._.CaptchaAPISolver_gettypeName();
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
        ret.put(DialogSolverService.ID, 60000);
        // ret.put(DialogClickCaptchaSolver.ID, 0);
        // ret.put(DialogBasicCaptchaSolver.ID, 0);
        // ret.put(CaptchaAPISolver.ID, 0);
        ret.put(JacSolverService.ID, 30000);
        ret.put(NineKwSolverService.ID, 120000);
        ret.put(CaptchaMyJDSolverService.ID, 60000);
        ret.put(CBSolverService.ID, 120000);
        ret.put(DeathByCaptchaSolverService.ID, 60000);

        return ret;
    }
}
