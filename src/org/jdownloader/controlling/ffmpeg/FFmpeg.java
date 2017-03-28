package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;

public class FFmpeg extends AbstractFFmpegBinary {

    public FFmpeg() {
        super(null);
        logger = LogController.getInstance().getLogger(FFmpeg.class.getName());
        final String path = config.getBinaryPath();
        if (path != null && !validatePaths(path)) {
            config.setBinaryPath(null);
            setPath(null);
        } else {
            setPath(path);
        }
    }

    private boolean validatePaths(final String path) {
        final File root = Application.getResource("");
        final String relative = Files.getRelativePath(root, new File(path));
        logger.info("Validate Relative Path: " + relative);
        if (relative != null) {
            final File correctPath = FFMpegInstallThread.getFFmpegPath("ffmpeg");
            final String relativeCorrect = Files.getRelativePath(root, correctPath);
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

    private static final Object LOCK = new Object();

    protected boolean demux(FFMpegProgress progress, String out, String audioIn, final String demuxCommand[]) throws InterruptedException, IOException, FFMpegException {
        synchronized (LOCK) {
            logger.info("Demux:Input=" + audioIn + "|Output=" + out);
            if (StringUtils.equals(out, audioIn)) {
                throw new FFMpegException("demux failed because input file equals output file!");
            }
            final long lastModifiedAudio = new File(audioIn).lastModified();
            final File outFile = new File(out);
            String stdOut = null;
            try {
                stdOut = runCommand(progress, fillCommand(out, null, audioIn, null, demuxCommand));
            } catch (FFMpegException e) {
                // some systems have problems with special chars to find the in or out file.
                if ((e.getError() != null && e.getError().contains("No such file or directory")) || (CrossSystem.isMac() && !outFile.exists())) {
                    final File tmpAudioIn = Application.getTempResource("ffmpeg_audio_in_" + UniqueAlltimeID.create());
                    final File tmpOut = Application.getTempResource("ffmpeg_out" + UniqueAlltimeID.create());
                    logger.info("Try special char workaround!");
                    logger.info("Replace In:'" + audioIn + "' with '" + tmpAudioIn + "'");
                    logger.info("Replace Out'" + out + "' with '" + tmpOut + "'");
                    boolean okay = false;
                    try {
                        IO.copyFile(new File(audioIn), tmpAudioIn);
                        stdOut = runCommand(progress, fillCommand(tmpOut.getAbsolutePath(), null, tmpAudioIn.getAbsolutePath(), null, demuxCommand));
                        outFile.delete();
                        okay = tmpOut.renameTo(outFile);
                        if (!okay) {
                            outFile.delete();
                            IO.copyFile(tmpOut, outFile);
                            okay = true;
                        }
                    } finally {
                        tmpAudioIn.delete();
                        if (!okay) {
                            tmpOut.delete();
                        }
                    }
                } else {
                    throw e;
                }
            }
            if (stdOut != null && outFile.exists() && outFile.isFile()) {
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
    }

    protected boolean mux(FFMpegProgress progress, String out, String videoIn, String audioIn, final String muxCommand[]) throws InterruptedException, IOException, FFMpegException {
        synchronized (LOCK) {
            logger.info("Mux:Video=" + videoIn + "|Audio=" + audioIn + "|Output=" + out);
            if (StringUtils.equals(out, videoIn) || StringUtils.equals(out, audioIn)) {
                throw new FFMpegException("demux failed because input file equals output file!");
            }
            final long lastModifiedVideo = new File(videoIn).lastModified();
            final long lastModifiedAudio = new File(audioIn).lastModified();
            final File outFile = new File(out);
            String stdOut = null;
            try {
                stdOut = runCommand(progress, fillCommand(out, videoIn, audioIn, null, muxCommand));
            } catch (FFMpegException e) {
                // some systems have problems with special chars to find the in or out file.
                if ((e.getError() != null && e.getError().contains("No such file or directory")) || (CrossSystem.isMac() && !outFile.exists())) {
                    final File tmpAudioIn = Application.getTempResource("ffmpeg_audio_in_" + UniqueAlltimeID.create());
                    final File tmpVideoIn = Application.getTempResource("ffmpeg_video_in_" + UniqueAlltimeID.create());
                    final File tmpOut = Application.getTempResource("ffmpeg_out" + UniqueAlltimeID.create());
                    logger.info("Try special char workaround!");
                    logger.info("Replace In:'" + audioIn + "' with '" + tmpAudioIn + "'");
                    logger.info("Replace In:'" + videoIn + "' with '" + tmpVideoIn + "'");
                    logger.info("Replace Out'" + out + "' with '" + tmpOut + "'");
                    boolean okay = false;
                    try {
                        IO.copyFile(new File(videoIn), tmpVideoIn);
                        IO.copyFile(new File(audioIn), tmpAudioIn);
                        stdOut = runCommand(progress, fillCommand(tmpOut.getAbsolutePath(), tmpVideoIn.getAbsolutePath(), tmpAudioIn.getAbsolutePath(), null, muxCommand));
                        outFile.delete();
                        okay = tmpOut.renameTo(outFile);
                        if (!okay) {
                            outFile.delete();
                            IO.copyFile(tmpOut, outFile);
                            okay = true;
                        }
                    } finally {
                        tmpAudioIn.delete();
                        tmpVideoIn.delete();
                        if (!okay) {
                            tmpOut.delete();
                        }
                    }
                } else {
                    throw e;
                }
            }
            if (stdOut != null && outFile.exists() && outFile.isFile()) {
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
    }

    public boolean demuxM4a(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
        return demux(progress, out, audioIn, config.getDemux2M4aCommand());
    }

    public List<File> demuxAudio(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {
        synchronized (LOCK) {
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

                    String command = null;

                    try {
                        command = runCommand(progress, fillCommand(tempout, null, audioIn, map, config.getDemuxGenericCommand()));

                    } catch (FFMpegException e1) {
                        // some systems have problems with special chars to find the in or out file.
                        if (e.getError() != null && e.getError().contains("No such file or directory")) {
                            File tmpAudioIn = Application.getTempResource("ffmpeg_audio_in_" + UniqueAlltimeID.create());
                            File tmpOut = Application.getTempResource("ffmpeg_out" + UniqueAlltimeID.create());
                            File outFile = new File(tempout);
                            try {
                                IO.copyFile(new File(audioIn), tmpAudioIn);
                                command = runCommand(progress, fillCommand(tmpOut.getAbsolutePath(), null, tmpAudioIn.getAbsolutePath(), map, config.getDemuxGenericCommand()));
                                outFile.delete();
                                tmpOut.renameTo(outFile);

                            } finally {
                                tmpAudioIn.delete();

                            }
                        } else {
                            throw e;
                        }
                    }

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
