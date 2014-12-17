package org.jdownloader.plugins;

import javax.swing.Icon;

import jd.plugins.DownloadLink;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public enum FinalLinkState {
    FINISHED(_GUI._.TaskColumn_getStringValue_finished_(), IconKey.ICON_TRUE),
    FINISHED_MIRROR(_GUI._.TaskColumn_getStringValue_finished_mirror(), IconKey.ICON_TRUE_ORANGE),
    FINISHED_MD5(_JDT._.system_download_doCRC2_success("MD5"), IconKey.ICON_OK),
    FINISHED_SHA1(_JDT._.system_download_doCRC2_success("SHA1"), IconKey.ICON_OK),
    FINISHED_CRC32(_JDT._.system_download_doCRC2_success("CRC32"), IconKey.ICON_OK),

    FAILED(_JDT._.downloadlink_status_error_downloadfailed(), IconKey.ICON_FALSE),
    FAILED_MD5(_JDT._.system_download_doCRC2_failed("MD5"), IconKey.ICON_FALSE),
    FAILED_SHA1(_JDT._.system_download_doCRC2_failed("SHA1"), IconKey.ICON_FALSE),
    FAILED_CRC32(_JDT._.system_download_doCRC2_failed("CRC32"), IconKey.ICON_FALSE),
    FAILED_EXISTS(_JDT._.downloadlink_status_error_file_exists(), IconKey.ICON_FALSE),

    OFFLINE(_JDT._.downloadlink_status_error_file_not_found(), IconKey.ICON_FALSE),

    FAILED_FATAL(_JDT._.downloadlink_status_error_fatal(), IconKey.ICON_FALSE),

    PLUGIN_DEFECT(_JDT._.downloadlink_status_error_defect(), IconKey.ICON_FALSE);

    private final String exp;
    private String       iconKey;
    private AbstractIcon icon16;

    public String getIconKey() {
        return iconKey;
    }

    private FinalLinkState(String exp, String iconKey) {
        this.exp = exp;
        this.iconKey = iconKey;
        icon16 = new AbstractIcon(iconKey, 16);
    }

    public Icon getIcon(int size) {
        switch (size) {
        case 16:
            return icon16;
        }
        return new AbstractIcon(iconKey, 16);
    }

    public String getExplanation(Object requestor, DownloadLink link) {
        if (this == FAILED_FATAL && link != null) {
            String ret = link.getStringProperty(DownloadLink.PROPERTY_CUSTOM_MESSAGE, null);
            if (ret != null) {
                return ret;
            }
        }
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
        default:
            return false;
        }

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
