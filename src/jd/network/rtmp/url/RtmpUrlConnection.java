package jd.network.rtmp.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

public class RtmpUrlConnection extends URLConnection {

    private URL url = null;
    public final static int DEFAULT_PORT = 1935;

    // Connection Parameters
    private String KEY_APP = "app";
    private String KEY_TC_URL = "tcUrl";
    private String KEY_PAGE_URL = "pageUrl";
    private String KEY_FLASH_VER = "flashVer";
    private String KEY_SWF_URL = "swfUrl";
    private String KEY_CONN = "conn";

    // Session Parameters
    private String KEY_PLAYPATH = "playpath";
    private String KEY_PLAYLIST = "playlist";
    private String KEY_LIVE = "live";
    private String KEY_SUBSCRIBE = "subscribe";
    private String KEY_START = "start";
    private String KEY_STOP = "stop";
    private String KEY_BUFFER = "buffer";
    private String KEY_TIMEOUT = "timeout";

    // Security Parameters
    private String KEY_SWF_VFY = "swfVfy";
    private String KEY_SWF_AGE = "swfAge";

    // Network Parameters
    private String KEY_SOCKS = "socks";

    private HashMap<String, String> parameterMap = null;

    protected RtmpUrlConnection(URL url) {
        super(url);
        this.url = url;
        this.parameterMap = new HashMap<String, String>();
    }

    public void setPlayPath(String value) {
        parameterMap.put(KEY_PLAYPATH, value);
    }

    public void setApp(String value) {
        parameterMap.put(KEY_APP, value);
    }

    public void setPageUrl(String value) {
        parameterMap.put(KEY_PAGE_URL, value);
    }

    public void setConn(String value) {
        parameterMap.put(KEY_CONN, value);
    }

    public void setSocks(String value) {
        parameterMap.put(KEY_SOCKS, value);
    }

    public void setFlashVer(String value) {
        parameterMap.put(KEY_FLASH_VER, value);
    }

    public void setSwfAge(String value) {
        parameterMap.put(KEY_SWF_AGE, value);
    }

    public void setSwfUrl(String value) {
        parameterMap.put(KEY_SWF_URL, value);
    }

    public void setSwfVfy(boolean value) {
        parameterMap.put(KEY_SWF_VFY, (value ? "1" : "0"));
    }

    public void setTcUrl(String value) {
        parameterMap.put(KEY_TC_URL, value);
    }

    @Override
    public void connect() throws IOException {
        connected = true;
    }

    public void disconnect() {
        connected = false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new RtmpBufferedInputStream(url, parameterMap);
    }
}
