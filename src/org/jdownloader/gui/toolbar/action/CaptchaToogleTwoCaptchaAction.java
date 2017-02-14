package org.jdownloader.gui.toolbar.action;

import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.solver.twocaptcha.TwoCaptchaSolver;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_TWO_CAPTCHA;

public class CaptchaToogleTwoCaptchaAction extends AbstractToolbarToggleAction {

    public CaptchaToogleTwoCaptchaAction() {
        super(CFG_TWO_CAPTCHA.ENABLED);
        setIconKey(IconKey.ICON_LOGO_2CAPTCHA);
    }

    @Override
    protected String createTooltip() {
        SolverService service = TwoCaptchaSolver.getInstance().getService();
        return _GUI.T.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        SolverService service = TwoCaptchaSolver.getInstance().getService();
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        SolverService service = TwoCaptchaSolver.getInstance().getService();
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
