package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.api.DeprecatedAPIHttpServerController;
import org.jdownloader.controlling.UniqueAlltimeID;

public abstract class BrowserReference implements HttpRequestHandler {

    private HttpHandlerInfo          handlerInfo;
    private AbstractBrowserChallenge challenge;
    private UniqueAlltimeID          id;
    private int                      port;

    public BrowserReference(AbstractBrowserChallenge challenge) {
        this.challenge = challenge;
        id = new UniqueAlltimeID();
        this.port = 12345;

    }

    public void open() throws IOException {
        handlerInfo = DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(port, true, this);
        CrossSystem.openURL("http://127.0.0.1:" + port + "/" + id.getID());
    }

    public void dispose() {
        if (handlerInfo != null) {
            DeprecatedAPIHttpServerController.getInstance().unregisterRequestHandler(handlerInfo);
        }
    }

    @Override
    public boolean onGetRequest(GetRequest request, HttpResponse response) throws BasicRemoteAPIException {

        if (!StringUtils.equals(request.getRequestedPath(), "/" + id.getID())) {
            return false;
        }
        try {
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            response.getOutputStream(true).write(challenge.getHTML().getBytes("UTF-8"));
            return true;
        } catch (Throwable e) {
            error(response, e);
            return true;
        }

    }

    private void error(HttpResponse response, Throwable e) {
        try {
            response.setResponseCode(ResponseCode.SERVERERROR_INTERNAL);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            response.getOutputStream(true).write(Exceptions.getStackTrace(e).getBytes("UTF-8"));
        } catch (Throwable e1) {
            throw new WTFException(e1);
        }

    }

    @Override
    public boolean onPostRequest(PostRequest request, HttpResponse response) throws BasicRemoteAPIException {
        if (!StringUtils.equals(request.getRequestedPath(), "/" + id.getID())) {
            return false;
        }
        try {
            String parameter = request.getParameterbyKey("g-recaptcha-response");
            onResponse(parameter);
            if (parameter == null) {
                throw new WTFException("No Response");
            }
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            response.getOutputStream(true).write("You can close the browser now".getBytes("UTF-8"));

            // Close Browser Tab
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_W);
            Thread.sleep(100);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.keyRelease(KeyEvent.VK_W);
            return true;
        } catch (Throwable e) {
            error(response, e);
            return true;
        }

    }

    abstract void onResponse(String parameter);

}
