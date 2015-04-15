package org.jdownloader.captcha.v2.solver.browser;

import org.jdownloader.captcha.v2.AbstractResponse;

public class BrowserResponse extends AbstractResponse<String> {

    public BrowserResponse(AbstractBrowserChallenge challenge, Object solver, String captchaCode, int priority) {
        super(challenge, solver, priority, captchaCode);
    }

}
