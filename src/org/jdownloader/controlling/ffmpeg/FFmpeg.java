package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;

public class FFmpeg extends AbstractFFmpegBinary {

    public FFmpeg() {
        config = JsonConfig.create(FFmpegSetup.class);
        logger = LogController.getInstance().getLogger(FFmpeg.class.getName());
        path = config.getBinaryPath();

    }

    public FFmpeg(String path) {
        this();
        this.path = path;
    }

    // public boolean validateBinary() {
    // String fp = getFullPath();
    // logger.info("Validate FFmpeg Binary: " + fp);
    // if (StringUtils.isEmpty(fp)) {
    // logger.info("Binary does not exist");
    // return false;
    // }else{
    // return
    // }
    //
    //
    // if (fp.toLowerCase(Locale.ENGLISH).endsWith("ffmpeg") || fp.toLowerCase(Locale.ENGLISH).endsWith("ffmpeg.exe")) {
    //
    // // only check if the binary is ffmpeg
    // for (int i = 0; i < 5; i++) {
    // try {
    // logger.info("Start ");
    // long t = System.currentTimeMillis();
    // String[] result = execute(-1, null, null, fp, "-version");
    // logger.info("STD: " + result[0]);
    // logger.info("ERR: " + result[1]);
    // logger.info("Done in" + (System.currentTimeMillis() - t));
    // boolean ret = result != null && result.length == 2 && result[0] != null && result[0].toLowerCase(Locale.ENGLISH).contains("ffmpeg");
    // if (ret) {
    // logger.info("Binary is ok: " + ret);
    // return ret;
    // }
    // } catch (InterruptedException e) {
    // logger.log(e);
    // logger.info("Binary is ok(i): " + false);
    // return false;
    // } catch (IOException e) {
    // logger.log(e);
    // }
    // }
    // }
    // logger.info("Binary is ok: " + false);
    // return false;
    // }

    public boolean muxToMp4(FFMpegProgress progress, String out, String videoIn, String audioIn) throws InterruptedException, IOException, FFMpegException {
        logger.info("Merging " + videoIn + " + " + audioIn + " = " + out);

        long lastModifiedVideo = new File(videoIn).lastModified();
        long lastModifiedAudio = new File(audioIn).lastModified();

        ArrayList<String> commandLine = fillCommand(out, videoIn, audioIn, null, config.getMuxToMp4Command());
        if (runCommand(progress, commandLine) != null) {

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

    public boolean generateM4a(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {

        long lastModifiedAudio = new File(audioIn).lastModified();

        ArrayList<String> commandLine = fillCommand(out, null, audioIn, null, config.getDash2M4aCommand());
        if (runCommand(progress, commandLine) != null) {

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

    public boolean generateAac(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {

        long lastModifiedAudio = new File(audioIn).lastModified();

        ArrayList<String> commandLine = fillCommand(out, null, audioIn, null, config.getDash2AacCommand());
        if (runCommand(progress, commandLine) != null) {

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

    public boolean demuxAAC(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {

        long lastModifiedAudio = new File(audioIn).lastModified();

        ArrayList<String> commandLine = fillCommand(out, null, audioIn, null, config.getDemux2AacCommand());
        if (runCommand(progress, commandLine) != null) {

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

        ArrayList<String> commandLine = fillCommand(out, null, audioIn, null, config.getDemux2Mp3Command());
        if (runCommand(progress, commandLine) != null) {
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

    public boolean demuxM4a(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        long lastModifiedAudio = new File(audioIn).lastModified();

        ArrayList<String> commandLine = fillCommand(out, null, audioIn, null, config.getDemux2M4aCommand());
        if (runCommand(progress, commandLine) != null) {

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

    // public boolean demuxAndConvertToOgg(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException,
    // FFMpegException {
    // long lastModifiedAudio = new File(audioIn).lastModified();
    //
    // ArrayList<String> commandLine = fillCommand(out, null, audioIn, null, config.getDemuxAndConvert2Ogg());
    // if (runCommand(progress, commandLine) != null) {
    // try {
    // if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
    // new File(out).setLastModified(lastModifiedAudio);
    // }
    // } catch (final Throwable e) {
    // LogSource.exception(logger, e);
    // }
    // return true;
    // }
    // return false;
    // }

    public List<File> demuxAudio(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {
        long lastModifiedAudio = new File(audioIn).lastModified();
        ArrayList<File> ret = null;
        ArrayList<String> infoCommand = fillCommand(out, null, audioIn, null, "-i", "%audio");
        try {
            String res = runCommand(null, infoCommand);
            //
        } catch (FFMpegException e) {

            String[][] audioStreams = new Regex(e.getError(), "Stream \\#0\\:(\\d+)[^\\:]*\\: Audio\\: ([\\w\\d]+)").getMatches();
            int i = 0;
            ret = new ArrayList<File>();
            for (String[] audioStream : audioStreams) {
                //
                i++;
                HashMap<String, String[]> map = new HashMap<String, String[]>();
                map.put("%map", new String[] { "-map", "0:" + audioStream[0] });
                audioStream[1] = codecToContainer(audioStream[1]);
                String tempout = out + "." + i + "." + audioStream[1];
                ArrayList<String> commandLine = fillCommand(tempout, null, audioIn, map, config.getDemuxGenericCommand());
                String command = runCommand(progress, commandLine);
                //
                if (command != null) {

                    if (i > 1) {
                        File f;
                        ret.add(f = new File(tempout));

                        try {
                            if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                                f.setLastModified(lastModifiedAudio);
                            }
                        } catch (final Throwable e1) {
                            LogSource.exception(logger, e1);
                        }
                    } else {
                        File f;
                        ret.add(f = new File(out + "." + audioStream[1]));
                        new File(tempout).renameTo(f);
                        try {
                            if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                                f.setLastModified(lastModifiedAudio);
                            }
                        } catch (final Throwable e1) {
                            LogSource.exception(logger, e1);
                        }
                    }

                }

            }
        }

        return ret;
    }

    private String codecToContainer(String codec) {
        if ("aac".equals(codec)) {
            return "m4a";
        }
        return codec;
    }

}
