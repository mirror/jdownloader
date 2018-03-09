package org.jdownloader.plugins;

import javax.swing.Icon;

import org.appwork.swing.components.ExtMergedIcon;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public enum SkipReason {
    CONNECTION_UNAVAILABLE(_JDT.T.DownloadLink_setSkipped_statusmessage_noconnectionavailable(), IconKey.ICON_ERROR),
    TOO_MANY_RETRIES(_JDT.T.DownloadLink_setSkipped_statusmessage_toomanyretries(), IconKey.ICON_ERROR),
    CAPTCHA(_JDT.T.DownloadLink_setSkipped_statusmessage_captcha(), IconKey.ICON_OCR),
    MANUAL(_JDT.T.DownloadLink_setSkipped_statusmessage()),
    DISK_FULL(_JDT.T.DownloadLink_setSkipped_statusmessage_disk_full(), IconKey.ICON_SAVE),
    NO_ACCOUNT(_JDT.T.DownloadLink_setSkipped_statusmessage_account(), IconKey.ICON_PREMIUM),
    INVALID_DESTINATION(_JDT.T.DownloadLink_setSkipped_statusmessage_invalid_path(), IconKey.ICON_SAVETO),
    FILE_EXISTS(_JDT.T.DownloadLink_setSkipped_statusmessage_file_exists(), IconKey.ICON_COPY),
    UPDATE_RESTART_REQUIRED(_JDT.T.DownloadLink_setSkipped_statusmessage_update_restart(), IconKey.ICON_RESTART),
    FFMPEG_MISSING(_JDT.T.DownloadLink_setSkipped_statusmessage_ffmpeg(), IconKey.ICON_LOGO_FFMPEG),
    FFPROBE_MISSING(_JDT.T.DownloadLink_setSkipped_statusmessage_ffprobe(), IconKey.ICON_LOGO_FFMPEG),
    PHANTOM_JS_MISSING(_JDT.T.DownloadLink_setSkipped_statusmessage_phantom(), IconKey.ICON_LOGO_PHANTOMJS_LOGO),
    PLUGIN_DEFECT(_JDT.T.downloadlink_status_error_defect(), IconKey.ICON_FALSE);
    private final String exp;
    private final String iconKey;

    private SkipReason(String exp, String iconKey) {
        this.exp = exp;
        this.iconKey = iconKey;
    }

    public Icon getIcon(Object requestor, int size) {
        final String id = "Skipped" + iconKey + "-" + size;
        Icon ret = NewTheme.I().getCached(id);
        // if (ret != null) return ret;
        if (iconKey == null) {
            ret = new AbstractIcon(IconKey.ICON_SKIPPED, size);
            NewTheme.I().cache(ret, id);
            return ret;
        }
        int main = (int) (size * 0.75d);
        int badge = (int) (size * 0.75d);
        ret = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_SKIPPED, main), -1, -1).add(new AbstractIcon(iconKey, badge), size - badge, size - badge).crop(size, size);
        NewTheme.I().cache(ret, id);
        return ret;
    }

    private SkipReason(String exp) {
        this.exp = exp;
        this.iconKey = IconKey.ICON_SKIPPED;
    }

    public String getExplanation(Object requestor) {
        return exp;
    }
}
