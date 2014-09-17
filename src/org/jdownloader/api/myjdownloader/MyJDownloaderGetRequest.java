package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.util.List;

import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.KeyValuePair;

public class MyJDownloaderGetRequest extends GetRequest implements MyJDownloaderRequestInterface {

    public static final String  API_VERSION    = "apiVer";
    public static final String  RID            = "rid";
    public static final String  SIGNATURE      = "signature";
    public static final String  CALLBACK       = "callback";
    public static final String  DIFF_KEEPALIVE = "diffKA";
    public static final String  DIFF_ID        = "diffID";
    private static final String DIFF_TYPE      = null;

    public static class GetData {
        public static final GetData EMPTY         = new GetData();
        public long                 rid           = -1;
        public int                  apiVersion    = -1;
        public String               signature     = null;
        public String               callback      = null;
        public long                 diffKeepalive = -1;
        public String               diffID        = null;
        public String               diffType      = null;

    }

    public static GetData parseGetData(final List<KeyValuePair> requestedURLParameters) {
        final GetData ret = new GetData();
        if (requestedURLParameters != null) {
            for (final KeyValuePair param : requestedURLParameters) {
                if (param.key != null) {
                    /* key=value(parameter) */
                    if (MyJDownloaderGetRequest.CALLBACK.equalsIgnoreCase(param.key)) {
                        /* filter jquery callback */
                        ret.callback = param.value;
                        continue;
                    } else if (MyJDownloaderGetRequest.SIGNATURE.equalsIgnoreCase(param.key)) {
                        /* filter url signature */
                        ret.signature = param.value;
                        continue;
                    } else if (MyJDownloaderGetRequest.RID.equalsIgnoreCase(param.key)) {
                        ret.rid = Long.parseLong(param.value);
                        continue;
                    } else if (MyJDownloaderGetRequest.API_VERSION.equalsIgnoreCase(param.key)) {
                        ret.apiVersion = Integer.parseInt(param.value);
                        continue;
                    } else if (MyJDownloaderGetRequest.DIFF_KEEPALIVE.equalsIgnoreCase(param.key)) {
                        ret.diffKeepalive = Long.parseLong(param.value);
                        continue;
                    } else if (MyJDownloaderGetRequest.DIFF_ID.equalsIgnoreCase(param.key)) {
                        ret.diffID = param.value;
                        continue;
                    } else if (MyJDownloaderGetRequest.DIFF_TYPE.equalsIgnoreCase(param.key)) {
                        ret.diffType = param.value;
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
    public void setRequestedURLParameters(final List<KeyValuePair> requestedURLParameters) {
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

    @Override
    public long getDiffKeepAlive() {
        return requestProperties.diffKeepalive;
    }

    @Override
    public String getDiffID() {
        return requestProperties.diffID;
    }

    @Override
    public String getDiffType() throws IOException {
        return requestProperties.diffType;
    }

}
