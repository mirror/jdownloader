package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.logging.LogController;

public class FFMpegInfoReader {

    // if the call takes longer than 10 minutes. interrupt it
    protected static final long FFMPEG_EXECUTE_TIMEOUT = 10 * 60 * 1000l;
    private DownloadLink        downloadLink;
    private ArrayList<Stream>   streams;
    private String              majorBrand;

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    private LogSource logger;

    private FFProbe   probeResult;
    private Format    format;
    private File      thumb;

    public Format getFormat() {
        return format;
    }

    public ArrayList<Stream> getStreams() {
        return streams;
    }

    public FFMpegInfoReader(DownloadLink dl) {
        this.downloadLink = dl;
        logger = LogController.getInstance().getLogger(FFMpegInfoReader.class.getName());
    }

    public void load(StreamingExtension extension) throws InterruptedException, IOException {

        String id = new UniqueAlltimeID().toString();
        String streamurl = extension.createStreamUrl(id, "ffmpeg", null);
        try {
            extension.addDownloadLink(id, downloadLink);
            File ffprobe = Application.getResource("tools\\Windows\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\bin\\ffprobe.exe");

            if (ffprobe.exists()) {
                for (int myTry = 0; myTry < 3; myTry++) {
                    String[] results = execute(ffprobe.getAbsolutePath(), "-show_format", "-show_streams", "-of", "json", "-i", streamurl);
                    logger.info("Get STream Info: " + downloadLink.getDownloadURL());

                    String result = results[0];
                    String report = results[1];
                    logger.info(report);
                    logger.info(result);

                    if (result.trim().length() == 0 || report.trim().length() == 0 || report.contains("Input/output error")) {
                        // hm.. bad luck.. new try
                        continue;

                    }

                    probeResult = JSonStorage.restoreFromString(result, new TypeRef<FFProbe>() {

                    });
                    streams = probeResult.getStreams();
                    format = probeResult.getFormat();
                    break;

                }

            } else {
                logger.info("ffprobe not found at " + ffprobe);
            }

            ArrayList<Stream> streams = getStreams();
            if (streams != null) {
                for (Stream info : streams) {

                    if ("video".equals(info.getCodec_type()) && !"mjpeg".equalsIgnoreCase(info.getCodec_name())) {

                        File ffmpeg = Application.getResource("tools\\Windows\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\bin\\ffmpeg.exe");
                        if (ffmpeg.exists()) {
                            thumb = Application.getResource("tmp/streaming/thumbs/" + downloadLink.getUniqueID().toString() + ".jpg");
                            thumb.getParentFile().mkdirs();
                            thumb.delete();
                            int duration = getFormat().parseDuration();
                            int offsetInSeconds = (int) (((duration * 0.6 * Math.random())) + duration * 0.2);
                            if (offsetInSeconds < 0) offsetInSeconds = 10;
                            String[] ret = execute(ffmpeg.getAbsolutePath(), "-ss", "" + (offsetInSeconds), "-i", streamurl, "-vcodec", "mjpeg", "-vframes", "1", "-an", "-f", "rawvideo", "-s", info.getWidth() + "x" + info.getHeight(), thumb.getAbsolutePath());
                            logger.info(ret[1]);
                            System.out.println(2);
                            if (thumb.length() == 0) thumb = null;
                            break;
                        } else {
                            logger.info("FFMPeg not found at " + ffprobe);

                        }
                    }
                }
            }

            // ffmpeg -itsoffset -4 -i test.avi -vcodec mjpeg -vframes 1 -an -f rawvideo -s 320x240 test.jpg

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            extension.removeDownloadLink(id);
        }
    }

    public String getMajorBrand() {
        return majorBrand;
    }

    private String[] execute(String... cmds) throws InterruptedException, IOException {

        final ProcessBuilder pb = ProcessBuilderFactory.create(cmds);

        final StringBuilder sb = new StringBuilder();
        final StringBuilder sb2 = new StringBuilder();
        final Process process = pb.start();

        final Thread reader1 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    sb.append(readInputStreamToString(process.getInputStream()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        final Thread reader2 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    sb2.append(readInputStreamToString(process.getErrorStream()));

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        reader1.start();
        reader2.start();
        final boolean[] interrupted = new boolean[] { false };
        Thread timouter = new Thread("ffmpegReaderTimeout") {
            public void run() {

                try {
                    Thread.sleep(FFMPEG_EXECUTE_TIMEOUT);
                } catch (InterruptedException e) {
                    return;
                }
                interrupted[0] = true;
                reader1.interrupt();
                reader2.interrupt();

                process.destroy();
            }
        };
        timouter.start();
        System.out.println(process.waitFor());
        timouter.interrupt();
        if (interrupted[0]) { throw new InterruptedException("Timeout!"); }
        return new String[] { sb.toString(), sb2.toString() };

    }

    public static String readInputStreamToString(final InputStream fis) throws UnsupportedEncodingException, IOException {
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(fis, "UTF8"));
            String line;
            final StringBuilder ret = new StringBuilder();
            final String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                } else if (line.startsWith("\uFEFF")) {
                    /*
                     * Workaround for this bug: http://bugs.sun.com/view_bug.do?bug_id=4508058
                     * http://bugs.sun.com/view_bug.do?bug_id=6378911
                     */

                    line = line.substring(1);
                }
                ret.append(line);
            }

            return ret.toString();
        } catch (IOException e) {

            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } finally {
            // don't close streams this might ill the process
        }
    }

    public String getThumbnailPath() {
        return thumb == null ? null : thumb.getAbsolutePath();
    }

}
