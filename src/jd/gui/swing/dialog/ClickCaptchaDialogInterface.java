package jd.gui.swing.dialog;

import org.appwork.utils.swing.dialog.UserIODefinition;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;

public interface ClickCaptchaDialogInterface extends UserIODefinition {

    ClickedPoint getResult();

}
