package jd.plugins;

import org.jdownloader.translate._JDT;

public class DecrypterRetryException extends Exception {
    public static enum RetryReason {
        CAPTCHA(_JDT.T.decrypter_wrongcaptcha()),
        EMPTY_FOLDER(_JDT.T.decrypter_empty_folder()),
        NO_ACCOUNT(_JDT.T.decrypter_invalidaccount()),
        FILE_NOT_FOUND(_JDT.T.decrypter_contentoffline()),
        PLUGIN_DEFECT(_JDT.T.decrypter_plugindefect()),
        PLUGIN_SETTINGS(_JDT.T.decrypter_pluginsettings()),
        PASSWORD(_JDT.T.decrypter_wrongpassword()),
        HOST(_JDT.T.plugins_errors_hosterproblem()),
        HOST_RATE_LIMIT(_JDT.T.plugins_errors_hosterproblem_rate_limit()),
        GEO(_JDT.T.decrypter_unavailable_geo()),
        IP(_JDT.T.decrypter_unavailable_ip());

        private final String exp;

        private RetryReason(String exp) {
            this.exp = exp;
        }

        public String getExplanation(Object requestor) {
            return exp;
        }
    }

    protected final RetryReason reason;

    public RetryReason getReason() {
        return reason;
    }

    public String getCustomName() {
        return customName;
    }

    public String getComment() {
        if (customComment != null) {
            return customComment;
        }
        /* Return default comment for some states. */
        if (this.reason == RetryReason.EMPTY_FOLDER) {
            return _JDT.T.decrypter_empty_folder_comment();
        } else if (this.reason == RetryReason.GEO) {
            return _JDT.T.decrypter_unavailable_geo_comment();
        } else if (this.reason == RetryReason.PLUGIN_SETTINGS) {
            return _JDT.T.decrypter_pluginsettings_comment();
        } else if (this.reason == RetryReason.IP) {
            return _JDT.T.decrypter_unavailable_ip_comment();
        } else {
            return null;
        }
    }

    public String getCustomComment() {
        return customComment;
    }

    protected final String customName;
    protected final String customComment;

    public DecrypterRetryException(RetryReason reason) {
        this(reason, null, null);
    }

    public DecrypterRetryException(RetryReason reason, final String customName) {
        this(reason, customName, null);
    }

    public DecrypterRetryException(final RetryReason reason, final String customName, final String customComment) {
        this.reason = reason;
        this.customName = customName;
        this.customComment = customComment;
    }

    public DecrypterRetryException(final RetryReason reason, final String customName, final String customComment, Throwable cause) {
        super(cause);
        this.reason = reason;
        this.customName = customName;
        this.customComment = customComment;
    }
}
