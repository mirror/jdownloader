package org.jdownloader.plugins;

import jd.plugins.DownloadLink;

import org.jdownloader.translate._JDT;

public enum SkipReason {
    TOO_MANY_RETRIES(_JDT._.DownloadLink_setSkipped_statusmessage_toomanyretries()),
    CAPTCHA(_JDT._.DownloadLink_setSkipped_statusmessage_captcha()),
    MANUAL(_JDT._.DownloadLink_setSkipped_statusmessage()),
    DISK_FULL(_JDT._.DownloadLink_setSkipped_statusmessage_disk_full()),
    NO_ACCOUNT(_JDT._.DownloadLink_setSkipped_statusmessage_account()),
    INVALID_DESTINATION(_JDT._.DownloadLink_setSkipped_statusmessage_invalid_path()),
    FILE_EXISTS(_JDT._.DownloadLink_setSkipped_statusmessage_file_exists());

    private final String exp;

    private SkipReason(String exp) {
        this.exp = exp;
    }

    public String getExplanation(Object requestor, DownloadLink link) {
        return exp;
    }
}
