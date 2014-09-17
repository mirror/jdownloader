package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.util.List;

import org.appwork.utils.net.httpserver.requests.HeadRequest;
import org.appwork.utils.net.httpserver.requests.KeyValuePair;
import org.jdownloader.api.myjdownloader.MyJDownloaderGetRequest.GetData;

public class MyJDownloaderHeadRequest extends HeadRequest implements MyJDownloaderRequestInterface {

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
    public String getDiffType() throws IOException {
        return requestProperties.diffType;
    }

    public MyJDownloaderHeadRequest(MyJDownloaderHttpConnection connection) {
        super(connection);

    }

    public String getRequestConnectToken() {
        return getConnection().getRequestConnectToken();
    }

    @Override
    public MyJDownloaderHttpConnection getConnection() {
        return (MyJDownloaderHttpConnection) super.getConnection();
    }

    @Override
    public long getDiffKeepAlive() {
        return requestProperties.diffKeepalive;
    }

    @Override
    public String getDiffID() {
        return requestProperties.diffID;
    }

}