package org.jdownloader.plugins;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackageView;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.gui.views.downloads.columns.TaskColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class WaitingSkipReason implements ConditionalSkipReason, TimeOutCondition, ValidatableConditionalSkipReason {

    public static enum CAUSE {
        IP_BLOCKED(_JDT._.downloadlink_status_error_download_limit()),
        FILE_TEMP_UNAVAILABLE(_JDT._.downloadlink_status_error_temp_unavailable()),
        CONNECTION_TEMP_UNAVAILABLE(_JDT._.download_error_message_networkreset()),
        HOST_TEMP_UNAVAILABLE(_JDT._.downloadlink_status_error_hoster_temp_unavailable()),
        RETRY_IN(null);

        private final String exp;

        private CAUSE(String exp) {
            this.exp = exp;
        }

        public String getExplanation() {
            return exp;
        }
    }

    private final CAUSE cause;

    public CAUSE getCause() {
        return cause;
    }

    private final long      timeOutTimeStamp;
    private final String    message;
    private final ImageIcon icon;
    private boolean         valid = true;

    public long getTimeOutTimeStamp() {
        return timeOutTimeStamp;
    }

    public WaitingSkipReason(CAUSE cause, long timeOut, String message) {
        this.cause = cause;
        this.timeOutTimeStamp = System.currentTimeMillis() + timeOut;
        this.message = message;
        switch (cause) {
        case FILE_TEMP_UNAVAILABLE:
            icon = NewTheme.I().getIcon("warning_green", 16);
            break;
        case HOST_TEMP_UNAVAILABLE:
            icon = NewTheme.I().getIcon("warning_red", 16);
            break;
        case IP_BLOCKED:
            icon = NewTheme.I().getIcon(IconKey.ICON_AUTO_RECONNECT, 16);
            break;
        default:
            icon = NewTheme.I().getIcon("wait", 16);
            break;
        }
    }

    public WaitingSkipReason(CAUSE cause, long timeOut) {
        this(cause, timeOut, null);
    }

    public String getMessage() {
        if (message == null) {
            if (cause == CAUSE.RETRY_IN) {
                long left = getTimeOutLeft();
                if (left > 0) {
                    return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(left / 1000));
                } else {
                    return _JDT._.gui_download_waittime_status2("");
                }
            }
            return cause.getExplanation();
        }
        return message;
    }

    public long getTimeOutLeft() {
        return Math.max(0, timeOutTimeStamp - System.currentTimeMillis());
    }

    @Override
    public boolean isConditionReached() {
        return System.currentTimeMillis() > timeOutTimeStamp;
    }

    @Override
    public void finalize(DownloadLink link) {
    }

    @Override
    public String getMessage(Object requestor, AbstractNode node) {
        long left = getTimeOutLeft();
        if (left > 0) {
            if (requestor instanceof TaskColumn) { return getMessage(); }
            if (requestor instanceof FilePackageView) { return getMessage(); }
            if (requestor instanceof ETAColumn) { return Formatter.formatSeconds(left / 1000); }
        }
        return null;
    }

    @Override
    public ImageIcon getIcon(Object requestor, AbstractNode node) {
        return icon;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void invalidate() {
        valid = false;
    }

}
