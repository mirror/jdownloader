package org.jdownloader.gui.toolbar.action;

import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.solver.antiCaptchaCom.AntiCaptchaComSolver;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_ANTICAPTCHA_COM;

public class CaptchaToogleAntiCaptchaAction extends AbstractToolbarToggleAction {

    public CaptchaToogleAntiCaptchaAction() {
        super(CFG_ANTICAPTCHA_COM.ENABLED);
        setIconKey(IconKey.ICON_LOGO_ANTICAPTCHA);
    }

    @Override
    protected String createTooltip() {
        final SolverService service = AntiCaptchaComSolver.getInstance().getService();
        return _GUI.T.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        final SolverService service = AntiCaptchaComSolver.getInstance().getService();
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        final SolverService service = AntiCaptchaComSolver.getInstance().getService();
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
