package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import org.jdownloader.captcha.v2.AbstractDialogHandler;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaCategoryChallenge;

import jd.gui.swing.dialog.DialogType;

public class KeyCaptchaCategoryDialogHandler extends AbstractDialogHandler<KeyCaptchaCategoryDialog, KeyCaptchaCategoryChallenge, String> {

    public KeyCaptchaCategoryDialogHandler(KeyCaptchaCategoryChallenge captchaChallenge) {
        super(captchaChallenge.getDomainInfo(), captchaChallenge);

    }

    @Override
    protected KeyCaptchaCategoryDialog createDialog(DialogType dialogType, int flag, final Runnable onDispose) {
        return new KeyCaptchaCategoryDialog(captchaChallenge, flag, dialogType, getHost(), captchaChallenge) {
            public void dispose() {

                super.dispose();
                onDispose.run();
            }
        };
    }

}