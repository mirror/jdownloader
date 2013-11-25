package org.jdownloader.plugins;

import javax.swing.Icon;

import org.appwork.swing.components.ExtMergedIcon;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public enum SkipReason {
    TOO_MANY_RETRIES(_JDT._.DownloadLink_setSkipped_statusmessage_toomanyretries(), IconKey.ICON_ERROR),
    CAPTCHA(_JDT._.DownloadLink_setSkipped_statusmessage_captcha(), IconKey.ICON_OCR),
    MANUAL(_JDT._.DownloadLink_setSkipped_statusmessage()),
    DISK_FULL(_JDT._.DownloadLink_setSkipped_statusmessage_disk_full(), IconKey.ICON_SAVE),
    NO_ACCOUNT(_JDT._.DownloadLink_setSkipped_statusmessage_account(), IconKey.ICON_PREMIUM),
    INVALID_DESTINATION(_JDT._.DownloadLink_setSkipped_statusmessage_invalid_path(), IconKey.ICON_SAVETO),
    FILE_EXISTS(_JDT._.DownloadLink_setSkipped_statusmessage_file_exists(), IconKey.ICON_COPY);

    private final String exp;
    private String       iconKey;

    private SkipReason(String exp, String iconKey) {
        this.exp = exp;
        this.iconKey = iconKey;
    }

    public Icon getIcon(Object requestor, int size) {
        String id;
        Icon ret = NewTheme.I().getCached(id = "Skipped" + iconKey + "-" + size);
        // if (ret != null) return ret;

        if (iconKey == null) return new AbstractIcon(IconKey.ICON_SKIPPED, size);
        int main = (int) (size * 0.75d);
        int badge = (int) (size * 0.75d);
        ret = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_SKIPPED, main), -1, -1).add(new AbstractIcon(iconKey, badge), size - badge, size - badge).crop(size, size);
        NewTheme.I().cache(ret, id);
        return ret;
    }

    private SkipReason(String exp) {
        this.exp = exp;
    }

    public String getExplanation(Object requestor) {
        return exp;
    }
}
