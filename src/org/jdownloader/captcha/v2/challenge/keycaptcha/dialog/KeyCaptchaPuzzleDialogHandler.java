package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import jd.gui.swing.dialog.DialogType;

import org.jdownloader.captcha.v2.AbstractDialogHandler;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleResponseData;

public class KeyCaptchaPuzzleDialogHandler extends AbstractDialogHandler<KeyCaptchaPuzzleDialog, KeyCaptchaPuzzleChallenge, KeyCaptchaPuzzleResponseData> {

    public KeyCaptchaPuzzleDialogHandler(KeyCaptchaPuzzleChallenge captchaChallenge) {
        super(captchaChallenge.getDomainInfo(), captchaChallenge);

    }

    @Override
    protected KeyCaptchaPuzzleDialog createDialog(DialogType dialogType, int flag, final Runnable onDispose) {
        return new KeyCaptchaPuzzleDialog(flag, dialogType, getHost(), captchaChallenge) {
            public void dispose() {

                super.dispose();
                onDispose.run();
            }
        };
    }

}