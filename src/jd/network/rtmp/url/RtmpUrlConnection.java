package jd.network.rtmp.url;

import java.io.IOException;
import java.net.SocketPermission;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.HashMap;
import java.util.Iterator;

/**
 * <tt>UrlConnection</tt> for the rtmp protocol
 * 
 * @author mike0007
 * @author bismarck
 */
public class RtmpUrlConnection extends URLConnection {

    private final static int          DEFAULT_PORT  = 1935;

    // Connection Parameters
    private static final String       KEY_APP       = "a"; // app
    private static final String       KEY_TC_URL    = "t"; // tcUrl
    private static final String       KEY_PAGE_URL  = "p"; // pageUrl
    private static final String       KEY_FLASH_VER = "f"; // flashVer
    private static final String       KEY_SWF_URL   = "s"; // swfUrl
    private static final String       KEY_CONN      = "C"; // conn
    private static final String       KEY_RTMP      = "r"; // Url
    private static final String       KEY_PORT      = "c"; // Port
    private static final String       KEY_PROTOCOL  = "l"; // protocol

    // Session Parameters
    private static final String       KEY_PLAYPATH  = "y"; // playpath
    private static final String       KEY_LIVE      = "v"; // live
    private static final String       KEY_SUBSCRIBE = "d"; // subscribe
    private static final String       KEY_REALTIME  = "R"; // realtime
    private static final String       KEY_START     = "A"; // start
    private static final String       KEY_STOP      = "B"; // stop
    private static final String       KEY_BUFFER    = "b"; // buffer
    private static final String       KEY_TIMEOUT   = "m"; // timeout
    private static final String       KEY_RESUME    = "e"; // resume

    // Security Parameters
    private static final String       KEY_SWF_VFY   = "W"; // swfVfy
    private static final String       KEY_SWF_AGE   = "X"; // swfAge"
    private static final String       KEY_TOKEN     = "T"; // token

    // Network Parameters
    private static final String       KEY_SOCKS     = "S"; // socks

    // Debug Parameters
    private static final String       KEY_DEBUG     = "z"; // debug
    private static final String       KEY_VERBOSE   = "V"; // verbose

    protected HashMap<String, String> parameterMap  = null;

    /**
     * Constructor
     * <p>
     * An example character string suitable for use with RtmpDump:
     * <p>
     * <tt>
     * "rtmp://flashserver:1935/ondemand/___the_file___"
     * </tt>
     * </p>
     * <p>
     * <tt>
     * "<----------Host--------><--App--><--Playpath-->"
     * </tt>
     * </p>
     * <p>
     * <tt>
     * "<--------------TcUrl------------>"
     * </tt>
     * </p>
     * 
     * @param url
     *            rtmp <tt>URL</tt> to the rtmp source
     */
    public RtmpUrlConnection(final URL url) {
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
    public RtmpUrlConnection(final URL url, final HashMap<String, String> parameterMap) {
        super(url);
        this.parameterMap = parameterMap;
    }

    /**
     * Opens a communications link to the resource referenced by this URL, if such a connection has not already been established.
     * <p>
     * If the <code>connect</code> method is called when the connection has already been opened (indicated by the <code>connected</code>
     * field having the value <code>true</code>), the call is ignored.
     * <p>
     * URLConnection objects go through two phases: first they are created, then they are connected. After being created, and before being
     * connected, various options can be specified (e.g., doInput and UseCaches). After connecting, it is an error to try to set them.
     * Operations that depend on being connected, like getContentLength, will implicitly perform the connection, if necessary.
     * 
     * @throws SocketTimeoutException
     *             if the timeout expires before the connection can be established
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
     * Indicates that other requests to the server are unlikely in the near future. Calling disconnect() should not imply that this
     * RtmpURLConnection instance can be reused for other requests.
     */
    public void disconnect() {
        connected = false;
    }

    /**
     * Create command line parameters.
     */
    public String getCommandLineParameter() {
        final StringBuilder cmdargs = new StringBuilder("");
        if (parameterMap != null) {
            final Iterator<String> keyIter = parameterMap.keySet().iterator();
            while (keyIter.hasNext()) {
                final String key = keyIter.next();
                cmdargs.append(" -").append(key);
                if (parameterMap.get(key) != null) {
                    cmdargs.append(" \"").append(parameterMap.get(key)).append("\"");
                }
            }
        }
        return cmdargs.toString();
    }

    /**
     * Returns the value of the <code>content-length</code>.
     * 
     * @return the content length of the resource that this connection's URL references, or <code>-1</code> if the content length is not
     *         known.
     */
    @Override
    public int getContentLength() {
        // Use RTMP_GetDuration and recalculate the seconds to byte!?
        return -1;
    }

    /**
     * Returns the value of the <code>content-type</code>.
     * 
     * @return the content type of the resource that the URL references, or <code>null</code> if not known.
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

    @Override
    public Permission getPermission() throws IOException {
        int port = url.getPort();
        port = port < 0 ? DEFAULT_PORT : port;
        final String host = url.getHost() + ":" + port;
        final Permission permission = new SocketPermission(host, "connect");
        return permission;
    }

    /**
     * Name of application to connect to on the RTMP server. Overrides the app in the RTMP URL. Sometimes the librtmp URL parser cannot
     * determine the app name automatically, so it must be given explicitly using this option.
     * 
     * @param value
     */
    public void setApp(final String value) {
        parameterMap.put(KEY_APP, value);
    }

    /**
     * Set buffer time to num milliseconds. The default is 36000000.
     * 
     * @param value
     */
    public void setBuffer(final int value) {
        parameterMap.put(KEY_BUFFER, String.valueOf(value));
    }

    /**
     * Append arbitrary AMF data to the Connect message. The type must be B for Boolean, N for number, S for string, O for object, or Z for
     * null. For Booleans the data must be either 0 or 1 for FALSE or TRUE, respectively. Likewise for Objects the data must be 0 or 1 to
     * end or begin an object, respectively. Data items in subobjects may be named, by prefixing the type with 'N' and specifying the name
     * before the value, e.g. NB:myFlag:1. This option may be used multiple times to construct arbitrary AMF sequences. E.g.
     * <p>
     * <tt>
     * conn=B:1 conn=S:authMe conn=O:1 conn=NN:code:1.23 conn=NS:flag:ok conn=O:0
     * </tt>
     * </p>
     * 
     * @param value
     */
    public void setConn(final String value) {
        parameterMap.put(KEY_CONN, value);
    }

    /**
     * Debug level command output.
     * 
     */
    public void setDebug() {
        parameterMap.put(KEY_DEBUG, null);
    }

    /**
     * Version of the Flash plugin used to run the SWF player. The default is "LNX 10,0,32,18".
     * 
     * @param value
     */
    public void setFlashVer(final String value) {
        parameterMap.put(KEY_FLASH_VER, value);
    }

    /**
     * Specify that the media is a live stream. No resuming or seeking in live streams is possible.
     * 
     * @param value
     */
    public void setLive(final boolean value) {
        parameterMap.put(KEY_LIVE, (value ? "1" : "0"));
    }

    /**
     * URL of the web page in which the media was embedded. By default no value will be sent.
     * 
     * @param value
     */
    public void setPageUrl(final String value) {
        parameterMap.put(KEY_PAGE_URL, value);
    }

    /**
     * Overrides the playpath parsed from the RTMP URL. Sometimes the rtmpdump URL parser cannot determine the correct playpath
     * automatically, so it must be given explicitly using this option.
     * 
     * @param value
     */
    public void setPlayPath(final String value) {
        parameterMap.put(KEY_PLAYPATH, value);
    }

    /**
     * Overrides the port in the rtmp url.
     * 
     * @param value
     */
    public void setPort(final int value) {
        parameterMap.put(KEY_PORT, String.valueOf(value));
    }

    /**
     * Overrides the protocol in the rtmp url.
     * 
     * @param value
     *            (0 - RTMP, 3 - RTMPE)
     */
    public void setProtocol(final int value) {
        if (value == 0 || value == 3) {
            parameterMap.put(KEY_PROTOCOL, String.valueOf(value));
        }
    }

    /**
     * Resume an incomplete RTMP download.
     * 
     * @param boolean
     */
    public void setResume(final boolean value) {
        if (value) {
            parameterMap.put(KEY_RESUME, null);
        }
    }

    /**
     * Use the specified SOCKS proxy.
     * 
     * @param value
     *            host:port
     */
    public void setSocks(final String value) {
        parameterMap.put(KEY_SOCKS, value);
    }

    /**
     * Start at num seconds into the stream. Not valid for live streams.
     * 
     * @param value
     */
    public void setStart(final int value) {
        parameterMap.put(KEY_START, String.valueOf(value));
    }

    /**
     * Stop at num seconds into the stream.
     * 
     * @param value
     */
    public void setStop(final int value) {
        parameterMap.put(KEY_STOP, String.valueOf(value));
    }

    /**
     * Name of live stream to subscribe to. Defaults to playpath.
     * 
     * @param value
     */
    public void setSubscribe(final String value) {
        parameterMap.put(KEY_SUBSCRIBE, value);
    }

    /**
     * Don't attempt to speed up download via the Pause/Unpause BUFX hack
     * 
     * @param value
     */
    public void setRealTime() {
        parameterMap.put(KEY_REALTIME, null);
    }

    /**
     * Specify how many days to use the cached SWF info before re-checking. Use 0 to always check the SWF URL. Note that if the check shows
     * that the SWF file has the same modification timestamp as before, it will not be retrieved again.
     * 
     * @param value
     */
    public void setSwfAge(final String value) {
        parameterMap.put(KEY_SWF_AGE, value);
    }

    /**
     * URL of the SWF player for the media. By default no value will be sent.
     * 
     * @param value
     */
    public void setSwfUrl(final String value) {
        parameterMap.put(KEY_SWF_URL, value);
    }

    /**
     * URL to player swf file, compute hash/size automatically.
     * 
     * @param value
     */
    public void setSwfVfy(final String value) {
        parameterMap.put(KEY_SWF_VFY, value);
    }

    /**
     * URL of the target stream. Defaults to rtmp[t][e|s]://host[:port]/app.
     * 
     * @param value
     */
    public void setTcUrl(final String value) {
        parameterMap.put(KEY_TC_URL, value);
    }

    /**
     * Timeout the session after num seconds without receiving any data from the server. The default is 120.
     * 
     * @param value
     */
    public void setTimeOut(final int value) {
        parameterMap.put(KEY_TIMEOUT, String.valueOf(value));
    }

    /**
     * Key for SecureToken response, used if the server requires SecureToken authentication.
     * 
     * @param value
     */
    public void setToken(final String value) {
        parameterMap.put(KEY_TOKEN, value);
    }

    /**
     * URL (e.g. rtmp[t][e|s]://hostname[:port]/app/).
     * 
     * @param value
     */
    public void setUrl(final String value) {
        parameterMap.put(KEY_RTMP, value);
    }

    /**
     * Verbose command output.
     * 
     */
    public void setVerbose() {
        parameterMap.put(KEY_VERBOSE, null);
    }
}
