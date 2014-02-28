package org.jdownloader.api.myjdownloader;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.OptionsRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;

public class OptionsRequestHandler implements HttpRequestHandler {
    
    @Override
    public boolean onPostRequest(PostRequest request, HttpResponse response) {
        return false;
    }
    
    @Override
    public boolean onGetRequest(GetRequest request, HttpResponse response) {
        if (request instanceof OptionsRequest) {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_MAX_AGE, "30"));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST"));
            final String headers = request.getRequestHeaders().getValue("Access-Control-Request-Headers");
            if (headers != null) {
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_HEADERS, headers));
            }
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, "0"));
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            return true;
        } else {
            return false;
        }
    }
    
}
