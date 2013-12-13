package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.json.CaptchaChallenge;

public class ChangePasswordTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final String email = Dialog.getInstance().showInputDialog(0, "Enter Email", "Enter", null, null, null, null);
        final CaptchaChallenge challenge = api.getChallenge();
        final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
        challenge.setCaptchaResponse(response);
        api.requestPasswordResetEmail(challenge, email);

        api.finishPasswordReset(email, Dialog.getInstance().showInputDialog(0, "Confirmal Key", "Enter", null, null, null, null), Dialog.getInstance().showInputDialog(0, "New Password", "Enter", null, null, null, null));
    }

}
