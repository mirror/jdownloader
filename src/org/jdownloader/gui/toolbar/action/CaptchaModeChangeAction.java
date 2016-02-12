package org.jdownloader.gui.toolbar.action;

import java.awt.event.ActionEvent;

import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class CaptchaModeChangeAction extends AbstractToolBarAction implements GenericConfigEventListener<Enum> {

    private final EnumKeyHandler keyHandler;

    public CaptchaModeChangeAction() {
        setIconKey(IconKey.ICON_DIALOGOCR);
        this.keyHandler = CFG_CAPTCHA.CAPTCHA_MODE;
        keyHandler.getEventSender().addListener(this, true);
        setSelected((CaptchaSettings.MODE) keyHandler.getValue());
    }

    public void setSelected(final CaptchaSettings.MODE mode) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                final CaptchaSettings.MODE finalMode;
                if (mode == null) {
                    finalMode = CaptchaSettings.MODE.NORMAL;
                } else {
                    finalMode = mode;
                }
                keyHandler.setValue(finalMode);
                switch (finalMode) {
                case NORMAL:
                    setSelected(true);
                    break;
                case SKIP_ALL:
                    setSelected(false);
                    break;
                }
                setName(finalMode.name());
            }
        };
    }

    public void actionPerformed(ActionEvent e) {
        final boolean sel = isSelected();
        if (sel) {
            setSelected(CaptchaSettings.MODE.NORMAL);
        } else {
            setSelected(CaptchaSettings.MODE.SKIP_ALL);
        }
    }

    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
        setSelected((CaptchaSettings.MODE) newValue);
    }

    @Override
    protected String createTooltip() {
        return _GUI.T.CaptchaMode_createTooltip_();
    }

}
