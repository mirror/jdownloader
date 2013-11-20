package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.logging.LogController;

public class FFMpegInfoReader {
    /*
     * Have a static image displayed while a song plays.
     * 
     * $ ffmpeg -i audio.mp3 -loop_input -i albumcover.jpg -vcodec libx264 -preset slow -crf 20 -threads 0 -acodec copy -shortest output.mkv
     */
    // if the call takes longer than 10 minutes. interrupt it
    protected static final long FFMPEG_EXECUTE_TIMEOUT = 10 * 60 * 1000l;
    private MediaItem           mediaItem;

    public MediaItem getMediaItem() {
        return mediaItem;
    }

    private ArrayList<Stream> streams;

    private LogSource         logger;

    private FFProbe           probeResult;
    private Format            format;
    private File              thumb;
    private String            result;

    public Format getFormat() {
        return format;
    }

    public ArrayList<Stream> getStreams() {
        return streams;
    }

    public FFMpegInfoReader(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
        logger = LogController.getInstance().getLogger(FFMpegInfoReader.class.getName());
    }

    public void load(StreamingExtension extension) throws Exception {

        String id = new UniqueAlltimeID().toString();
        String streamurl = extension.createStreamUrl(id, "ffmpeg", null, null);
        try {
            extension.linkMediaItem(id, mediaItem);
            String path = getFFProbePath();
            if (path != null) {
                String report = null;

                String[] results = execute(path, "-show_format", "-show_streams", "-probesize", "10000000", "-of", "json", "-i", streamurl);
                logger.info("Get STream Info: " + mediaItem);

                result = results[0];
                report = results[1];
                logger.info(report);
                logger.info(result);

                isError(results[0], results[1]);

                probeResult = JSonStorage.restoreFromString(result, new TypeRef<FFProbe>() {

                });
                streams = probeResult.getStreams();
                format = probeResult.getFormat();

            } else {
                logger.info("ffprobe not found at " + path);
            }

            if ("mp3".equals(getFormat().getFormat_name())) {
                String ffmpeg = getFFMpegPath();
                if (ffmpeg != null) {
                    thumb = Application.getResource("tmp/streaming/cover/" + mediaItem.getUniqueID().toString() + ".jpg");
                    FileCreationManager.getInstance().mkdir(thumb.getParentFile());
                    FileCreationManager.getInstance().delete(thumb, null);
                    String[] ret = execute(ffmpeg, "-i", streamurl, "-c", "copy", thumb.getAbsolutePath());
                    logger.info(ret[1]);
                    System.out.println(2);
                    if (thumb.length() == 0) thumb = null;

                }
            } else {
                ArrayList<Stream> streams = getStreams();
                if (streams != null) {

                    for (Stream info : streams) {

                        if ("video".equals(info.getCodec_type()) && !"mjpeg".equalsIgnoreCase(info.getCodec_name())) {

                            String ffmpeg = getFFMpegPath();
                            if (ffmpeg != null) {
                                thumb = Application.getResource("tmp/streaming/thumbs/" + mediaItem.getUniqueID().toString() + ".jpg");
                                FileCreationManager.getInstance().mkdir(thumb.getParentFile());
                                FileCreationManager.getInstance().delete(thumb, null);
                                long duration = getFormat().parseDuration() / 1000;
                                int offsetInSeconds = (int) (((duration * 0.6 * Math.random())) + duration * 0.2);
                                if (offsetInSeconds < 0) offsetInSeconds = 10;
                                String[] ret = execute(ffmpeg, "-ss", "" + (offsetInSeconds), "-i", streamurl, "-vcodec", "mjpeg", "-vframes", "1", "-an", "-f", "rawvideo", "-s", info.getWidth() + "x" + info.getHeight(), thumb.getAbsolutePath());
                                logger.info(ret[1]);
                                System.out.println(2);

                                isError(null, ret[1]);

                                if (thumb.length() == 0) thumb = null;
                                break;
                            } else {
                                logger.info("FFMpeg not found at " + ffmpeg);

                            }
                        }

                    }
                }
            }

        } finally {
            extension.unlinkMediaItem(id);
        }
    }

    private void isError(String result, String report) throws Exception {
        if (result != null && result.trim().length() == 0) throw new IOException("No Result");
        if (report.contains("Input/output error")) throw new IOException(report);
        if (report.contains("Invalid data found when processing input")) throw new IOException(report);
    }

    public String getResult() {
        return result;
    }

    private String getFFMpegPath() {

        if (CrossSystem.isWindows()) {
            File ffprobe = Application.getResource("tools\\Windows\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\bin\\ffmpeg.exe");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        } else if (CrossSystem.isLinux()) {
            File ffprobe = Application.getResource("tools\\linux\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\ffmpeg");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        } else if (CrossSystem.isMac()) {
            File ffprobe = Application.getResource("tools\\mac\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\ffmpeg");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        }
        return "ffmpeg";
    }

    private String getFFProbePath() {
        if (CrossSystem.isWindows()) {
            File ffprobe = Application.getResource("tools\\Windows\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\bin\\ffprobe.exe");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        } else if (CrossSystem.isLinux()) {
            File ffprobe = Application.getResource("tools\\linux\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\ffprobe");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        } else if (CrossSystem.isMac()) {
            File ffprobe = Application.getResource("tools\\mac\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\ffprobe");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        }
        return "ffprobe";

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
