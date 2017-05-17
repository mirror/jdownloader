package org.jdownloader.api.myjdownloader;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.myjdownloader.client.json.ServerErrorType;

public class SessionTokenCheckHandler implements HttpRequestHandler {
    @Override
    public boolean onPostRequest(PostRequest request, HttpResponse response) throws BasicRemoteAPIException {
        checkToken(request, response);
        return false;
    }

    @Override
    public boolean onGetRequest(GetRequest request, HttpResponse response) throws BasicRemoteAPIException {
        checkToken(request, response);
        return false;
    }

    private void checkToken(HttpRequest request, HttpResponse response) throws BasicRemoteAPIException {
        final HttpConnection connection = request.getConnection();
        if (connection instanceof MyJDownloaderDirectHttpConnection) {
            final MyJDownloaderDirectHttpConnection myConnection = (MyJDownloaderDirectHttpConnection) connection;
            final String sessionToken = myConnection.getRequestConnectToken();
            if (!MyJDownloaderController.getInstance().isSessionValid(sessionToken)) {
                writeTokenInvalid(response);
            }
        }
    }

    private void writeTokenInvalid(HttpResponse response) throws BasicRemoteAPIException {
        final ServerErrorType type = ServerErrorType.TOKEN_INVALID;
        final ResponseCode code = ResponseCode.get(type.getCode());
        throw new BasicRemoteAPIException(null, type.name(), code, null);
    }
}
