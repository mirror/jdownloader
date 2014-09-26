package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.RemoteAPISignatureHandler;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSignatureRequired;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.jdownloader.myjdownloader.client.json.JsonMap;

@ApiNamespace("JService")
public interface JService extends RemoteAPIInterface, RemoteAPISignatureHandler {

    public String st1(String id, String os, RemoteAPIRequest request) throws RemoteAPIException;

    public String st(String id, String version, String os, RemoteAPIRequest request) throws RemoteAPIException;

    public long get5(int version);

    public long get10(int version);

    public void check(RemoteAPIRequest request, RemoteAPIResponse response);

    public long get30(int version);

    public long get60(int version);

    @ApiSignatureRequired
    public long getNew(int version, int minutes, String country, String os);

    @ApiSignatureRequired
    public JsonMap getMemoryUsage();

}
