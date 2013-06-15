package org.jdownloader.gui.toolbar.action;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class CaptchaExchangeToogleAction extends AbstractToolbarToggleAction {

    public CaptchaExchangeToogleAction(SelectionInfo<?, ?> selection) {
        super(CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED);
        setIconKey("ces");

    }

    @Override
    protected String createTooltip() {
        return _GUI._.CaptchaExchangeToogleAction_createTooltip_();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.CaptchaExchangeToogleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.CaptchaExchangeToogleAction_getNameWhenEnabled_();
    }

}
