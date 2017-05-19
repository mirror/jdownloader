package org.jdownloader.api.cnl2;

import java.io.IOException;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.jdownloader.api.DeprecatedAPIHttpServerController;
import org.jdownloader.api.RemoteAPIConfig;
import org.jdownloader.api.myjdownloader.OptionsRequestHandler;

public class ExternInterface {
    private static ExternInterface INSTANCE = new ExternInterface();

    private ExternInterface() {
        final RemoteAPIConfig config = JsonConfig.create(RemoteAPIConfig.class);
        if (config.isExternInterfaceEnabled()) {
            final Thread serverInit = new Thread() {
                @Override
                public void run() {
                    final RemoteAPI remoteAPI = new RemoteAPI();
                    try {
                        remoteAPI.register(new ExternInterfaceImpl());
                        while (config.isExternInterfaceEnabled() && !Thread.currentThread().isInterrupted()) {
                            try {
                                final HttpHandlerInfo handler = DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(9666, config.isExternInterfaceLocalhostOnly(), new OptionsRequestHandler());
                                handler.getHttpServer().registerRequestHandler(remoteAPI);
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
