package org.jdownloader.gui.toolbar.action;

import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;

public class CaptchaToogle9KWAction extends AbstractToolbarToggleAction {

    public CaptchaToogle9KWAction() {
        super(CFG_9KWCAPTCHA.ENABLED_GLOBALLY);

        setIconKey("9kw");

    }

    @Override
    protected String createTooltip() {
        SolverService service = NineKwSolverService.getInstance();
        return _GUI._.createTooltip_Captcha_Service_toggle(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenDisabled() {
        SolverService service = NineKwSolverService.getInstance();
        return _GUI._.createTooltip_Captcha_Service_getNameWhenDisabled_(service.getName(), service.getType());
    }

    @Override
    protected String getNameWhenEnabled() {
        SolverService service = NineKwSolverService.getInstance();
        return _GUI._.createTooltip_Captcha_Service_getNameWhenEnabled_(service.getName(), service.getType());
    }

}
