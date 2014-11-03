package org.jdownloader.captcha.v2.solver.jac;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class JACCaptchaResponse extends CaptchaResponse {

    private int unmodifiedTrustValue;

    public int getUnmodifiedTrustValue() {
        return unmodifiedTrustValue;
    }

    public JACCaptchaResponse(BasicCaptchaChallenge captchaChallenge, JACSolver jacSolver, String captchaCode, int trust, int orgTrust) {
        super(captchaChallenge, jacSolver, captchaCode, trust);
        unmodifiedTrustValue = orgTrust;
    }

}
