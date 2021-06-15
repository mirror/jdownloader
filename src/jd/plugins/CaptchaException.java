package jd.plugins;

import jd.controlling.captcha.SkipRequest;

import org.jdownloader.captcha.blacklist.BlacklistEntry;

public class CaptchaException extends PluginException {
    private final SkipRequest    skipRequest;
    private final BlacklistEntry blackListEntry;

    public BlacklistEntry getBlackListEntry() {
        return blackListEntry;
    }

    public SkipRequest getSkipRequest() {
        return skipRequest;
    }

    public CaptchaException(SkipRequest skipRequest) {
        this(skipRequest, null);
    }

    public CaptchaException(BlacklistEntry blackListEntry) {
        this(blackListEntry.getSkipRequest(), blackListEntry);
    }

    protected CaptchaException(SkipRequest skipRequest, BlacklistEntry blackListEntry) {
        super(LinkStatus.ERROR_CAPTCHA);
        this.skipRequest = skipRequest;
        this.blackListEntry = blackListEntry;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        if (skipRequest != null) {
            sb.append(":SkipRequest=").append(skipRequest.name());
        }
        if (blackListEntry != null) {
            sb.append(":BlacklistEntry=").append(blackListEntry.toString());
        }
        return sb.toString();
    }

    public void throwMeIfNoRefresh() throws CaptchaException {
        switch (getSkipRequest()) {
        case REFRESH:
            return;
        default:
            throw this;
        }
    }
}
