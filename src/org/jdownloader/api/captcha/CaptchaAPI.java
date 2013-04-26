package org.jdownloader.api.captcha;

import java.util.List;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.ApiDoc;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.APIError;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.remoteapi.exceptions.RemoteAPIException;

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

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) throws InternalApiException, RemoteAPIException;

    public boolean solve(final long id, String result) throws RemoteAPIException;

    public boolean abort(final long id) throws RemoteAPIException;

}
