package org.jdownloader.captcha.api;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.FileNotFound404Exception;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("captchaforward")
public interface CaptchaForwarderAPIInterface extends RemoteAPIInterface {
    public long createJobRecaptchaV2(String siteKey, String stoken, final String domain, final String reason);

    public String getResult(long jobID) throws FileNotFound404Exception, InternalApiException;
}
