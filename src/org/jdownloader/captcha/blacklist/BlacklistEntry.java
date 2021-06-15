package org.jdownloader.captcha.blacklist;

import jd.controlling.captcha.SkipRequest;

import org.jdownloader.captcha.v2.Challenge;

public interface BlacklistEntry<T> {
    public boolean canCleanUp();

    public boolean matches(Challenge<T> c);

    public SkipRequest getSkipRequest();
}
