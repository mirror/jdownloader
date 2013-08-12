package org.jdownloader.plugins;

import org.jdownloader.translate._JDT;

public enum SkipReason {
    NONE(""),
    CAPTCHA(_JDT._.DownloadLink_setSkipped_statusmessage_captcha()),
    MANUAL(_JDT._.DownloadLink_setSkipped_statusmessage()),
    DISK_FULL(_JDT._.DownloadLink_setSkipped_statusmessage_disk_full()),
    NO_ACCOUNT(_JDT._.DownloadLink_setSkipped_statusmessage_account()),
    INVALID_DESTINATION(_JDT._.DownloadLink_setSkipped_statusmessage_invalid_path()),
    FILE_EXISTS(_JDT._.DownloadLink_setSkipped_statusmessage_file_exists()),
    FILE_IN_PROCESS(_JDT._.DownloadLink_setSkipped_statusmessage_file_loading()),
    PLUGIN_DEFECT(_JDT._.downloadlink_status_error_defect());

    private final String exp;

    private SkipReason(String exp) {
        this.exp = exp;
    }

    public String getExplanation() {
        return exp;
    }
}
