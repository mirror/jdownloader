package org.jdownloader.captcha.v2.solver.jac;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.solver.service.AbstractSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class JacSolverService extends AbstractSolverService implements SolverService {
    private JACSolverConfig config;

    public JacSolverService() {
        config = JsonConfig.create(JACSolverConfig.class);
        AdvancedConfigManager.getInstance().register(config);
    }

    public static final String ID = "jac";

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_OCR, size);
    }

    @Override
    public String getType() {
        return _GUI._.JACSolver_getName_();
    }

    @Override
    public String getID() {
        return ID;
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
        return _GUI._.JACSolver_gettypeName_();
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        // ret.put(Captcha9kwSolverClick.ID, 60000);
        // ret.put(DialogClickCaptchaSolver.ID, 60000);
        // ret.put(DialogBasicCaptchaSolver.ID, 60000);
        // ret.put(CaptchaAPISolver.ID, 60000);
        // ret.put(JACSolver.ID, 30000);
        // ret.put(Captcha9kwSolver.ID, 60000);
        // ret.put(CaptchaMyJDSolver.ID, 60000);
        // ret.put(CBSolver.ID, 60000);
        // ret.put(DeathByCaptchaSolver.ID, 60000);

        return ret;
    }

}
