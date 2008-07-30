package jd.plugins.optional.webinterface;

import java.io.IOException;
import java.io.OutputStream;

public class JDSimpleWebserverResponseCreator {
    /**
     * The body
     */
    private StringBuilder body;

    /**
     * Binary body
     */
    private byte[] bytes;

    private String contentType;

    /**
     * The headers
     */
    private StringBuilder headers;

    /**
     * Create new response
     */
    public JDSimpleWebserverResponseCreator() {
        headers = new StringBuilder();
        body = new StringBuilder();
        contentType = "text/html";
    }

    /**
     * Append the given string to the body so far
     * 
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
     * 
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
     * Mark the response as 200 OK. This also sets the content length, so
     * the method should not be called until all content has been appended
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
                headers.append(body.toString().getBytes("iso-8859-1").length);
            }
            headers.append("\r\n");
        } catch (Exception e) {
        }

    }

    /**
     * Set a redirect
     * 
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
        outputStream.write(headers.toString().getBytes("iso-8859-1"));
        if (bytes != null) {
            outputStream.write(bytes);
        } else {
            outputStream.write(body.toString().getBytes("iso-8859-1"));
        }
    }
}
