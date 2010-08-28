package jd.network.rtmp.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketPermission;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.security.Permission;
import java.util.HashMap;

/**
 * <tt>UrlConnection</tt> for the rtmp protocol
 * 
 * @author mike0007
 */
public class RtmpUrlConnection extends URLConnection {

    private final static int DEFAULT_PORT = 1935;

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
    private String KEY_TOKEN = "token";

    // Network Parameters
    private String KEY_SOCKS = "socks";

    private HashMap<String, String> parameterMap = null;

    /**
     * Constructor
     * <p>
     * An example character string suitable for use with RTMP_SetupURL():
     * <p>
     * <tt>
     * "rtmp://flashserver:1935/ondemand/thefile swfUrl=http://flashserver/player.swf swfVfy=1"
     * </tt>
     * </p>
     * 
     * @param url
     *            rtmp <tt>URL</tt> to the rtmp source
     */
    public RtmpUrlConnection(URL url) {
        this(url, new HashMap<String, String>());
    }

    /**
     * Constructor
     * <p>
     * An example character string suitable for use with RTMP_SetupURL():
     * <p>
     * <tt>
     * "rtmp://flashserver:1935/ondemand/thefile swfUrl=http://flashserver/player.swf swfVfy=1"
     * </tt>
     * </p>
     * 
     * @param url
     *            rtmp <tt>URL</tt> to the rtmp source
     * @param parameterMap
     *            <tt>HashMap</tt> with parameters for the stream
     */
    public RtmpUrlConnection(URL url, HashMap<String, String> parameterMap) {
        super(url);
        this.parameterMap = parameterMap;
    }

    /**
     * Name of application to connect to on the RTMP server. Overrides the app
     * in the RTMP URL. Sometimes the librtmp URL parser cannot determine the
     * app name automatically, so it must be given explicitly using this option.
     * 
     * @param value
     */
    public void setApp(String value) {
        parameterMap.put(KEY_APP, value);
    }

    /**
     * URL of the web page in which the media was embedded. By default no value
     * will be sent.
     * 
     * @param value
     */
    public void setPageUrl(String value) {
        parameterMap.put(KEY_PAGE_URL, value);
    }

    /**
     * Append arbitrary AMF data to the Connect message. The type must be B for
     * Boolean, N for number, S for string, O for object, or Z for null. For
     * Booleans the data must be either 0 or 1 for FALSE or TRUE, respectively.
     * Likewise for Objects the data must be 0 or 1 to end or begin an object,
     * respectively. Data items in subobjects may be named, by prefixing the
     * type with 'N' and specifying the name before the value, e.g. NB:myFlag:1.
     * This option may be used multiple times to construct arbitrary AMF
     * sequences. E.g.
     * <p>
     * <tt>
     * conn=B:1 conn=S:authMe conn=O:1 conn=NN:code:1.23 conn=NS:flag:ok conn=O:0
     * </tt>
     * </p>
     * 
     * @param value
     */
    public void setConn(String value) {
        parameterMap.put(KEY_CONN, value);
    }

    public void setSocks(String value) {
        parameterMap.put(KEY_SOCKS, value);
    }

    /**
     * Version of the Flash plugin used to run the SWF player. The default is
     * "LNX 10,0,32,18".
     * 
     * @param value
     */
    public void setFlashVer(String value) {
        parameterMap.put(KEY_FLASH_VER, value);
    }

    /**
     * Specify how many days to use the cached SWF info before re-checking. Use
     * 0 to always check the SWF URL. Note that if the check shows that the SWF
     * file has the same modification timestamp as before, it will not be
     * retrieved again.
     * 
     * @param value
     */
    public void setSwfAge(String value) {
        parameterMap.put(KEY_SWF_AGE, value);
    }

    /**
     * URL of the SWF player for the media. By default no value will be sent.
     * 
     * @param value
     */
    public void setSwfUrl(String value) {
        parameterMap.put(KEY_SWF_URL, value);
    }

    /**
     * If the value is 1 or TRUE, the SWF player is retrieved from the specified
     * swfUrl for performing SWF Verification. The SWF hash and size (used in
     * the verification step) are computed automatically. Also the SWF
     * information is cached in a .swfinfo file in the user's home directory, so
     * that it doesn't need to be retrieved and recalculated every time. The
     * .swfinfo file records the SWF URL, the time it was fetched, the
     * modification timestamp of the SWF file, its size, and its hash. By
     * default, the cached info will be used for 30 days before re-checking.
     * 
     * @param value
     */
    public void setSwfVfy(boolean value) {
        parameterMap.put(KEY_SWF_VFY, (value ? "1" : "0"));
    }

    /**
     * URL of the target stream. Defaults to rtmp[t][e|s]://host[:port]/app.
     * 
     * @param value
     */
    public void setTcUrl(String value) {
        parameterMap.put(KEY_TC_URL, value);
    }

    /**
     * Set buffer time to num milliseconds. The default is 30000.
     * 
     * @param value
     */
    public void setBuffer(int value) {
        parameterMap.put(KEY_BUFFER, String.valueOf(value));
    }

    /**
     * Timeout the session after num seconds without receiving any data from the
     * server. The default is 120.
     * 
     * @param value
     */
    public void setTimeOut(int value) {
        parameterMap.put(KEY_TIMEOUT, String.valueOf(value));
    }

    /**
     * Stop at num seconds into the stream.
     * 
     * @param value
     */
    public void setStop(int value) {
        parameterMap.put(KEY_STOP, String.valueOf(value));
    }

    /**
     * Start at num seconds into the stream. Not valid for live streams.
     * 
     * @param value
     */
    public void setStart(int value) {
        parameterMap.put(KEY_START, String.valueOf(value));
    }

    /**
     * Key for SecureToken response, used if the server requires SecureToken
     * authentication.
     * 
     * @param value
     */
    public void setToken(String value) {
        parameterMap.put(KEY_TOKEN, value);
    }

    /**
     * Name of live stream to subscribe to. Defaults to playpath.
     * 
     * @param value
     */
    public void setSubscribe(String value) {
        parameterMap.put(KEY_SUBSCRIBE, value);
    }

    /**
     * Specify that the media is a live stream. No resuming or seeking in live
     * streams is possible.
     * 
     * @param value
     */
    public void setLive(boolean value) {
        parameterMap.put(KEY_LIVE, (value ? "1" : "0"));
    }

    /**
     * If the value is 1 or TRUE, issue a set_playlist command before sending
     * the play command. The playlist will just contain the current playpath. If
     * the value is 0 or FALSE, the set_playlist command will not be sent. The
     * default is FALSE.
     * 
     * @param value
     */
    public void setPlayList(boolean value) {
        parameterMap.put(KEY_PLAYLIST, (value ? "1" : "0"));
    }

    /**
     * Overrides the playpath parsed from the RTMP URL. Sometimes the rtmpdump
     * URL parser cannot determine the correct playpath automatically, so it
     * must be given explicitly using this option.
     * 
     * @param value
     */
    public void setPlayPath(String value) {
        parameterMap.put(KEY_PLAYPATH, value);
    }

    /**
     * Opens a communications link to the resource referenced by this URL, if
     * such a connection has not already been established.
     * <p>
     * If the <code>connect</code> method is called when the connection has
     * already been opened (indicated by the <code>connected</code> field having
     * the value <code>true</code>), the call is ignored.
     * <p>
     * URLConnection objects go through two phases: first they are created, then
     * they are connected. After being created, and before being connected,
     * various options can be specified (e.g., doInput and UseCaches). After
     * connecting, it is an error to try to set them. Operations that depend on
     * being connected, like getContentLength, will implicitly perform the
     * connection, if necessary.
     * 
     * @throws SocketTimeoutException
     *             if the timeout expires before the connection can be
     *             established
     * @exception IOException
     *                if an I/O error occurs while opening the connection.
     * @see java.net.URLConnection#connected
     * @see #getConnectTimeout()
     * @see #setConnectTimeout(int)
     */
    @Override
    public void connect() throws IOException {
        connected = true;
    }

    /**
     * Indicates that other requests to the server are unlikely in the near
     * future. Calling disconnect() should not imply that this RtmpURLConnection
     * instance can be reused for other requests.
     */
    public void disconnect() {
        connected = false;
    }

    /**
     * Returns the value of the <code>content-length</code>.
     * 
     * @return the content length of the resource that this connection's URL
     *         references, or <code>-1</code> if the content length is not
     *         known.
     */
    public int getContentLength() {
        // Use RTMP_GetDuration and recalculate the seconds to byte!?
        return -1;
    }

    /**
     * Returns the value of the <code>content-type</code>.
     * 
     * @return the content type of the resource that the URL references, or
     *         <code>null</code> if not known.
     */
    @Override
    public String getContentType() {
        // For now we don't know which content we have
        // For the future one of these may be right (adobe stuff)

        // File extension:
        // .flv
        // .f4v
        // .f4p
        // .f4a
        // .f4b

        // Internet media type:
        // video/x-flv
        // video/mp4
        // video/x-m4v
        // audio/mp4a-latm
        // video/3gpp
        // video/quicktime
        // audio/mp4
        return null;
    }

    /**
     * Returns an input stream that reads from this open connection. A
     * SocketTimeoutException can be thrown when reading from the returned input
     * stream if the read timeout expires before data is available for read.
     * 
     * @return an input stream that reads from this open connection.
     * @exception IOException
     *                if an I/O error occurs while creating the input stream.
     * @exception UnknownServiceException
     *                if the protocol does not support input.
     * @see #setReadTimeout(int)
     * @see #getReadTimeout()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return new RtmpInputStream(url, parameterMap);
    }

    public Permission getPermission() throws IOException {
        int port = url.getPort();
        port = port < 0 ? DEFAULT_PORT : port;
        String host = url.getHost() + ":" + port;
        Permission permission = new SocketPermission(host, "connect");
        return permission;
    }

}
