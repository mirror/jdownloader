package jd.plugins.hoster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;

import jd.network.rtmp.url.CustomUrlStreamHandlerFactory;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;

/**
 * This is a wrapper for RTMP
 * 
 * @author thomas
 * 
 */
public class RTMPDownload extends RAFDownload {
    static {
        URL.setURLStreamHandlerFactory(new CustomUrlStreamHandlerFactory());

        try {
            /* these libs are 32bit */
            if (CrossSystem.isWindows()) {
                System.load(JDUtilities.getResourceFile("libs/rtmp.dll").getAbsolutePath());
            } else if (CrossSystem.isLinux()) {
                System.load(JDUtilities.getResourceFile("libs/librtmp.so").getAbsolutePath());
            }
        } catch (Throwable e) {
            System.out.println("Error loading 32bit: " + e);
            if (CrossSystem.isLinux()) {
                /* on linux we try to load the *maybe installed* native lib */
                try {
                    System.load("/usr/lib/librtmp.so");
                } catch (Throwable e2) {
                    System.out.println("Error loading /usr/lib/librtmp.so: " + e);
                }
            }
        }

    }
    private Chunk             chunk;
    private long              speed = 0l;
    private URL               url;
    private RtmpUrlConnection rtmpConnection;

    public RtmpUrlConnection getRtmpConnection() {
        return rtmpConnection;
    }

    public RTMPDownload(PluginForHost plugin, DownloadLink downloadLink, String rtmpURL) throws IOException, PluginException {
        super(plugin, downloadLink, null);
        // TODO Auto-generated constructor stub
        url = new URL(rtmpURL);
        rtmpConnection = (RtmpUrlConnection) url.openConnection();

        setResume(false);

        downloadLink.setDownloadInstance(this);

    }

    public boolean startDownload() throws Exception {
        try {
            addChunksDownloading(1);
            chunk = new Chunk(0, 0, null, null) {

                @Override
                public long getSpeed() {
                    return speed;
                }
            };
            chunk.setInProgress(true);
            getChunks().add(chunk);
            downloadLink.getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);

            rtmpConnection.connect();

            InputStream in = rtmpConnection.getInputStream();

            byte[] puffer = new byte[1024 * 1024];
            File tmp = new File(downloadLink.getFileOutput() + ".part");
            FileOutputStream fos = new FileOutputStream(tmp);

            // fos.write(LibRtmp.flvHeader);

            int size = 0;
            long before = 0;

            long lastTime = System.currentTimeMillis();
            long bytesLoaded = 0l;
            try {
                // 0 byte means eof
                while ((size = in.read(puffer)) > 0) {
                    fos.write(puffer, 0, size);
                    bytesLoaded += size;
                    if (Thread.currentThread().isInterrupted()) { throw new InterruptedIOException(); }

                    downloadLink.setDownloadCurrent(bytesLoaded);
                    // downloadLink.setDownloadSize(Math.max(downloadLink.getDownloadSize(),
                    // bytesLoaded));
                    if (System.currentTimeMillis() - lastTime > 1000) {
                        speed = ((bytesLoaded - before) / (System.currentTimeMillis() - lastTime)) * 1000l;
                        lastTime = System.currentTimeMillis();
                        before = bytesLoaded;
                        downloadLink.requestGuiUpdate();
                        downloadLink.setChunksProgress(new long[] { bytesLoaded });
                    }

                }
            } finally {
                fos.close();
                rtmpConnection.disconnect();
                in.close();
            }
            downloadLink.setDownloadSize(bytesLoaded);
            if (!tmp.renameTo(new File(downloadLink.getFileOutput()))) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, " Rename failed. file exists?"); }
            downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
            return true;
        } finally {
            downloadLink.getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            downloadLink.setDownloadInstance(null);
            downloadLink.getLinkStatus().setStatusText(null);
            chunk.setInProgress(false);
        }
    }

}
