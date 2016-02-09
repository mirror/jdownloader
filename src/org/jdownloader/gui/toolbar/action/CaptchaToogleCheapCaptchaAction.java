package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolver;
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class CaptchaToogleCheapCaptchaAction extends AbstractToolbarToggleAction {

    public CaptchaToogleCheapCaptchaAction() {
        super(CheapCaptchaSolver.getInstance().getService().getConfig()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class));

        setIconKey(IconKey.ICON_LOGO_CHEAPCAPTCHA);

    }

    @Override
    protected String createTooltip() {
        CheapCaptchaSolverService service = CheapCaptchaSolver.getInstance().getService();
        ;
        return _GUI.T.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        CheapCaptchaSolverService service = CheapCaptchaSolver.getInstance().getService();
        ;
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        CheapCaptchaSolverService service = CheapCaptchaSolver.getInstance().getService();
        ;
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
