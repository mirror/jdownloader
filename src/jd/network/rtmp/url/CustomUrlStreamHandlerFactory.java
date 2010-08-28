package jd.network.rtmp.url;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class CustomUrlStreamHandlerFactory implements URLStreamHandlerFactory {

    public final static String URL_PROTOCOL_RTMP = "rtmp";
    public final static String URL_PROTOCOL_RTMPT = "rtmpt";

    public final static String URL_PROTOCOL_RTMPS = "rtmps";
    public final static String URL_PROTOCOL_RTMPE = "rtmpe";
    public final static String URL_PROTOCOL_RTMPTE = "rtmpte";

    public final static String URL_PROTOCOL_RTMFP = "rtmfp";

    public URLStreamHandler createURLStreamHandler(String protocol) {
        System.out.println("RtmpUrlStreamHandlerFactory # protocol: " + protocol);

        if (URL_PROTOCOL_RTMP.equalsIgnoreCase(protocol)) { return new RtmpUrlStreamHandler(); }
        return null;
    }
}
