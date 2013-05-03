package org.jdownloader.api.config;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.APIError;
import org.appwork.remoteapi.exceptions.RemoteAPIException;

public class InvalidValueException extends RemoteAPIException {
    public static enum Type implements APIError {
        INVALID_VALUE(ResponseCode.ERROR_BAD_REQUEST);
        private ResponseCode code;

        private Type(ResponseCode code) {
            this.code = code;
        }

        @Override
        public ResponseCode getCode() {
            return code;
        }

    }

    public InvalidValueException(Throwable e) {
        super(e, Type.INVALID_VALUE, null);
    }

}
