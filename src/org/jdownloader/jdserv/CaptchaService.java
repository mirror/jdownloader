package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPISignatureHandler;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.RemoteAPIException;

@ApiNamespace("captcha")
public interface CaptchaService extends RemoteAPIInterface, RemoteAPISignatureHandler {

    void upload(RemoteAPIRequest request) throws RemoteAPIException;

}
