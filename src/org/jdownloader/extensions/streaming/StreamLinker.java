package org.jdownloader.extensions.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.utils.net.ChunkedOutputStream;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.extensions.streaming.dataprovider.StreamFactoryInterface;

public class StreamLinker {
    private HttpResponse response;

    public StreamLinker(HttpResponse response) {
        this.response = response;

    }

    public void run(StreamFactoryInterface streamfactory, ByteRange range) throws IOException {

        InputStream sis = null;
        try {

            sis = streamfactory.getInputStream(range.getStart(), range.getEnd());
            long completeSize = streamfactory.getContentLength();
            long guaranteedContentLength = streamfactory.getGuaranteedContentLength();
            if (sis == null) {
                response.setResponseCode(ResponseCode.ERROR_NOT_FOUND);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, "0"));

            } else {
                // TODO: 416 Error Handling
                boolean chunked = false;
                if (response.getResponseHeaders().get(HTTPConstants.HEADER_REQUEST_ACCEPT_RANGES) == null) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_RANGES, "bytes"));
                if (!range.isValid()) {
                    response.setResponseCode(ResponseCode.SUCCESS_OK);
                    if (completeSize != -1) {
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, completeSize + ""));
                    } else {

                        chunked = true;
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING, HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED));
                    }
                } else {
                    response.setResponseCode(ResponseCode.SUCCESS_PARTIAL_CONTENT);
                    if (range.isOpenEnd()) {
                        if (completeSize > 0) {
                            response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + range.getStart() + "-" + (completeSize - 1) + "/" + completeSize));
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, (completeSize - range.getStart()) + ""));
                        } else {
                            response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + range.getStart() + "-" + (guaranteedContentLength - 1) + "/*"));
                            // response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH,
                            // (guaranteedContentLength - range.getStart()) + ""));
                            chunked = true;
                        }
                    } else {
                        if (completeSize > 0) {
                            response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + range.getStart() + "-" + (range.getEnd()) + "/" + completeSize));
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, (range.getEnd() - range.getStart() + 1) + ""));
                        } else {
                            response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + range.getStart() + "-" + (range.getEnd()) + "/" + completeSize));
                            // response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, (range.getEnd()
                            // - range.getStart() + 1) + ""));
                            chunked = true;
                        }
                    }

                }

                OutputStream ops = response.getOutputStream(true);
                if (chunked) {
                    ops = new ChunkedOutputStream(ops);
                }
                copyStream(sis, ops);

            }

        } finally {

            try {
                sis.close();
            } catch (final Throwable e) {
            }

        }

    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[500 * 1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

}
