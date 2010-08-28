package jd.network.rtmp.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import jd.network.rtmp.rtmp.LibRtmp;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class RtmpBufferedInputStream extends InputStream {

    // private final static int DEFAULT_BUFFER_SIZE = 1048576 * 3; // 3MB

    // private int bufferIndex = 0;
    // private byte[] buffer = null;
    // private int bufferSize = DEFAULT_BUFFER_SIZE;

    private LibRtmp lib = null;
    private Pointer session = null;

    public RtmpBufferedInputStream(URL rtmpUrl) {
        this(rtmpUrl, null);
    }

    public RtmpBufferedInputStream(URL rtmpUrl, int bufferSize) {
        this(rtmpUrl, null);
    }

    public RtmpBufferedInputStream(URL rtmpUrl, HashMap<String, String> parameterMap) {

        String rtmpUrlStr = rtmpUrl.toExternalForm();

        if (parameterMap != null) {
            rtmpUrlStr = rtmpUrlStr.trim() + " ";

            Iterator<String> keyIter = parameterMap.keySet().iterator();

            while (keyIter.hasNext()) {
                String key = keyIter.next();
                rtmpUrlStr += key + "=" + parameterMap.get(key) + " ";
            }
            rtmpUrlStr = rtmpUrlStr.trim();
        }

        // this.bufferSize = bufferSize;

        lib = (LibRtmp) Native.loadLibrary("rtmp", LibRtmp.class);

        session = lib.RTMP_Alloc();
        System.out.println("Alloc done");

        lib.RTMP_Init(session);
        System.out.println("Init done");

        System.out.println("URL is " + rtmpUrlStr);
        int res = lib.RTMP_SetupURL(session, rtmpUrlStr);
        System.out.println("RTMP_SetupURL=" + res);

        res = lib.RTMP_Connect(session, Pointer.NULL);
        System.out.println("RTMP_Connect=" + res + " RTMP_IsConnected=" + lib.RTMP_IsConnected(session));

        res = lib.RTMP_ConnectStream(session, 0);
        System.out.println("RTMP_ConnectStream=" + res);

        // session = RTMP_Allocate();
        // RTMP_Init(session);
        // RTMP_SetupURL(session,rtmpUrlStr)
    }

    @Override
    public void close() throws IOException {
        if (lib != null && session != null) {
            lib.RTMP_Close(session);
            lib.RTMP_Free(session);
        }
        // RTMP_Close(session);
        // RTMP_Free(session);
        super.close();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int size = lib.RTMP_Read(session, b, b.length);
        System.out.println("read " + size);
        return size;
    }

    @Override
    public int read() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    //
    // @Override
    // public int read() throws IOException
    // {
    //
    // byte retVal = 0;
    //
    // if (buffer != null && bufferIndex > buffer.length - 1)
    // {
    // buffer = null;
    // }
    //
    // if (buffer == null)
    // {
    // // TODO RTMP_READ(session,buffer,buffer.length);
    // buffer = new byte[bufferSize];
    // if ((bufferSize = lib.RTMP_Read(session, buffer, buffer.length)) <= 0)
    // {
    // return -1;
    // }
    // bufferIndex = 0;
    // System.out.println("read");
    // }
    //
    // if (bufferIndex <= bufferSize - 1)
    // {
    // retVal = buffer[bufferIndex];
    // bufferIndex++;
    // }
    //
    // return retVal;
    // }
}
