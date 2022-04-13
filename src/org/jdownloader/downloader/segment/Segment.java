package org.jdownloader.downloader.segment;

import java.io.IOException;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.plugins.download.raf.ChunkRange;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.net.HTTPHeader;

public class Segment {
    protected final String url;

    public boolean isLoaded() {
        return chunkRange.isValidLoaded();
    }

    private final ChunkRange chunkRange;

    public String getUrl() {
        return url;
    }

    public Segment(String url) {
        this(url, null, null);
    }

    public Segment(String url, Long range_from, Long range_to) {
        this.url = url;
        chunkRange = range_from != null ? new ChunkRange(range_from.longValue(), range_to) : new ChunkRange();
    }

    public ChunkRange getChunkRange() {
        return chunkRange;
    }

    protected Request createRequest() throws IOException {
        final GetRequest ret = new GetRequest(getUrl());
        ret.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity", false));
        final ChunkRange chunkRange = getChunkRange();
        if (chunkRange != null && chunkRange.isRangeRequested()) {
            final String requestedRange = chunkRange.getRangeHeaderContent(false);
            ret.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_RANGE, requestedRange, false));
        }
        return ret;
    }

    protected URLConnectionAdapter open(Browser br, Request request) throws IOException {
        br.setRequest(null);
        final URLConnectionAdapter ret = br.openRequestConnection(request);
        return ret;
    }

    protected boolean isConnectionValid(URLConnectionAdapter con) throws IOException {
        // TODO: add chunk range verification
        return con.getResponseCode() == 200 || con.getResponseCode() == 206;
    }

    protected Boolean retrySegmentConnection(Browser br, Segment segment, int segmentRetryCounter) {
        return null;
    }

    @Override
    public String toString() {
        final ChunkRange chunkRange = getChunkRange();
        return "URL:" + getUrl() + "|" + chunkRange;
    }
}
