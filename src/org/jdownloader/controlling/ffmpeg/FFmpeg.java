package org.jdownloader.controlling.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.logging.LogController;

public class FFmpeg {
    public static void main(String[] args) throws InterruptedException, IOException {
        Application.setApplication(".jd_home");
        FFmpeg f = new FFmpeg();
        if (!f.validateBinary()) { throw new WTFException("FFmpeg Binary missing"); }
        System.out.println(f.getVersion());

        System.out.println("ok");
    }

    private String getVersion() throws InterruptedException, IOException {
        String[] ret = execute(5000, getFullPath(), "-version");
        String version = new Regex(ret[0], "ffmpeg version N-(\\d+)").getMatch(0);
        return version;
    }

    private String[] execute(final int timeout, String... cmds) throws InterruptedException, IOException {

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
        if (timeout > 0) {
            final boolean[] interrupted = new boolean[] { false };
            Thread timouter = new Thread("ffmpegReaderTimeout") {
                public void run() {

                    try {
                        Thread.sleep(timeout);
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
        }
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

    private FFmpegSetup config;
    private LogSource   logger;

    public FFmpeg() {
        config = JsonConfig.create(FFmpegSetup.class);
        logger = LogController.getInstance().getLogger(FFmpeg.class.getName());

    }

    public boolean validateBinary() {
        return StringUtils.isNotEmpty(getFullPath());
    }

    private String getFullPath() {
        try {
            String path = config.getBinaryPath();
            if (StringUtils.isEmpty(path)) return null;

            File file = new File(path);
            if (!file.isAbsolute()) {
                file = Application.getResource(path);
            }
            if (!file.exists()) return null;
            return file.getCanonicalPath();
        } catch (Exception e) {
            logger.log(e);
            return null;

        }
    }
}
