package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.io.File;

import jd.controlling.IOPermission;
import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;

public class BasicCaptchaChallenge extends ImageCaptchaChallenge<String> {

    public BasicCaptchaChallenge(IOPermission ioPermission, final String method, final File file, final String defaultValue, final String explain, Plugin plugin, int flag) {
        super(file, method, explain, plugin, ioPermission);

    }

    public boolean isSolved() {
        return StringUtils.isNotEmpty(getResult());
    }

}
