package org.jdownloader.extensions.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jd.parser.Regex;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.utils.net.ChunkedOutputStream;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;

public class StreamLinker {
    private HttpResponse response;
    private GetRequest   request;

    public StreamLinker(HttpResponse response, GetRequest request) {
        this.response = response;
        this.request = request;
    }

    public void run(StreamingInterface streamingInterface) throws IOException {

        InputStream sis = null;
        try {
            final HTTPHeader rangeRequest = request.getRequestHeaders().get("Range");
            long startPosition = 0;
            long stopPosition = -1;
            if (rangeRequest != null) {
                String start = new Regex(rangeRequest.getValue(), "(\\d+).*?-").getMatch(0);
                String stop = new Regex(rangeRequest.getValue(), "-.*?(\\d+)").getMatch(0);
                if (start != null) startPosition = Long.parseLong(start);
                if (stop != null) stopPosition = Long.parseLong(stop);
            }
            if (rangeRequest != null && streamingInterface.isRangeRequestSupported() == false) {
                response.setResponseCode(ResponseCode.ERROR_RANGE_NOT_SUPPORTED);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_RANGES, "none"));
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, "0"));
                return;
            }
            sis = streamingInterface.getInputStream(startPosition, stopPosition);
            long completeSize = streamingInterface.getFinalFileSize();

            if (sis == null) {
                response.setResponseCode(ResponseCode.ERROR_NOT_FOUND);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, "0"));

            } else {
                if (response.getResponseHeaders().get(HTTPConstants.HEADER_REQUEST_ACCEPT_RANGES) == null) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_RANGES, "bytes"));
                if (rangeRequest == null) {
                    response.setResponseCode(ResponseCode.SUCCESS_OK);
                    if (completeSize != -1) {
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, completeSize + ""));
                    } else {
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING, HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED));
                    }
                } else {
                    response.setResponseCode(ResponseCode.SUCCESS_PARTIAL_CONTENT);
                    if (stopPosition == -1) {
                        response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + startPosition + "-" + (completeSize - 1) + "/" + (completeSize >= 0 ? (completeSize) : "*")));
                    } else {
                        response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + startPosition + "-" + (stopPosition - 1) + "/" + (completeSize >= 0 ? (completeSize) : "*")));
                    }
                    if (completeSize != -1) {
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, completeSize + ""));
                    } else {
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING, HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED));
                    }
                }

                OutputStream ops = response.getOutputStream();
                if (completeSize == -1) {
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
        byte[] buffer = new byte[100 * 1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

}
