package jd.plugins.components;

import java.io.File;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.ffmpeg.FFMpegInstallProgress;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegProvider;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.updatev2.UpdateController;

public class YoutubeMp4ToAACAudio implements YoutubeConverter {
    private static final YoutubeMp4ToAACAudio INSTANCE = new YoutubeMp4ToAACAudio();

    /**
     * get the only existing instance of YoutubeMp4ToM4aAudio. This is a singleton
     * 
     * @return
     */
    public static YoutubeMp4ToAACAudio getInstance() {
        return YoutubeMp4ToAACAudio.INSTANCE;
    }

    private LogSource logger;

    /**
     * Create a new instance of YoutubeMp4ToM4aAudio. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private YoutubeMp4ToAACAudio() {
        logger = LogController.getInstance().getLogger(YoutubeMp4ToAACAudio.class.getName());
    }

    @Override
    public void run(DownloadLink downloadLink) throws Exception {
        PluginProgress old = null;
        FFMpegProgress set = null;
        try {
            old = downloadLink.setPluginProgress(set = new FFMpegProgress());
            File file = new File(downloadLink.getFileOutput());

            FFmpeg ffmpeg = new FFmpeg();
            synchronized (DownloadWatchDog.getInstance()) {

                if (!ffmpeg.isAvailable()) {
                    if (UpdateController.getInstance().getHandler() == null) {
                        logger.warning("Please set FFMPEG: BinaryPath in advanced options");
                        throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
                    }
                    FFMpegInstallProgress progress = new FFMpegInstallProgress();
                    progress.setProgressSource(this);
                    PluginProgress old2 = null;
                    try {
                        old = downloadLink.setPluginProgress(progress);
                        FFmpegProvider.getInstance().install(progress, _GUI._.YoutubeDash_handleDownload_youtube_dash());
                    } finally {
                        downloadLink.compareAndSetPluginProgress(progress, old2);
                    }
                    ffmpeg.setPath(JsonConfig.create(FFmpegSetup.class).getBinaryPath());
                    if (!ffmpeg.isAvailable()) {
                        //

                        List<String> requestedInstalls = UpdateController.getInstance().getHandler().getRequestedInstalls();
                        if (requestedInstalls != null && requestedInstalls.contains(org.jdownloader.controlling.ffmpeg.InstallThread.getFFmpegExtensionName())) {
                            throw new SkipReasonException(SkipReason.UPDATE_RESTART_REQUIRED);

                        } else {
                            throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
                        }

                        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
                        // _GUI._.YoutubeDash_handleFree_ffmpegmissing());
                    }
                }
            }
            downloadLink.setFinalFileOutput(null);
            File finalFile = new File(downloadLink.getFileOutput(false, true));
            if (!ffmpeg.demuxAAC(set, finalFile.getAbsolutePath(), file.getAbsolutePath())) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_()); }

            file.delete();
            downloadLink.setDownloadSize(finalFile.length());
            downloadLink.setDownloadCurrent(finalFile.length());
            try {
                downloadLink.setFinalFileOutput(finalFile.getAbsolutePath());
                downloadLink.setCustomFileOutputFilenameAppend(null);
                downloadLink.setCustomFileOutputFilename(null);
            } catch (final Throwable e) {
            }
        } finally {
            downloadLink.compareAndSetPluginProgress(set, old);
        }

    }

}
