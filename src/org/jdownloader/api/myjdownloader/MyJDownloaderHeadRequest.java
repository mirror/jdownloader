package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.util.LinkedList;

import org.appwork.utils.net.httpserver.requests.HeadRequest;
import org.jdownloader.api.myjdownloader.MyJDownloaderGetRequest.GetData;

public class MyJDownloaderHeadRequest extends HeadRequest implements MyJDownloaderRequestInterface {

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

}