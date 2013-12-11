package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.util.LinkedList;

import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.requests.GetRequest;

public class MyJDownloaderGetRequest extends GetRequest implements MyJDownloaderRequestInterface {

    public static final String API_VERSION = "apiVer";
    public static final String RID         = "rid";
    public static final String SIGNATURE   = "signature";
    public static final String CALLBACK    = "callback";

    public static class GetData {
        public static final GetData EMPTY      = new GetData();
        public long                 rid        = -1;
        public int                  apiVersion = -1;
        public String               signature  = null;
        public String               callback   = null;

    }

    public static GetData parseGetData(final LinkedList<String[]> requestedURLParameters) {
        final GetData ret = new GetData();
        if (requestedURLParameters != null) {
            for (final String[] param : requestedURLParameters) {
                if (param[1] != null) {
                    /* key=value(parameter) */
                    if (MyJDownloaderGetRequest.CALLBACK.equalsIgnoreCase(param[0])) {
                        /* filter jquery callback */
                        ret.callback = param[1];
                        continue;
                    } else if (MyJDownloaderGetRequest.SIGNATURE.equalsIgnoreCase(param[0])) {
                        /* filter url signature */
                        ret.signature = param[1];
                        continue;
                    } else if (MyJDownloaderGetRequest.RID.equalsIgnoreCase(param[0])) {
                        ret.rid = Long.parseLong(param[1]);
                        continue;
                    } else if (MyJDownloaderGetRequest.API_VERSION.equalsIgnoreCase(param[0])) {
                        ret.apiVersion = Integer.parseInt(param[1]);
                        continue;
                    }

                }
            }
        }
        return ret;
    }

    public MyJDownloaderGetRequest(HttpConnection connection) {
        super(connection);

    }

    public String getRequestConnectToken() {
        return getConnection().getRequestConnectToken();
    }

    @Override
    public MyJDownloaderHttpConnection getConnection() {
        return (MyJDownloaderHttpConnection) super.getConnection();
    }

    private GetData requestProperties = GetData.EMPTY;

    @Override
    public void setRequestedURLParameters(final LinkedList<String[]> requestedURLParameters) {
        super.setRequestedURLParameters(requestedURLParameters);

        requestProperties = MyJDownloaderGetRequest.parseGetData(requestedURLParameters);

    }

    public int getApiVersion() {
        return requestProperties.apiVersion;
    }

    @Override
    public long getRid() throws IOException {
        return requestProperties.rid;
    }

    @Override
    public String getSignature() {
        return requestProperties.signature;
    }

    @Override
    public String getJqueryCallback() {
        return requestProperties.callback;
    }

}
