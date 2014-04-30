package org.jdownloader.controlling.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.plugins.PluginProgress;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;

public class FFmpeg {
    
    private String[] execute(final int timeout, PluginProgress progess, File runin, String... cmds) throws InterruptedException, IOException {
        final ProcessBuilder pb = ProcessBuilderFactory.create(cmds);
        if (runin != null) pb.directory(runin);
        final StringBuilder inputStream = new StringBuilder();
        final StringBuilder errorStream = new StringBuilder();
        final Process process = pb.start();
        
        final Thread reader1 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    readInputStreamToString(inputStream, process.getInputStream());
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
        };
        
        final Thread reader2 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    readInputStreamToString(errorStream, process.getErrorStream());
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
        };
        if (CrossSystem.isWindows()) {
            reader1.setPriority(Thread.NORM_PRIORITY + 1);
            reader2.setPriority(Thread.NORM_PRIORITY + 1);
        }
        reader1.start();
        reader2.start();
        if (timeout > 0) {
            final AtomicBoolean timeoutReached = new AtomicBoolean(false);
            final AtomicBoolean processAlive = new AtomicBoolean(true);
            Thread timouter = new Thread("ffmpegReaderTimeout") {
                public void run() {
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (processAlive.get()) {
                        timeoutReached.set(true);
                        process.destroy();
                    }
                }
            };
            timouter.start();
            process.waitFor();
            processAlive.set(false);
            timouter.interrupt();
            if (timeoutReached.get()) { throw new InterruptedException("Timeout!"); }
            reader1.join();
            reader2.join();
            return new String[] { inputStream.toString(), errorStream.toString() };
        } else {
            process.waitFor();
            reader1.join();
            reader2.join();
            return new String[] { inputStream.toString(), errorStream.toString() };
        }
        
    }
    
    private String readInputStreamToString(StringBuilder ret, final InputStream fis) throws IOException {
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(fis, "UTF8"));
            String line;
            final String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                if (ret != null) {
                    synchronized (ret) {
                        if (ret.length() > 0) {
                            ret.append(sep);
                        } else if (line.startsWith("\uFEFF")) {
                            /*
                             * Workaround for this bug: http://bugs.sun.com/view_bug.do?bug_id=4508058 http://bugs.sun.com/view_bug.do?bug_id=6378911
                             */
                            line = line.substring(1);
                        }
                        ret.append(line);
                    }
                }
            }
            if (ret == null) return null;
            return ret.toString();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof Error) throw (Error) e;
            throw new RuntimeException(e);
        }
    }
    
    private FFmpegSetup config;
    private LogSource   logger;
    private String      path;
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public FFmpeg() {
        config = JsonConfig.create(FFmpegSetup.class);
        logger = LogController.getInstance().getLogger(FFmpeg.class.getName());
        path = config.getBinaryPath();
    }
    
    public FFmpeg(String path) {
        this();
        this.path = path;
    }
    
    public boolean validateBinary() {
        String fp = getFullPath();
        logger.info("Validate FFmpeg Binary: " + fp);
        if (StringUtils.isEmpty(fp)) {
            logger.info("Binary does not exist");
            return false;
        }
        
        if (fp.toLowerCase(Locale.ENGLISH).endsWith("ffmpeg") || fp.toLowerCase(Locale.ENGLISH).endsWith("ffmpeg.exe")) {
            
            // only check if the binary is ffmpeg
            for (int i = 0; i < 5; i++) {
                try {
                    logger.info("Start ");
                    long t = System.currentTimeMillis();
                    String[] result = execute(-1, null, null, fp, "-version");
                    logger.info(result[0]);
                    logger.info(result[1]);
                    logger.info("Done in" + (System.currentTimeMillis() - t));
                    boolean ret = result != null && result.length == 2 && result[0] != null && result[0].toLowerCase(Locale.ENGLISH).contains("ffmpeg");
                    if (ret) {
                        logger.info("Binary is ok: " + ret);
                        return ret;
                    }
                } catch (InterruptedException e) {
                    logger.log(e);
                    logger.info("Binary is ok(i): " + false);
                    return false;
                } catch (IOException e) {
                    logger.log(e);
                }
            }
        }
        logger.info("Binary is ok: " + false);
        return false;
    }
    
    private String getFullPath() {
        try {
            
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
    
    public boolean isAvailable() {
        return validateBinary();
    }
    
    public boolean muxToMp4(FFMpegProgress progress, String out, String videoIn, String audioIn) throws InterruptedException, IOException, FFMpegException {
        logger.info("Merging " + videoIn + " + " + audioIn + " = " + out);
        
        long lastModifiedVideo = new File(videoIn).lastModified();
        long lastModifiedAudio = new File(audioIn).lastModified();
        
        ArrayList<String> commandLine = fillCommand(out, videoIn, audioIn, config.getMuxToMp4Command());
        if (runCommand(progress, commandLine)) {
            
            try {
                
                if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    new File(out).setLastModified(Math.max(lastModifiedAudio, lastModifiedVideo));
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            return true;
        }
        return false;
        
    }
    
    /**
     * @param out
     * @param videoIn
     * @param audioIn
     * @param mc
     * @return
     */
    public ArrayList<String> fillCommand(String out, String videoIn, String audioIn, String[] mc) {
        ArrayList<String> commandLine = new ArrayList<String>();
        commandLine.add(getFullPath());
        for (int i = 0; i < mc.length; i++) {
            String param = mc[i];
            param = param.replace("%video", videoIn == null ? "" : videoIn);
            param = param.replace("%audio", audioIn == null ? "" : audioIn);
            param = param.replace("%out", out);
            commandLine.add(param);
        }
        
        return commandLine;
    }
    
    public boolean generateM4a(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {
        
        long lastModifiedAudio = new File(audioIn).lastModified();
        
        ArrayList<String> commandLine = fillCommand(out, null, audioIn, config.getDash2M4aCommand());
        if (runCommand(progress, commandLine)) {
            
            try {
                
                if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    new File(out).setLastModified(lastModifiedAudio);
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            return true;
        }
        return false;
        
    }
    
    public boolean runCommand(FFMpegProgress progress, ArrayList<String> commandLine) throws IOException, InterruptedException, FFMpegException {
        
        final ProcessBuilder pb = ProcessBuilderFactory.create(commandLine);
        
        final StringBuilder errorStream = new StringBuilder();
        final Process process = pb.start();
        try {
            final Thread reader1 = new Thread("ffmpegReader") {
                public void run() {
                    try {
                        readInputStreamToString(null, process.getInputStream());
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            };
            
            final Thread reader2 = new Thread("ffmpegReader") {
                public void run() {
                    try {
                        readInputStreamToString(errorStream, process.getErrorStream());
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            };
            if (CrossSystem.isWindows()) {
                reader1.setPriority(Thread.NORM_PRIORITY + 1);
                reader2.setPriority(Thread.NORM_PRIORITY + 1);
            }
            reader1.start();
            reader2.start();
            long lastUpdate = System.currentTimeMillis();
            long lastLength = 0;
            while (true) {
                final String string;
                synchronized (errorStream) {
                    string = errorStream.toString();
                }
                String duration = new Regex(string, "Duration\\: (.*?).?\\d*?\\, start").getMatch(0);
                if (duration != null) {
                    long ms = formatStringToMilliseconds(duration);
                    String[] times = new Regex(string, "time=(.*?).?\\d*? ").getColumn(0);
                    if (times != null && times.length > 0) {
                        long msDone = formatStringToMilliseconds(times[times.length - 1]);
                        System.out.println(msDone + "/" + ms);
                        if (progress != null) progress.updateValues(msDone, ms);
                    }
                }
                if (lastLength != string.length()) {
                    lastUpdate = System.currentTimeMillis();
                    lastLength = string.length();
                }
                try {
                    int exitCode = process.exitValue();
                    reader2.join();
                    
                    logger.info(errorStream.toString());
                    
                    boolean ret = exitCode == 0;
                    if (!ret) {
                        throw new FFMpegException("FFmpeg Failed");
                    } else {
                        return true;
                    }
                } catch (IllegalThreadStateException e) {
                    // still running;
                }
                if (System.currentTimeMillis() - lastUpdate > 60000) {
                    // 60 seconds without any ffmpeg update. interrupt
                    throw new InterruptedException("FFMPeg does not answer");
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            logger.log(e);
            throw e;
        } finally {
            if (process != null) process.destroy();
        }
    }
    
    public boolean generateAac(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        
        long lastModifiedAudio = new File(audioIn).lastModified();
        
        ArrayList<String> commandLine = fillCommand(out, null, audioIn, config.getDash2AacCommand());
        if (runCommand(progress, commandLine)) {
            
            try {
                
                if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    new File(out).setLastModified(lastModifiedAudio);
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            return true;
        }
        return false;
        
    }
    
    public static long formatStringToMilliseconds(final String text) {
        final String[] found = new Regex(text, "(\\d+):(\\d+):(\\d+)").getRow(0);
        if (found == null) { return 0; }
        int hours = Integer.parseInt(found[0]);
        int minutes = Integer.parseInt(found[1]);
        int seconds = Integer.parseInt(found[2]);
        
        return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000;
    }
    
    public boolean demuxAAC(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        
        long lastModifiedAudio = new File(audioIn).lastModified();
        
        ArrayList<String> commandLine = fillCommand(out, null, audioIn, config.getDemux2AacCommand());
        if (runCommand(progress, commandLine)) {
            
            try {
                
                if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    new File(out).setLastModified(lastModifiedAudio);
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            return true;
        }
        return false;
    }
    
    public boolean demuxMp3(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        long lastModifiedAudio = new File(audioIn).lastModified();
        
        ArrayList<String> commandLine = fillCommand(out, null, audioIn, config.getDemux2Mp3Command());
        if (runCommand(progress, commandLine)) {
            try {
                if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    new File(out).setLastModified(lastModifiedAudio);
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            return true;
        }
        return false;
    }
    
    public boolean demuxMp4(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        long lastModifiedAudio = new File(audioIn).lastModified();
        
        ArrayList<String> commandLine = fillCommand(out, null, audioIn, config.getDemux2M4aCommand());
        if (runCommand(progress, commandLine)) {
            
            try {
                if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    new File(out).setLastModified(lastModifiedAudio);
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            return true;
        }
        return false;
    }
    
}
