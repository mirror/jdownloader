package jd.gui.swing.dialog;

import java.awt.Image;

import org.appwork.utils.swing.dialog.UserIODefinition;
import org.jdownloader.DomainInfo;

public interface CaptchaDialogInterface extends UserIODefinition {
    public static enum DialogType {
        CRAWLER,
        HOSTER,
        OTHER;
    }

    public String getFilename();

    public String getCrawlerStatus();

    public long getFilesize();

    public String getCaptchaCode();

    public DomainInfo getDomainInfo();

    public DialogType getType();

    public Image[] getImages();

    public String getDefaultValue();

    public String getHelpText();

}
