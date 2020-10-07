package jd.controlling.captcha;

import org.jdownloader.captcha.v2.Challenge;

public class SkipException extends Exception {
    /**
     *
     */
    private static final long  serialVersionUID = 1L;
    private final SkipRequest  skipRequest;
    private final Challenge<?> challenge;

    public Challenge<?> getChallenge() {
        return challenge;
    }

    public SkipException(Challenge<?> challenge, SkipRequest skipRequest) {
        this(challenge, skipRequest, null);
    }

    public SkipException(Challenge<?> challenge, SkipRequest skipRequest, final String message) {
        super(skipRequest.name() + (message != null ? (":" + message) : ""));
        this.skipRequest = skipRequest;
        this.challenge = challenge;
    }

    public SkipRequest getSkipRequest() {
        return skipRequest;
    }
}
