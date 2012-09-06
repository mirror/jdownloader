package org.jdownloader.extensions.streaming;

import jd.parser.Regex;

import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.GetRequest;

public class ByteRange {

    private long start;

    public long getStart() {
        return start < 0 ? 0 : start;
    }

    public long getEnd() {
        return end;
    }

    public boolean isValid() {
        return start >= 0;
    }

    private long end;

    public ByteRange(GetRequest request) {
        final HTTPHeader rangeRequest = request.getRequestHeaders().get("Range");
        start = -1;
        end = -1;
        if (rangeRequest != null) {
            String startString = new Regex(rangeRequest.getValue(), "(\\d+).*?-").getMatch(0);
            String stopString = new Regex(rangeRequest.getValue(), "-.*?(\\d+)").getMatch(0);
            if (startString != null) start = Long.parseLong(startString);
            if (stopString != null) end = Long.parseLong(stopString);
        }

    }

    public boolean isOpenEnd() {
        return end < 0;
    }

}
