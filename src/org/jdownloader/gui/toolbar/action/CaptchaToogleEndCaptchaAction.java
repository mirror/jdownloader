package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolver;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolverService;
import org.jdownloader.gui.translate._GUI;

public class CaptchaToogleEndCaptchaAction extends AbstractToolbarToggleAction {

    public CaptchaToogleEndCaptchaAction() {
        super(EndCaptchaSolver.getInstance().getService().getConfig()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class));

        setIconKey("endCaptcha");

    }

    @Override
    protected String createTooltip() {
        EndCaptchaSolverService service = EndCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        EndCaptchaSolverService service = EndCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        EndCaptchaSolverService service = EndCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
