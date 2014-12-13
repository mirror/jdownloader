package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzCaptchaSolver;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzSolverService;
import org.jdownloader.gui.translate._GUI;

public class CaptchaToogleImageTyperzAction extends AbstractToolbarToggleAction {

    public CaptchaToogleImageTyperzAction() {
        super(ImageTyperzCaptchaSolver.getInstance().getService().getConfig()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class));

        setIconKey("image_typerz");

    }

    @Override
    protected String createTooltip() {
        ImageTyperzSolverService service = ImageTyperzCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        ImageTyperzSolverService service = ImageTyperzCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        ImageTyperzSolverService service = ImageTyperzCaptchaSolver.getInstance().getService();
        ;
        return _GUI._.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
