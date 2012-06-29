package jd.gui.swing.dialog;

import java.awt.Image;

import jd.controlling.captcha.CaptchaResult;

import org.appwork.utils.swing.dialog.UserIODefinition;
import org.jdownloader.DomainInfo;

public interface CaptchaDialogInterface extends UserIODefinition {
    public static enum DialogType {
        CRAWLER,
        HOSTER,
        OTHER;
    }

    public static enum CaptchaType {
        TEXT,
        CLICK
    }

    public String getFilename();

    public String getCrawlerStatus();

    public long getFilesize();

    public CaptchaResult getCaptchaResult();

    public DomainInfo getDomainInfo();

    public DialogType getType();

    public CaptchaType getCaptchaType();

    public Image[] getImages();

    public CaptchaResult getDefaultValue();

    public String getHelpText();

    public void dispose();

}
