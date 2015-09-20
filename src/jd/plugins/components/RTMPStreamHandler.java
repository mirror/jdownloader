package jd.plugins.components;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import jd.network.rtmp.url.RtmpUrlConnection;

public class RTMPStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new RtmpUrlConnection(url);
    }

}
