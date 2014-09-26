package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.RemoteAPISignatureHandler;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSessionRequired;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("BuyPremiumInterface")
public interface BuyPremiumInterface extends RemoteAPIInterface, RemoteAPISignatureHandler {
    @ApiSessionRequired
    public String put(String domain, String pattern, String url);

    @ApiSessionRequired
    public String remove(String domain);

    public void redirect(String domainOrUrl, String src, RemoteAPIRequest request, RemoteAPIResponse response) throws InternalApiException;

    @ApiSessionRequired
    public void list(RemoteAPIResponse response) throws InternalApiException;

    @ApiSessionRequired
    public void listCache(RemoteAPIResponse response) throws InternalApiException;

    @ApiSessionRequired
    public boolean clearCache();

}
