package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

public interface CanCancelInterface {
    public boolean isDone();

    public void update() throws Throwable;
}
