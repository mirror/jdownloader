/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.remoteapi.test
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.jdownloader.extensions.newWebinterface;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.IO;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.extensions.newWebinterface.translate.newWebinterfaceTranslation;

/**
 * @author thomas
 * 
 */
public class WebinterfaceHandler implements HttpRequestHandler {
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.net.httpserver.HttpRequestHandler#onGetRequest(org.
     * appwork.utils.net.httpserver.requests.GetRequest,
     * org.appwork.utils.net.httpserver.responses.HttpResponse)
     */
    public boolean onGetRequest(final GetRequest request, final HttpResponse response) {
        // TODO Auto-generated method stub
        // TODO: SECURITY
        try {
            String path = request.getRequestedPath().substring(1);
            if (path.contains(".."))
            // Bad Security Hole: Some Browser may allow Urls with Parts like
            // "/../" witch will give the User Access to all Files on the
            // current Disk.
            // There might be a better Solution for this, i will do this later
            {
                response.setResponseCode(ResponseCode.ERROR_FORBIDDEN);
                return true;
            }
            if (path.isEmpty())
                path = "index.html";
            else if (path.equals("lang.js")) {
                // TODO:
                // - Decide by Headers (Cookie & Browserlanguage) and
                // Config-Settings which Language should be used
                response.setResponseCode(ResponseCode.SUCCESS_OK);

                final newWebinterfaceTranslation ti = TranslationFactory.create(newWebinterfaceTranslation.class);
                final Method[] methods = ti._getHandler().getMethods();
                String resp = "if( typeof(JD) == \"undefined\" ) JD= new Object();\r\n";
                resp += "JD.lng = {\r\n";
                for (final Method m : methods) {
                    resp += "\t" + m.getName() + " : \"" + ti._getHandler().getTranslation(m) + "\",\r\n";
                }
                resp += "\tLANGUAGE : \"ENGLISH\"\r\n";
                resp += "};";
                response.getOutputStream().write(resp.getBytes());
                // TODO:
                // - ^^ Should match with the sent header.

                response.getOutputStream().flush();

                return true;
            }
            final URL url = this.getClass().getResource("resources/" + path);
            if (url != null) {
                response.setResponseCode(ResponseCode.SUCCESS_OK);

                // TODO:
                // -Set Mime-Typ, Content-Endoding
                // -Compress js and use gzip if possible
                // -Implement Session Handling

                response.getOutputStream().write(IO.readURL(url));
                response.getOutputStream().flush();
                return true;
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        response.setResponseCode(ResponseCode.ERROR_NOT_FOUND);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.net.httpserver.HttpRequestHandler#onPostRequest(org
     * .appwork.utils.net.httpserver.requests.PostRequest,
     * org.appwork.utils.net.httpserver.responses.HttpResponse)
     */
    public boolean onPostRequest(final PostRequest request, final HttpResponse response) {
        // TODO Auto-generated method stub
        return false;
    }
}
