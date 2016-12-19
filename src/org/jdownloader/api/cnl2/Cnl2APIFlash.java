package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiMethodName;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiRawMethod;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("flash")
public interface Cnl2APIFlash extends RemoteAPIInterface {
    @ApiMethodName("addcnl")
    @APIParameterNames({ "response", "request", "cnl" })
    public void addcnl(RemoteAPIResponse response, RemoteAPIRequest request, CnlQueryStorable cnl) throws InternalApiException;

    @ApiMethodName("addcrypted2")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the addcrypted2 function of the cnl2 interface
     *
     * @param response
     * @param request
     */
    @APIParameterNames({ "response", "request" })
    public void addcrypted2(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("add")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the normal add function of the cnl2 interface
     *
     * @param response
     * @param request
     */
    @APIParameterNames({ "response", "request" })
    public void add(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("addcrypted")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the normal addcrypted(dlc) function of the cnl2 interface
     *
     * @param response
     * @param request
     */
    @APIParameterNames({ "response", "request" })
    public void addcrypted(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this function returns "JDownloader" and has no function name /flash/
     *
     * @param response
     * @param request
     */
    @APIParameterNames({ "response", "request" })
    public void alive(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("addcrypted2Remote")
    /**
     * this is the addcrypted2 function of the cnl2 interface for use with MyJDownloader
     *
     * @param response
     * @param request
     */
    @APIParameterNames({ "response", "request", "crypted", "jk", "k" })
    public void addcrypted2Remote(RemoteAPIResponse response, RemoteAPIRequest request, String crypted, String jk, String k);

    @ApiMethodName("add")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the normal add function of the cnl2 interface for use with MyJDownloader
     *
     * @param response
     * @param request
     */

    @APIParameterNames({ "request", "response", "password", "source", "url" })

    public void add(RemoteAPIRequest request, RemoteAPIResponse response, String password, String source, String url) throws InternalApiException;

    public void add(RemoteAPIRequest request, RemoteAPIResponse response, String fromFallback, String password, String source, String url) throws InternalApiException;
}
