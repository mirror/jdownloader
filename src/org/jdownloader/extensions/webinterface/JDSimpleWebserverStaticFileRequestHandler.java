//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.webinterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;

public class JDSimpleWebserverStaticFileRequestHandler {

    private Logger logger = jd.controlling.JDLogger.getLogger();
    private JDSimpleWebserverResponseCreator response;
    private HashMap<String, String> headers = new HashMap<String, String>();

    /**
     * Create a new handler that serves files from a base directory
     * 
     * @param base
     *            directory
     */
    public JDSimpleWebserverStaticFileRequestHandler(HashMap<String, String> headers, JDSimpleWebserverResponseCreator response) {
        this.headers = headers;
        this.response = response;
    }

    /**
     * Handle a request
     * 
     * @param type
     * @param url
     * @param parameters
     * @return a response
     */
    public void handleRequest(String url, HashMap<String, String> requestParameter) {
        File fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);

        if (fileToRead.length() > 10 * 1024) {

            response.setContentType("application/octet-stream");
            if (!headers.containsKey("range")) {
                response.setFileServe(fileToRead.toString(), 0, -1, fileToRead.length(), false);
            } else {
                String[] dat = new Regex(headers.get("range"), "bytes=(\\d+)-(\\d+)?").getRow(0);
                if (dat[1] == null) {
                    response.setFileServe(fileToRead.toString(), Long.parseLong(dat[0]), -1, fileToRead.length(), true);
                } else {
                    response.setFileServe(fileToRead.toString(), Long.parseLong(dat[0]), Long.parseLong(dat[1]), fileToRead.length(), true);
                }
            }
            response.setFilename(fileToRead.getName());
            response.setOk();
            return;
        }
        HashMap<String, String> mimes = new HashMap<String, String>();

        mimes.put("html", "text/html");
        mimes.put("htm", "text/html");
        mimes.put("txt", "text/plain");
        mimes.put("gif", "image/gif");
        mimes.put("css", "text/css");
        mimes.put("png", "image/png");
        mimes.put("jpeg", "image/jpeg");
        mimes.put("jpg", "image/jpeg");
        mimes.put("jpe", "image/jpeg");
        mimes.put("ico", "image/x-icon");
        // determine mime type
        String mimeType = null;
        int indexOfDot = fileToRead.getName().indexOf('.');
        if (indexOfDot >= 0) {
            String extension = JDIO.getFileExtension(fileToRead);
            mimeType = "text/plain";
            if (mimes.containsKey(extension.toLowerCase())) {
                mimeType = mimes.get(extension.toLowerCase());
            }
        }
        if (mimeType != null) {
            response.setContentType(mimeType);
        }

        FileInputStream in = null;
        StringWriter writer;
        try {
            in = new FileInputStream(fileToRead);
            int nextByte;
            if (mimeType == null || mimeType.startsWith("text")) {
                writer = new StringWriter();
                while ((nextByte = in.read()) >= 0) {
                    writer.write(nextByte);
                }
                writer.close();
                response.addContent(writer.toString());
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((nextByte = in.read()) >= 0) {
                    out.write(nextByte);
                }
                out.close();
                /* logger.info("size " + out.size()); */
                response.setBinaryContent(out.toByteArray());
            }
        } catch (Exception e) {
            logger.severe("error reading file" + e);
            response.setError(e);
            return;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.severe("failed to close stream" + e);
                }
            }
        }

        response.setOk();
        return;
    }
}