package org.jdownloader.gui.toolbar.action;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.solver.gui.DialogCaptchaSolverConfig;
import org.jdownloader.gui.translate._GUI;

public class CaptchaDialogsToogleAction extends AbstractToolbarToggleAction {

    public CaptchaDialogsToogleAction() {
        super(JsonConfig.create(DialogCaptchaSolverConfig.class)._getStorageHandler().getKeyHandler("Enabled", BooleanKeyHandler.class));
        setIconKey("dialogOCR");

    }

    @Override
    protected String createTooltip() {
        return _GUI._.CaptchaDialogsToogleAction_createTooltip_();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.CaptchaDialogsToogleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.CaptchaDialogsToogleAction_getNameWhenEnabled_();
    }
}
