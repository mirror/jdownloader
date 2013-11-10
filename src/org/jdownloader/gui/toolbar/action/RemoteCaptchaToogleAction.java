package org.jdownloader.gui.toolbar.action;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class RemoteCaptchaToogleAction extends AbstractToolbarToggleAction {

    public RemoteCaptchaToogleAction() {
        super(CFG_CAPTCHA.REMOTE_CAPTCHA_ENABLED);
        setIconKey("remoteOCR");

    }

    @Override
    protected String createTooltip() {
        return _GUI._.RemoteCaptchaToogleAction_createTooltip_();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.RemoteCaptchaToogleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.RemoteCaptchaToogleAction_getNameWhenEnabled_();
    }

}
