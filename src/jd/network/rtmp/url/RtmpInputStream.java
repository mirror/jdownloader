package jd.network.rtmp.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import jd.network.rtmp.rtmp.LibRtmp;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Initializes a RTMP connection and offers to read from it
 * 
 * @author mike0007
 */
public class RtmpInputStream extends InputStream {

    /** The native dynamic lib */
    private LibRtmp lib = null;
    /** Session of the rtmp connection (used by the external rtmp lib) */
    private Pointer session = null;

    /**
     * Constructor for the RTMP InputstreamStream
     * 
     * @param rtmpUrl
     *            URL to the RTMP source
     * @see de.vgrabber.rtmp.RtmpUrlConnection
     */
    public RtmpInputStream(URL rtmpUrl) {
        this(rtmpUrl, null);
    }

    /**
     * Constructor for the RTMP InputstreamStream
     * 
     * @param rtmpUrl
     *            URL to the RTMP source
     * @param parameterMap
     *            <tt>HashMap</tt> with parameter information
     * @see de.vgrabber.rtmp.RtmpUrlConnection
     */
    public RtmpInputStream(URL rtmpUrl, HashMap<String, String> parameterMap) {

        String rtmpUrlStr = rtmpUrl.toExternalForm();

        // Build the parameter String and append it to the URL
        if (parameterMap != null) {
            rtmpUrlStr = rtmpUrlStr.trim() + " ";

            Iterator<String> keyIter = parameterMap.keySet().iterator();

            while (keyIter.hasNext()) {
                String key = keyIter.next();
                rtmpUrlStr += key + "=" + parameterMap.get(key) + " ";
            }
            rtmpUrlStr = rtmpUrlStr.trim();
        }

        lib = (LibRtmp) Native.loadLibrary("rtmp", LibRtmp.class);

        // Allocate memory for an rtmp session
        session = lib.RTMP_Alloc();

        // Init the session
        lib.RTMP_Init(session);

        // setup the URL
        lib.RTMP_SetupURL(session, rtmpUrlStr);

        // Connect the session
        lib.RTMP_Connect(session, Pointer.NULL);

        // Connect the Stream
        lib.RTMP_ConnectStream(session, 0);

        // From hereon we now can read data from the stream via the read()
        // method

    }

    /**
     * Closes the <tt>RtmpInputStream</tt>
     */
    @Override
    public void close() throws IOException {
        if (lib != null && session != null) {
            // Close the session and stream
            lib.RTMP_Close(session);
            // Free memory
            lib.RTMP_Free(session);
        }
        super.close();
    }

    /**
     * Reads a <tt>byte</tt> array from the stream
     * 
     * @return Size of the read data (not equal to byte array length!) Is -1 if
     *         EOF is reached
     */
    @Override
    public int read(byte[] b) throws IOException {
        // Read data from the rtmp stream
        int size = lib.RTMP_Read(session, b, b.length);
        return size == 0 ? -1 : size;
    }

    /**
     * Reads one <tt>byte</tt> from the stream
     * 
     * @return Value of the read byte
     * @deprecated Use <tt>read(byte[] b)</tt>
     */
    @Override
    @Deprecated
    public int read() throws IOException {
        byte[] b = new byte[1];
        // Read one byte from the rtmp stream
        int size = lib.RTMP_Read(session, b, b.length);

        if (size > 0)
            return (int) b[0];
        else
            return -1;
    }

}
