package org.jdownloader.captcha.v2.solver.service;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.gui.DialogCaptchaSolverConfig;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzSolverService;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.captcha.v2.solver.twocaptcha.TwoCaptchaSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DialogSolverService extends AbstractSolverService {
    public static final String               ID       = "dialog";
    private static final DialogSolverService INSTANCE = new DialogSolverService();
    private static DialogCaptchaSolverConfig config;

    public static DialogSolverService getInstance() {
        config = JsonConfig.create(DialogCaptchaSolverConfig.class);
        return INSTANCE;
    }

    @Override
    public String getType() {
        return _GUI.T.DialogBasicCaptchaSolver_getName();
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_OCR, size);
    }

    @Override
    public String getName() {
        return _GUI.T.DialogBasicCaptchaSolver_gettypeName();
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {

            {
                addHeader(getTitle(), DialogSolverService.this.getIcon(32));
                addDescription(DialogSolverService.this.getType());

                addBlackWhiteList(config);

            }

            @Override
            public Icon getIcon() {
                return DialogSolverService.this.getIcon(32);
            }

            @Override
            public String getPanelID() {
                return "JAC_" + getTitle();
            }

            @Override
            public String getTitle() {
                return DialogSolverService.this.getName();
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
    public ChallengeSolverConfig getConfig() {
        return JsonConfig.create(DialogCaptchaSolverConfig.class);
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();

        // ret.put(DialogClickCaptchaSolver.ID, 0);
        // ret.put(DialogBasicCaptchaSolver.ID, 0);
        // ret.put(CaptchaAPISolver.ID, 0);
        ret.put(JacSolverService.ID, 30000);
        ret.put(NineKwSolverService.ID, 300000);
        ret.put(CaptchaMyJDSolverService.ID, 60000);
        ret.put(DeathByCaptchaSolverService.ID, 60000);
        ret.put(ImageTyperzSolverService.ID, 60000);
        ret.put(CheapCaptchaSolverService.ID, 60000);
        ret.put(EndCaptchaSolverService.ID, 60000);
        ret.put(TwoCaptchaSolverService.ID, 60000);

        return ret;
    }

    @Override
    public String getID() {
        return ID;
    }

}
