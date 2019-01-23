package org.jdownloader.plugins.components.youtube.converter;

import java.io.File;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.components.youtube.ExternalToolRequired;

public class YoutubeConverterFLVToM4AAudio extends AbstractDemuxToAudioConverter implements ExternalToolRequired {
    private static final YoutubeConverterFLVToM4AAudio INSTANCE = new YoutubeConverterFLVToM4AAudio();

    /**
     * get the only existing instance of YoutubeMp4ToM4aAudio. This is a singleton
     *
     * @return
     */
    public static YoutubeConverterFLVToM4AAudio getInstance() {
        return YoutubeConverterFLVToM4AAudio.INSTANCE;
    }

    private final LogSource logger;

    /**
     * Create a new instance of YoutubeMp4ToM4aAudio. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private YoutubeConverterFLVToM4AAudio() {
        logger = LogController.getInstance().getLogger(YoutubeConverterFLVToM4AAudio.class.getName());
    }

    @Override
    public void run(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
        final FFMpegProgress set = new FFMpegProgress();
        try {
            downloadLink.addPluginProgress(set);
            final File file = new File(downloadLink.getFileOutput());
            plugin.checkFFmpeg(downloadLink, _GUI.T.YoutubeDash_handleDownload_youtube_dash());
            final FFmpeg ffmpeg = plugin.getFFmpeg(null, downloadLink);
            final File finalFile = downloadLink.getDownloadLinkController().getFileOutput(false, true);
            if (!ffmpeg.demuxM4a(set, finalFile.getAbsolutePath(), file.getAbsolutePath())) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());
            }
            file.delete();
            downloadLink.setVerifiedFileSize(finalFile.length());
            downloadLink.setDownloadCurrent(finalFile.length());
            try {
                downloadLink.setInternalTmpFilenameAppend(null);
                downloadLink.setInternalTmpFilename(null);
            } catch (final Throwable e) {
            }
        } finally {
            downloadLink.removePluginProgress(set);
        }
    }
}
