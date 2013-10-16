package org.jdownloader.plugins;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public enum FinalLinkState {
    FINISHED(_GUI._.TaskColumn_getStringValue_finished_()),
    FINISHED_MIRROR(_GUI._.TaskColumn_getStringValue_finished_()),
    FINISHED_MD5(_JDT._.system_download_doCRC2_success("MD5")),
    FINISHED_SHA1(_JDT._.system_download_doCRC2_success("SHA1")),
    FINISHED_CRC32(_JDT._.system_download_doCRC2_success("CRC32")),

    FAILED(_JDT._.downloadlink_status_error_downloadfailed()),
    FAILED_MD5(_JDT._.system_download_doCRC2_failed("MD5")),
    FAILED_SHA1(_JDT._.system_download_doCRC2_failed("SHA1")),
    FAILED_CRC32(_JDT._.system_download_doCRC2_failed("CRC32")),
    FAILED_EXISTS(_JDT._.downloadlink_status_error_file_exists()),

    OFFLINE(_JDT._.downloadlink_status_error_file_not_found()),

    FAILED_FATAL(_JDT._.downloadlink_status_error_fatal()),

    PLUGIN_DEFECT(_JDT._.downloadlink_status_error_defect());

    private final String exp;

    private FinalLinkState(String exp) {
        this.exp = exp;
    }

    public String getExplanation() {
        return exp;
    }

    public boolean isFinished() {
        switch (this) {
        case FINISHED:
        case FINISHED_MIRROR:
        case FINISHED_CRC32:
        case FINISHED_MD5:
        case FINISHED_SHA1:
            return true;
        }
        return false;
    }

    public boolean isFailed() {
        return !isFinished();
    }

    public static boolean CheckFinished(FinalLinkState state) {
        return state != null && state.isFinished();
    }

    public static boolean CheckFailed(FinalLinkState state) {
        return state != null && state.isFailed();
    }
}
