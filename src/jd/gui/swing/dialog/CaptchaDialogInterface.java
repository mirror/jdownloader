package jd.gui.swing.dialog;

import javax.swing.ImageIcon;

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

    public ImageIcon getImage();

    public String getDefaultValue();

    public String getHelpText();

}
