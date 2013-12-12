package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialog.LoginData;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotValidatedException;
import org.jdownloader.myjdownloader.client.exceptions.ExceptionResponse;
import org.jdownloader.myjdownloader.client.json.CaptchaChallenge;

public class RegisterTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        final LoginDialog login = new LoginDialog(0);
        login.setMessage("MyJDownloader Account Register");
        login.setRememberDefault(true);
        login.setUsernameDefault(config.get("email", ""));
        login.setPasswordDefault(config.get("password", ""));

        final LoginData li = Dialog.getInstance().showDialog(login);
        if (li.isSave()) {
            config.put("email", li.getUsername());

            config.put("password", li.getPassword());
        }
        try {
            final CaptchaChallenge challenge = api.getChallenge();
            try {
                final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
                challenge.setCaptchaResponse(response);

                api.requestRegistrationEmail(challenge, li.getUsername(), null);
            } catch (final ExceptionResponse e) {
                e.printStackTrace();

            }
            api.finishRegistration(Dialog.getInstance().showInputDialog(0, "Email Confirmal Key", "Enter", null, null, null, null), li.getUsername(), li.getPassword());

        } catch (final EmailNotValidatedException e) {
            final CaptchaChallenge challenge = api.getChallenge();
            try {
                final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
                challenge.setCaptchaResponse(response);

                api.requestRegistrationEmail(challenge, li.getUsername(), null);
            } catch (final ExceptionResponse e1) {
                e1.printStackTrace();

            }
            api.finishRegistration(Dialog.getInstance().showInputDialog(0, "Email Confirmal Key", "Enter", null, null, null, null), li.getUsername(), li.getPassword());
            api.connect(li.getUsername(), li.getPassword());

        }

    }

}
