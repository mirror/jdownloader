package org.jdownloader.captcha.v2.solver.service;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.captchabrotherhood.CBSolver;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogCaptchaSolverConfig;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DialogSolverService extends AbstractSolverService {
    public static final String               ID       = "dialog";
    private static final DialogSolverService INSTANCE = new DialogSolverService();

    public static DialogSolverService getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return _GUI._.DialogBasicCaptchaSolver_getName();
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_OCR, size);
    }

    @Override
    public String getName() {
        return _GUI._.DialogBasicCaptchaSolver_gettypeName();
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
    public ChallengeSolverConfig getConfig() {
        return JsonConfig.create(DialogCaptchaSolverConfig.class);
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();

        // ret.put(DialogClickCaptchaSolver.ID, 0);
        // ret.put(DialogBasicCaptchaSolver.ID, 0);
        // ret.put(CaptchaAPISolver.ID, 0);
        ret.put(JACSolver.ID, 30000);
        ret.put(NineKwSolverService.ID, 120000);
        ret.put(CaptchaMyJDSolver.ID, 60000);
        ret.put(CBSolver.ID, 120000);
        ret.put(DeathByCaptchaSolver.ID, 60000);

        return ret;
    }

    @Override
    public String getID() {
        return ID;
    }

}
