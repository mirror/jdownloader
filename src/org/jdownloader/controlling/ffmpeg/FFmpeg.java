package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;

public class FFmpeg extends AbstractFFmpegBinary {

    public FFmpeg() {
        config = JsonConfig.create(FFmpegSetup.class);
        logger = LogController.getInstance().getLogger(FFmpeg.class.getName());
        path = config.getBinaryPath();
        if (path != null && !validatePaths()) {
            config.setBinaryPath(null);
            path = null;
        }
    }

    private boolean validatePaths() {
        File root = Application.getResource("");
        String relative = Files.getRelativePath(root, new File(path));
        logger.info("Validate Relative Path: " + relative);
        if (relative != null) {
            File correctPath = FFMpegInstallThread.getFFmpegPath("ffmpeg");
            String relativeCorrect = Files.getRelativePath(root, correctPath);
            logger.info("Validate Relative Correct Path: " + relativeCorrect);
            if (relativeCorrect != null) {
                if (!Application.getResource(relativeCorrect).exists() && Application.getResource(relative).exists()) {
                    // relative path doesn't have to match our expectation of where ffmpeg should be installed to.. Users will install
                    // to a directory structure of there own liking
                    logger.info("Validate Relative Path: User has installed to there own path!");
                    return true;
                } else if (!StringUtils.equals(relative, relativeCorrect)) {
                    logger.info("Mismatch. validation failed");
                    return false;
                }
            }
        }
        return true;
    }

    public boolean muxToMp4(FFMpegProgress progress, String out, String videoIn, String audioIn) throws InterruptedException, IOException, FFMpegException {
        return mux(progress, out, videoIn, audioIn, config.getMuxToMp4Command());
    }

    public boolean generateM4a(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {
        return demux(progress, out, audioIn, config.getDash2M4aCommand());
    }

    public boolean generateAac(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        return demux(progress, out, audioIn, config.getDash2AacCommand());
    }

    public boolean demuxAAC(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        return demux(progress, out, audioIn, config.getDemux2AacCommand());
    }

    public boolean demuxMp3(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        return demux(progress, out, audioIn, config.getDemux2Mp3Command());
    }

    private boolean demux(FFMpegProgress progress, String out, String audioIn, final String demuxCommand[]) throws InterruptedException, IOException, FFMpegException {
        logger.info("Demux:Input=" + audioIn + "|Output=" + out);
        if (StringUtils.equals(out, audioIn)) {
            throw new FFMpegException("demux failed because input file equals output file!");
        }
        final long lastModifiedAudio = new File(audioIn).lastModified();
        final File outFile = new File(out);
        final ArrayList<String> commandLine = fillCommand(out, null, audioIn, null, demuxCommand);
        if (runCommand(progress, commandLine) != null && outFile.exists() && outFile.isFile()) {
            try {
                if (lastModifiedAudio > 0 && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    outFile.setLastModified(lastModifiedAudio);
                }
            } catch (final Throwable e) {
                LoggerFactory.log(logger, e);
            }
            return true;
        }
        return false;
    }

    private boolean mux(FFMpegProgress progress, String out, String videoIn, String audioIn, final String demuxCommand[]) throws InterruptedException, IOException, FFMpegException {
        logger.info("Mux:Video=" + videoIn + "|Audio=" + audioIn + "|Output=" + out);
        if (StringUtils.equals(out, videoIn) || StringUtils.equals(out, audioIn)) {
            throw new FFMpegException("demux failed because input file equals output file!");
        }
        final long lastModifiedVideo = new File(videoIn).lastModified();
        final long lastModifiedAudio = new File(audioIn).lastModified();
        final File outFile = new File(out);
        final ArrayList<String> commandLine = fillCommand(out, videoIn, audioIn, null, demuxCommand);
        if (runCommand(progress, commandLine) != null && outFile.exists() && outFile.isFile()) {
            try {
                final long lastModified = Math.max(lastModifiedAudio, lastModifiedVideo);
                if (lastModified > 0 && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    outFile.setLastModified(lastModified);
                }
            } catch (final Throwable e) {
                LoggerFactory.log(logger, e);
            }
            return true;
        }
        return false;
    }

    public boolean demuxM4a(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        return demux(progress, out, audioIn, config.getDemux2M4aCommand());
    }

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
                            LoggerFactory.log(logger, e1);
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
                            LoggerFactory.log(logger, e1);
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

    public boolean muxToWebm(FFMpegProgress progress, String out, String videoIn, String audioIn) throws InterruptedException, IOException, FFMpegException {
        return mux(progress, out, videoIn, audioIn, config.getMuxToWebmCommand());
    }

    public boolean generateOpusAudio(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {
        return demux(progress, out, audioIn, config.getDash2OpusAudioCommand());
    }

    public boolean generateOggAudio(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {
        return demux(progress, out, audioIn, config.getDash2OggAudioCommand());
    }

}
