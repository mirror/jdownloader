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

    public SkipException(Challenge<?> challenge, SkipRequest single) {
        super(single.name());
        this.skipRequest = single;
        this.challenge = challenge;
    }

    public SkipRequest getSkipRequest() {
        return skipRequest;
    }

}
