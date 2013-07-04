package org.jdownloader.api.captcha;

import java.util.List;

import jd.controlling.captcha.SkipRequest;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.APIError;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("captcha")
public interface CaptchaAPI extends RemoteAPIInterface {
    public static enum ABORT {
        SINGLE,
        HOSTER,
        ALL
    }

    public static enum Error implements APIError {
        NOT_AVAILABLE(ResponseCode.ERROR_NOT_FOUND),
        UNKNOWN_CHALLENGETYPE(ResponseCode.ERROR_NOT_FOUND);

        private Error(ResponseCode code) {
            this.code = code;
        }

        private ResponseCode code;

        @Override
        public ResponseCode getCode() {
            return code;
        }

    }

    @ApiDoc("returns a list of all available captcha jobs")
    public List<CaptchaJob> list();

    @ApiDoc("Returns Captcha Image as Base64 encoded data url")
    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) throws InternalApiException, InvalidCaptchaIDException;

    @ApiDoc("Returns CaptchaJob Object for the given id")
    public CaptchaJob getCaptchaJob(final long id) throws InvalidCaptchaIDException;

    public boolean solve(final long id, String result) throws InvalidCaptchaIDException, InvalidChallengeTypeException;

    @Deprecated
    /**  
     * @deprecated use #skip(id,type) instead
     * @param id
     * @return
     * @throws InvalidCaptchaIDException
     */
    public boolean skip(final long id) throws InvalidCaptchaIDException;

    public boolean skip(final long id, SkipRequest type) throws InvalidCaptchaIDException;
}
