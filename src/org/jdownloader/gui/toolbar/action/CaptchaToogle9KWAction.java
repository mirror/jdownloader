package org.jdownloader.gui.toolbar.action;

import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;

public class CaptchaToogle9KWAction extends AbstractToolbarToggleAction {

    private NineKwSolverService service;

    public CaptchaToogle9KWAction() {
        super(CFG_9KWCAPTCHA.ENABLED_GLOBALLY);
        service = NineKwSolverService.getInstance();
        setIconKey("9kw");

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
