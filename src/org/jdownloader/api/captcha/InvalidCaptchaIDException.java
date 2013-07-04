package org.jdownloader.api.captcha;

import org.appwork.remoteapi.exceptions.RemoteAPIException;

public class InvalidCaptchaIDException extends RemoteAPIException {

    public InvalidCaptchaIDException() {
        super(CaptchaAPI.Error.NOT_AVAILABLE);
    }

}
