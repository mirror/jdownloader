package org.jdownloader.captcha.v2.challenge.solvemedia;

import java.io.File;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;

import jd.plugins.Plugin;

public class SolveMediaCaptchaChallenge extends BasicCaptchaChallenge {

    public SolveMediaCaptchaChallenge(File file, String defaultValue, String explain, Plugin plg, int flag) {
        super("solvemedia", file, defaultValue, explain, plg, flag);
    }

}
