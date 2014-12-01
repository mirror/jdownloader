package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.RemoteAPISignatureHandler;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSignatureRequired;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.remoteapi.exceptions.RemoteAPIException;

@ApiNamespace("UploadInterface")
public interface UploadInterface extends RemoteAPIInterface, RemoteAPISignatureHandler {

    public String upload(RemoteAPIRequest request) throws RemoteAPIException;

    @ApiSignatureRequired
    public LogCollection get(String id, String name);

    public String setKey(String key);

    @ApiSignatureRequired
    public LogCollection getLogByID(String id);

    @ApiSignatureRequired
    public void logunsorted(String id, RemoteAPIResponse response) throws InternalApiException;

    @ApiSignatureRequired
    public void log(String id, RemoteAPIResponse response) throws InternalApiException;

    // @ApiSignatureRequired
    // public HashMap<String, String> getTotalLogByID(String id);

}
