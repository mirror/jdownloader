package jd.plugins;

import org.jdownloader.translate._JDT;

public class DecrypterRetryException extends Exception {
    public static enum RetryReason {
        CAPTCHA(_JDT.T.decrypter_wrongcaptcha()),
        NO_ACCOUNT(_JDT.T.decrypter_invalidaccount()),
        PLUGIN_DEFECT(_JDT.T.decrypter_plugindefect()),
        PLUGIN_SETTINGS(_JDT.T.decrypter_pluginsettings()),
        PASSWORD(_JDT.T.decrypter_wrongpassword()),
        HOST(_JDT.T.plugins_errors_hosterproblem());
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

    public DecrypterRetryException(RetryReason reason, final String customName, final String customComment) {
        this.reason = reason;
        this.customName = customName;
        this.customComment = customComment;
    }

    public DecrypterRetryException(RetryReason reason, final String customName, final String customComment, Throwable cause) {
        super(cause);
        this.reason = reason;
        this.customName = customName;
        this.customComment = customComment;
    }
}
