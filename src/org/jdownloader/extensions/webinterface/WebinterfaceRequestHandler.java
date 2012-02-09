package org.jdownloader.extensions.webinterface;

import java.net.URL;
import java.net.URLDecoder;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.FileResponse;
import org.appwork.utils.net.httpserver.responses.HttpResponse;

public class WebinterfaceRequestHandler implements HttpRequestHandler {

    private static String ROOT = "webinterface/";

    public WebinterfaceRequestHandler(WebinterfaceExtension webinterface) {

    }

    public boolean onGetRequest(GetRequest request, HttpResponse response) {
        String path = request.getRequestedPath();
        String filePath = new Regex(path, ROOT + "(.+)").getMatch(0);
        if (filePath != null) {
            try {
                if (filePath.contains("/../")) throw new WTFException("Directory Traversal!");
                URL file = Application.getRessourceURL(ROOT + URLDecoder.decode(filePath, "UTF-8"));
                if (file == null) file = WebinterfaceRequestHandler.class.getResource(ROOT + URLDecoder.decode(filePath, "UTF-8"));
                if (file != null) {
                    final FileResponse fr = new FileResponse(request, response, file) {

                        protected boolean useContentDisposition() {
                            if (this.getMimeType().contains("application")) return true;
                            return false;
                        }

                    };

                    fr.sendFile();
                    return true;
                }
            } catch (Throwable e) {
                Log.exception(e);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public boolean onPostRequest(PostRequest request, HttpResponse response) {
        return false;
    }

}
