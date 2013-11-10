package org.jdownloader.gui.toolbar.action;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class CaptchaDialogsToogleAction extends AbstractToolbarToggleAction {

    public CaptchaDialogsToogleAction() {
        super(CFG_CAPTCHA.CAPTCHA_DIALOGS_ENABLED);
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
