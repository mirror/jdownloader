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

package jd.plugins.optional.webinterface;

import java.io.IOException;
import java.io.OutputStream;

import jd.nutils.Executer;

public class JDSimpleWebserverResponseCreator {
    
    // The body
    private StringBuilder body;

    // Binary body
    private byte[] bytes;

    private String contentType;

    // The headers
    private StringBuilder headers;

    // Create new response
    public JDSimpleWebserverResponseCreator() {
        headers = new StringBuilder();
        body = new StringBuilder();
        contentType = "text/html";
    }

    /*
     * Append the given string to the body so far 
     * @param content
     *            content
     */
    public void addContent(String content) {
        body.append(content);
    }

    public void setAuth_failed() {
        headers.append("HTTP/1.1 403 Forbidden\r\n");
        body.append("<html><body><h1><p>403 Forbidden</p></h1></body></html>");
    }

    public void setAuth_needed() {
        headers.append("HTTP/1.1 401 Unauthorized\r\n");
        headers.append("WWW-Authenticate: Basic realm=\"JDownloader\"\r\n");
    }

    /**
     * Set binary content 
     * @param bytes
     */
    public void setBinaryContent(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Set the content type (defaults to text/html utf-8)
     * 
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Set a 500 error condition
     * 
     * @param e
     *            the exception
     */
    public void setError(Exception e) {
        headers.append("HTTP/1.1 500 ");
        headers.append(e.getMessage());
        headers.append("\r\n");
        body.append("<html><body><h1><p>500 Internal server error</p></h1>");
        body.append(e.getMessage());
        body.append("</body></html>");
    }

    /**
     * Set a 404 not found
     * @param url
     *            url
     */
    public void setNotFound(String url) {
        headers.append("HTTP/1.1 403 Resource not found\r\n");
        body.append("<html><body><h1><p>404 Resource not found</p></h1>");
        body.append(url);
        body.append("</body></html>");
    }

    // leg die lib ins home_dir gibt ja noch keine lib, nur java und class f
    /**
     * Mark the response as 200 OK. This also sets the content length, so the
     * method should not be called until all content has been appended
     */
    public void setOk() {
        headers.append("HTTP/1.1 200 OK\r\n");
        headers.append("Connection: close\r\n");
        headers.append("Content-Type: ");
        headers.append(contentType);
        headers.append("\r\n");
        try {
            headers.append("Content-Length: ");
            if (bytes != null) {
                headers.append(bytes.length);
            } else {
                headers.append(body.toString().getBytes(Executer.CODEPAGE).length);
            }
            headers.append("\r\n");
        } catch (Exception e) {
        }
    }

    /**
     * Set a redirect
     * @param url
     *            url to redirect to
     */
    public void setRedirect(String url) {
        headers.append("HTTP/1.1 307 Temporary Redirect\n");
        headers.append("Location: /\n");
    }

    /**
     * Write the complete response to the given output stream
     * 
     * @param outputStream
     *            stream
     * @throws IOException
     *             on error
     */
    public void writeToStream(OutputStream outputStream) throws IOException {
        headers.append("\r\n");
        outputStream.write(headers.toString().getBytes(Executer.CODEPAGE));
        if (bytes != null) {
            outputStream.write(bytes);
        } else {
            outputStream.write(body.toString().getBytes(Executer.CODEPAGE));
        }
    }
}