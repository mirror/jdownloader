package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.json.CaptchaChallenge;

public class ChallengeTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {

        final CaptchaChallenge challenge = api.getChallenge();
        final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
        challenge.setCaptchaResponse(response);

    }

}
