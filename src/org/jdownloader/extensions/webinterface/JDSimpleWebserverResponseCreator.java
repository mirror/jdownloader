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

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import jd.nutils.encoding.Encoding;

public class JDSimpleWebserverResponseCreator {

    // The body
    private StringBuilder body;

    // Binary body
    private byte[] bytes;

    private String contentType;

    // The headers
    private StringBuilder headers;

    private String filepath = null;

    private long filestart = 0;

    private long fileend = -1;

    private long filesize = 0;

    private boolean range = false;

    private String filename = null;

    // Create new response
    public JDSimpleWebserverResponseCreator() {
        headers = new StringBuilder();
        body = new StringBuilder();
        contentType = "text/html; charset=UTF-8";
    }

    /*
     * Append the given string to the body so far
     * 
     * @param content content
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
     * 
     * @param bytes
     */
    public void setBinaryContent(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setFileServe(String path, long start, long end, long filesize, boolean range) {
        this.filepath = path;
        this.filestart = start;
        if (end == -1) {
            this.fileend = filesize;
        } else {
            this.fileend = end;
        }
        this.filesize = filesize;
        this.range = range;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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
     * 
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
        if (!this.range) {
            headers.append("HTTP/1.1 200 OK\r\n");
        } else {
            headers.append("HTTP/1.1 206 Partial content\r\n");
        }
        headers.append("Connection: close\r\n");
        headers.append("Content-Type: ");
        headers.append(contentType);
        headers.append("\r\n");
        try {
            headers.append("Content-Length: ");
            if (this.filepath != null) {
                if (!this.range) {
                    headers.append(this.filesize);
                    headers.append("\r\n");
                } else {
                    if (this.fileend == -1) {
                        this.fileend = this.filesize;
                    }
                    headers.append(Math.max(0, fileend - filestart));
                    headers.append("\r\n");
                    headers.append("Content-Range: bytes " + filestart + "-" + fileend + "/" + filesize);
                    headers.append("\r\n");
                }
                if (filename != null) {
                    headers.append("Content-Disposition: attachment;filename*=UTF-8''" + Encoding.urlEncode(filename));
                    headers.append("\r\n");
                }
                headers.append("Accept-Ranges: bytes");
            } else if (bytes != null) {
                headers.append(bytes.length);
            } else {
                headers.append(body.toString().getBytes("UTF-8").length);
            }
            headers.append("\r\n");
        } catch (Exception e) {
        }
    }

    /**
     * Set a redirect
     */
    public void setRedirect() {
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
        outputStream.write(headers.toString().getBytes("UTF-8"));
        if (this.filepath != null) {
            RandomAccessFile raf = null;
            long served = 0;
            try {
                raf = new RandomAccessFile(this.filepath, "r");
                raf.seek(filestart);
                long curpos = filestart;
                int toread = 1024;
                int read = 0;
                byte[] buffer = new byte[1024];
                if (fileend - curpos < 1024) {
                    toread = (int) (fileend - curpos);
                }
                while ((read = raf.read(buffer, 0, toread)) != -1) {
                    curpos += read;
                    served += read;
                    outputStream.write(buffer);
                    if ((fileend - curpos) < 1024) {
                        toread = (int) (fileend - curpos);
                    }
                    if (toread == 0 || fileend == curpos) {
                        break;
                    }
                }
                raf.close();
            } finally {
                try {
                    raf.close();
                } catch (Exception e) {
                }
            }
        }
        if (bytes != null) {
            outputStream.write(bytes);
        } else {
            outputStream.write(body.toString().getBytes("UTF-8"));
        }
    }
}