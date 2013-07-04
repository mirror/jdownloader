package org.jdownloader.api.captcha;

import org.appwork.remoteapi.exceptions.RemoteAPIException;

public class InvalidChallengeTypeException extends RemoteAPIException {

    public InvalidChallengeTypeException(String type) {
        super(CaptchaAPI.Error.UNKNOWN_CHALLENGETYPE, type);
    }

}
