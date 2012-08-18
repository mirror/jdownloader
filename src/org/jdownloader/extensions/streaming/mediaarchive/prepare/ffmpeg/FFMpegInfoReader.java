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

    private DownloadLink      downloadLink;
    private ArrayList<Stream> streams;
    private String            majorBrand;

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    private LogSource logger;

    private FFProbe   probeResult;
    private Format    format;

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
        String streamurl = extension.createStreamUrl(id, "ffmpeg");
        try {
            extension.addDownloadLink(id, downloadLink);
            File ffprobe = Application.getResource("tools\\Windows\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\bin\\ffprobe.exe");
            if (ffprobe.exists()) {
                for (int myTry = 0; myTry < 3; myTry++) {
                    String[] results = execute(ffprobe.getAbsolutePath(), "-pretty", "-show_format", "-show_streams", "-of", "json", "-i", streamurl);
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
                logger.info("FFMPeg not found at " + ffprobe);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            extension.removeDownloadLink(id);
        }
    }

    private long parseBitrate(String[] row) {
        try {
            // not sure if 1024 or 1000 faktor
            if (row[1].contains("mb")) {
                return Long.parseLong(row[0]) * 1000 * 1000;
            } else if (row[1].contains("kb")) {
                //
                return Long.parseLong(row[0]) * 1000;
            }
            return Long.parseLong(row[0]);
        } catch (Throwable e) {
            e.printStackTrace();
            return -1;
        }
    }

    private long parseDuration(String match) {
        try {

            String[] split = match.split("[\\:\\.]");
            int ret = 0;
            // hours
            ret += Integer.parseInt(split[0]) * 60 * 60;
            // minutes
            ret += Integer.parseInt(split[1]) * 60;
            // seconds
            ret += Integer.parseInt(split[2]);

            // ignore ms

            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
            return -1;
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
        new Thread("ffmpegReader") {
            public void run() {
                try {
                    sb.append(readInputStreamToString(process.getInputStream()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread("ffmpegReader") {
            public void run() {
                try {
                    sb2.append(readInputStreamToString(process.getErrorStream()));

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        System.out.println(process.waitFor());

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
                     * Workaround for this bug:
                     * http://bugs.sun.com/view_bug.do?bug_id=4508058
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
}
