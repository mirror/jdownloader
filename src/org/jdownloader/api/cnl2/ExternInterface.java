package org.jdownloader.api.cnl2;

import java.io.IOException;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpConnection.ConnectionHook;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.appwork.utils.net.httpserver.handler.ExtendedHttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.OptionsRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.api.DeprecatedAPIHttpServerController;
import org.jdownloader.api.RemoteAPIConfig;

public class ExternInterface {
    private class ExternInterfaceRemoteAPI extends RemoteAPI implements ExtendedHttpRequestHandler, ConnectionHook {
        @Override
        public boolean onGetRequest(GetRequest request, HttpResponse response) throws BasicRemoteAPIException {
            if (request instanceof OptionsRequest) {
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, "0"));
                response.setResponseCode(ResponseCode.SUCCESS_OK);
                return true;
            }
            return super.onGetRequest(request, response);
        }

        @Override
        public void onBeforeSendHeaders(HttpResponse response) {
            // https://scotthelme.co.uk/hardening-your-http-response-headers/#x-content-type-options
            HttpRequest request = response.getConnection().getRequest();
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_MAX_AGE, "1800"));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST"));
            final String headers = request.getRequestHeaders().getValue("Access-Control-Request-Headers");
            if (headers != null) {
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_HEADERS, headers));
            }
            response.getResponseHeaders().remove(HTTPConstants.HEADER_RESPONSE_CONTENT_SECURITY_POLICY);
            response.getResponseHeaders().remove(HTTPConstants.HEADER_RESPONSE_X_FRAME_OPTIONS);
            response.getResponseHeaders().remove(HTTPConstants.HEADER_RESPONSE_X_XSS_PROTECTION);
            response.getResponseHeaders().remove(HTTPConstants.HEADER_RESPONSE_REFERRER_POLICY);
            response.getResponseHeaders().remove(HTTPConstants.HEADER_RESPONSE_X_CONTENT_TYPE_OPTIONS);
        }

        @Override
        public void onBeforeRequest(HttpRequest request, HttpResponse response) {
            // do not do any origin filter. All origins are allowed
            response.setHook(this);
        }

        @Override
        public void onAfterRequest(HttpRequest request, HttpResponse response, boolean handled) {
        }

        @Override
        public void onAfterRequestException(HttpRequest request, HttpResponse response, Throwable e) {
        }
    }

    private static ExternInterface INSTANCE = new ExternInterface();

    private ExternInterface() {
        final RemoteAPIConfig config = JsonConfig.create(RemoteAPIConfig.class);
        if (config.isExternInterfaceEnabled()) {
            final Thread serverInit = new Thread() {
                @Override
                public void run() {
                    final RemoteAPI remoteAPI = new ExternInterfaceRemoteAPI();
                    try {
                        remoteAPI.register(new ExternInterfaceImpl());
                        while (config.isExternInterfaceEnabled() && !Thread.currentThread().isInterrupted()) {
                            try {
                                final HttpHandlerInfo handler = DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(9666, config.isExternInterfaceLocalhostOnly(), remoteAPI);
                                // handler.getHttpServer().registerRequestHandler(remoteAPI);
                                // handler.getHttpServer().registerRequestHandler(new HttpRequestHandler() {
                                //
                                // @Override
                                // public boolean onPostRequest(PostRequest request, HttpResponse response) throws BasicRemoteAPIException {
                                // return false;
                                // }
                                //
                                // @Override
                                // public boolean onGetRequest(GetRequest httpRequest, HttpResponse httpResponse) throws
                                // BasicRemoteAPIException {
                                // if (StringUtils.startsWithCaseInsensitive(httpRequest.getRequestedURL(), "http://")) {
                                // final Browser br = new Browser();
                                // br.setFollowRedirects(false);
                                // URLConnectionAdapter con = null;
                                // try {
                                // con = br.openGetConnection(httpRequest.getRequestedURL());
                                // httpResponse.setResponseCode(ResponseCode.get(con.getResponseCode()));
                                // Set<Entry<String, List<String>>> responseHeaders = con.getRequest().getResponseHeaders().entrySet();
                                // Iterator<Entry<String, List<String>>> it = responseHeaders.iterator();
                                // while (it.hasNext()) {
                                // final Entry<String, List<String>> next = it.next();
                                // if (next.getValue() != null) {
                                // for (String value : next.getValue()) {
                                // httpResponse.getResponseHeaders().add(new HTTPHeader(next.getKey(), value));
                                // }
                                // }
                                // }
                                // final OutputStream os = httpResponse.getOutputStream(true);
                                // final InputStream is = con.getInputStream();
                                // IO.readStreamToOutputStream(-1, is, os, false);
                                // os.close();
                                // return true;
                                // } catch (IOException e) {
                                // throw new BasicRemoteAPIException(e);
                                // } finally {
                                // if (con != null) {
                                // con.disconnect();
                                // }
                                // }
                                // }
                                // return false;
                                // }
                                // });
                                break;
                            } catch (IOException e) {
                                Thread.sleep(30 * 1000l);
                            }
                        }
                    } catch (Throwable e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                }
            };
            serverInit.setDaemon(true);
            serverInit.setName("ExternInterface: init");
            serverInit.start();
        }
    }

    public static ExternInterface getINSTANCE() {
        return INSTANCE;
    }
}
