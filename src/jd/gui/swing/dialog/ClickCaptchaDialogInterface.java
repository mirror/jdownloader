package jd.gui.swing.dialog;

import org.appwork.uio.UserIODefinition;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;

public interface ClickCaptchaDialogInterface extends UserIODefinition {

    ClickedPoint getResult();

    public boolean isHideCaptchasForHost();

    public boolean isHideCaptchasForPackage();

    public boolean isStopDownloads();

    public boolean isHideAllCaptchas();

    public boolean isStopCrawling();

    public boolean isStopShowingCrawlerCaptchas();

    public boolean isRefresh();

}
