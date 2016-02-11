package jd.controlling.captcha;

public class SkipException extends Exception {

    private SkipRequest skipRequest;

    public SkipException(SkipRequest single) {
        super(single.name());
        this.skipRequest = single;
    }

    public SkipRequest getSkipRequest() {
        return skipRequest;
    }

}
