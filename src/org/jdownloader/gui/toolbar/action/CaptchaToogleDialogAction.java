package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.solver.service.DialogSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class CaptchaToogleDialogAction extends AbstractToolbarToggleAction {

    private SolverService service;

    public CaptchaToogleDialogAction() {
        super(DialogSolverService.getInstance().getConfig()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class));
        service = DialogSolverService.getInstance();
        setIconKey(IconKey.ICON_OCR);

    }

    @Override
    protected String createTooltip() {
        return _GUI._.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
