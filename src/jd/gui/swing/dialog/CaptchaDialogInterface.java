package jd.gui.swing.dialog;

import java.awt.Image;

import org.appwork.uio.UserIODefinition;
import org.jdownloader.DomainInfo;

public interface CaptchaDialogInterface extends UserIODefinition {

    public String getFilename();

    public String getCrawlerStatus();

    public long getFilesize();

    public String getResult();

    public DomainInfo getDomainInfo();

    public DialogType getType();

    public Image[] getImages();

    public String getHelpText();

    public void dispose();

    public void suggest(String value);

    public boolean isHideCaptchasForHost();

    public boolean isHideCaptchasForPackage();

    public boolean isStopDownloads();

    public boolean isHideAllCaptchas();

    public boolean isStopCrawling();

    public boolean isStopShowingCrawlerCaptchas();

    public boolean isRefresh();

}
