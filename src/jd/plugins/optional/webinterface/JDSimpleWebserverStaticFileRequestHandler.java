package jd.plugins.optional.webinterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.utils.JDUtilities;

public class JDSimpleWebserverStaticFileRequestHandler {

    private Logger logger = JDUtilities.getLogger();
    private JDSimpleWebserverResponseCreator response;

    /**
     * Create a new handler that serves files from a base directory
     * 
     * @param base
     *            directory
     */
    public JDSimpleWebserverStaticFileRequestHandler(JDSimpleWebserverResponseCreator response) {

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
    public void handleRequest(String url,HashMap<String, String> requestParameter) {
        File fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);
        
        HashMap<String, String> mimes = new HashMap<String, String>();

        mimes.put("html", "text/html");
        mimes.put("htm", "text/html");
        mimes.put("txt", "text/plain");
        mimes.put("gif", "image/gif");
        mimes.put("css", "text/css");
        mimes.put("jpeg", "image/jpeg");
        mimes.put("jpg", "image/jpeg");
        mimes.put("jpe", "image/jpeg");
        // determine mime type
        String mimeType = null;
        int indexOfDot = fileToRead.getName().indexOf('.');
        if (indexOfDot >= 0) {
            String extension = JDUtilities.getFileExtension(fileToRead);
            mimeType = "text/plain";
            if (mimes.containsKey(extension.toLowerCase())) mimeType = mimes.get(extension.toLowerCase());
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
                logger.info("size " + out.size());
                response.setBinaryContent(out.toByteArray());
            }
        } catch (Exception e) {
            logger.info("error reading file" + e);
            response.setError(e);
            return;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.info("failed to close stream" + e);
                }
            }
        }

        response.setOk();
        return;
    }
}
