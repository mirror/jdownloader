package jd.network.rtmp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import jd.network.rtmp.url.CustomUrlStreamHandlerFactory;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;

public class RtmpDump2 {

    static {
        try {
            if (CrossSystem.isWindows()) {
                System.load(JDUtilities.getResourceFile("libs/rtmp.dll").getAbsolutePath());
            } else if (CrossSystem.isLinux()) {
                System.load(JDUtilities.getResourceFile("libs/librtmp.so").getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    public static void main(String[] args) {

        try {
            URL.setURLStreamHandlerFactory(new CustomUrlStreamHandlerFactory());

            URL rtmp = new URL("rtmpe://fms-fra9.rtl.de/rtlnow/248/V_52072_BBDB_m07007_45471_16x9-lq-512x288-h264-c0_c2e7538eba78393f4baf1635e93140e6.f4v");

            RtmpUrlConnection conn = (RtmpUrlConnection) rtmp.openConnection();

            conn.setApp("rtlnow/_definst_");
            conn.setTcUrl("rtmp://fms-hc1.rtl.de/rtlnow/_definst_");
            conn.setPageUrl("http://rtl-now.rtl.de/");
            conn.setSwfUrl("http://rtl-now.rtl.de/includes/rtlnow_videoplayer09_2.swf?ts=20090922");
            conn.setSwfVfy(true);
            conn.setPlayPath("mp4:/248/V_52072_BBDB_m07007_45471_16x9-lq-512x288-h264-c0_c2e7538eba78393f4baf1635e93140e6.f4v");

            conn.connect();

            InputStream in = conn.getInputStream();

            byte[] puffer = new byte[1024 * 1024];
            FileOutputStream fos = new FileOutputStream(new File(".\\test.flv"));

            // fos.write(LibRtmp.flvHeader);
            int size = 0;
            while ((size = in.read(puffer)) > 0) {
                // System.out.println("reading data " + puffer[0]);
                fos.write(puffer, 0, size);

            }
            fos.close();

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        // int res = lib
        // .RTMP_SetupURL(
        // session,
        // "rtmpe://fms-fra9.rtl.de/rtlnow/248/V_52072_BBDB_m07007_45471_16x9-lq-512x288-h264-c0_c2e7538eba78393f4baf1635e93140e6.f4v"
        // + " tcUrl=rtmp://fms-hc1.rtl.de/rtlnow/_definst_"
        // + " pageUrl=http://rtl-now.rtl.de/"
        // +
        // " swfUrl=http://rtl-now.rtl.de/includes/rtlnow_videoplayer09_2.swf?ts=20090922"
        // + " swfVfy=1"
        // + " app=rtlnow/_definst_"
        // +
        // " playpath=mp4:/248/V_52072_BBDB_m07007_45471_16x9-lq-512x288-h264-c0_c2e7538eba78393f4baf1635e93140e6.f4v");

    }
}
