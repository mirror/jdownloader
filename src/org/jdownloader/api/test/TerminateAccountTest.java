package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialog.LoginData;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.json.CaptchaChallenge;

public class TerminateAccountTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        CaptchaChallenge challenge = api.getChallenge();
        String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
        challenge.setCaptchaResponse(response);
        api.requestTerminationEmail(challenge);
        challenge = api.getChallenge();
        response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
        challenge.setCaptchaResponse(response);
        final LoginDialog login = new LoginDialog(0);
        login.setMessage("MyJDownloader Account Logins");
        login.setRememberDefault(true);
        login.setUsernameDefault(config.get("email", ""));
        login.setPasswordDefault(config.get("password", ""));
        final LoginData li = Dialog.getInstance().showDialog(login);
        if (li.isSave()) {
            config.put("email", li.getUsername());
            config.put("password", li.getPassword());
        }
        response = Dialog.getInstance().showInputDialog(0, "Email Confirmal Key", "Enter", null, null, null, null);
        api.finishTermination(response, li.getUsername(), li.getPassword(), challenge);

    }

}
