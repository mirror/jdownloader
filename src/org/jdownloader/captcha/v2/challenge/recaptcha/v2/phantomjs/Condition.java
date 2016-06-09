package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

import java.io.IOException;

public interface Condition {
    public boolean breakIfTrue() throws InterruptedException, IOException;
}
