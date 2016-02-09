package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class CaptchaToogleBrowserSolverAction extends AbstractToolbarToggleAction {

    public CaptchaToogleBrowserSolverAction() {
        super(BrowserSolverService.getInstance().getConfig()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class));

        setIconKey(IconKey.ICON_OCR);

    }

    @Override
    protected String createTooltip() {
        BrowserSolverService service = BrowserSolverService.getInstance();
        return _GUI.T.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        BrowserSolverService service = BrowserSolverService.getInstance();
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        BrowserSolverService service = BrowserSolverService.getInstance();
        return _GUI.T.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
