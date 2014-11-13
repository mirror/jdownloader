package org.jdownloader.gui.toolbar.action;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_JAC_CAPTCHA_SOLVER;

public class JAntiCaptchaToogleAction extends AbstractToolbarToggleAction {

    public JAntiCaptchaToogleAction() {
        super(CFG_JAC_CAPTCHA_SOLVER.ENABLED);
        setIconKey("ocr");

    }

    @Override
    protected String createTooltip() {
        return _GUI._.JAntiCaptchaToogleAction_createTooltip_();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.JAntiCaptchaToogleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.JAntiCaptchaToogleAction_getNameWhenEnabled_();
    }

}
