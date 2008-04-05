package jd.plugins.webinterface;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class JDSimpleWebserverResponse {
        /**
         * The headers
         */
        private StringBuilder headers;

        /**
         * The body
         */
        private StringBuilder body;

        
        private String contentType;

        /**
         * Binary body
         */
        private byte[] bytes;

        /**
         * Create new response
         */
        public JDSimpleWebserverResponse() {
            this.headers = new StringBuilder();
            this.body = new StringBuilder();
            this.contentType = "text/html; charset=utf-8";
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
         * Append the given string to the body so far
         * 
         * @param content
         *            content
         */
        public void addContent(String content) {
            this.body.append(content);
        }

        /**
         * Mark the response as 200 OK. This also sets the content length, so
         * the method should not be called until all content has been appended
         */
        public void setOk() {
            headers.append("HTTP/1.1 200 OK\n");
            headers.append("Content-Type: ");
            headers.append(this.contentType);
            headers.append("\n");
            try {
                headers.append("Content-Length: ");
                if (this.bytes != null) {
                    headers.append(bytes.length);
                } else {
                    headers.append(body.toString().getBytes("utf-8").length);
                }
                headers.append("\n");
            } catch (UnsupportedEncodingException e) {
                /* logger.error("failed to encode", e); */
            }
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
            headers.append("\n");
            outputStream.write(headers.toString().getBytes("iso-8859-1"));
            if (this.bytes != null) {
                outputStream.write(this.bytes);
            } else {
                outputStream.write(body.toString().getBytes("iso-8859-1"));
            }
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
            headers.append("\n");
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
            headers.append("HTTP/1.1 403 Resource not found\n");
            body.append("<html><body><h1><p>404 Resource not found</p></h1>");
            body.append(url);
            body.append("</body></html>");
        }

        public void setAuth_needed() {
            headers.append("HTTP/1.1 401 Unauthorized\n");
            headers.append("WWW-Authenticate: Basic realm=\"JDownloader\"");
        }
        
        
        public void setAuth_failed() {
            headers.append("HTTP/1.1 403 Forbidden\n");
           /* headers.append("WWW-Authenticate: Basic realm=\"JDownloader\"");*/
            body.append("<html><body><h1><p>403 Forbidden</p></h1></body></html>");
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
         * Set binary content
         * 
         * @param bytes
         */
        public void setBinaryContent(byte[] bytes) {
            this.bytes = bytes;
        }
    }

