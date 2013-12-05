package jd.plugins.hoster;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.requests.GetRequest;
import jd.plugins.Account;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.DownloadPluginProgress;
import jd.plugins.download.raf.HashResult;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.controlling.ffmpeg.FFMpegInstallProgress;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegProvider;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloadcore.v15.Downloadable;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;

@HostPlugin(revision = "$Revision: 24000 $", interfaceVersion = 3, names = { "youtube.com" }, urls = { "((httpJDYoutube|youtubeJDhttps?)://[\\w\\.\\-]*?(youtube|googlevideo)\\.com/(videoplayback\\?.+|get_video\\?.*?video_id=.+\\&.+(\\&fmt=\\d+)?))|((httpJDYoutube|youtubeJDhttps?)://[\\w\\.\\-]*?(video\\.google|googlevideo|youtube)\\.com/api/timedtext\\?.+\\&v=[a-z\\-_A-Z0-9]+)|((httpJDYoutube|youtubeJDhttps?)://img\\.youtube.com/vi/[a-z\\-_A-Z0-9]+/(hqdefault|mqdefault|default|maxresdefault)\\.jpg)" }, flags = { 2 })
public class YoutubeDash extends Youtube {

    private final String           DASH_AUDIO          = "DASH_AUDIO";
    private final String           DASH_AUDIO_SIZE     = "DASH_AUDIO_SIZE";
    private final String           DASH_AUDIO_LOADED   = "DASH_AUDIO_LOADED";
    private final String           DASH_AUDIO_CHUNKS   = "DASH_AUDIO_CHUNKS";
    private final String           DASH_AUDIO_FINISHED = "DASH_AUDIO_FINISHED";

    private final String           DASH_VIDEO          = "DASH_VIDEO";
    private final String           DASH_VIDEO_SIZE     = "DASH_VIDEO_SIZE";
    private final String           DASH_VIDEO_LOADED   = "DASH_VIDEO_LOADED";
    private final String           DASH_VIDEO_CHUNKS   = "DASH_VIDEO_CHUNKS";
    private final String           DASH_VIDEO_FINISHED = "DASH_VIDEO_FINISHED";

    protected String               dashAudioURL        = null;
    protected String               dashVideoURL        = null;
    private ArrayList<LinkVariant> variants;

    public static class YoutubeVariant implements LinkVariant {

        private String name;

        public YoutubeVariant(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

    }

    public YoutubeDash(PluginWrapper wrapper) {
        super(wrapper);
        variants = new ArrayList<LinkVariant>();
        variants.add(new YoutubeVariant("1080p Mp4-Video"));
        variants.add(new YoutubeVariant("720p Mp4-Video"));
        variants.add(new YoutubeVariant("480p Mp4-Video"));
        variants.add(new YoutubeVariant("AAC-Audio"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("DASH", false) == false) return super.requestFileInformation(downloadLink);
        downloadLink.setFinalFileName(downloadLink.getStringProperty("name", null));
        downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", -1l));
        final PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("youtube.com");
        if (plugin == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "cannot decrypt videolink"); }
        final HashMap<Integer, String[]> linksFound = ((jd.plugins.decrypter.TbCm) plugin).getLinks(downloadLink.getStringProperty("videolink", null), this.prem, this.br, 0);
        String[] linkFound = linksFound.get(downloadLink.getProperty(DASH_VIDEO));
        if (linkFound != null && linkFound.length > 0) dashVideoURL = linkFound[0];
        linkFound = linksFound.get(downloadLink.getProperty(DASH_AUDIO));
        if (linkFound != null && linkFound.length > 0) dashAudioURL = linkFound[0];
        if (dashAudioURL == null || dashVideoURL == null) {
            if (this.br.containsHTML("<div\\s+id=\"verify-age-actions\">")) { throw new PluginException(PluginException.VALUE_ID_PREMIUM_ONLY); }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private boolean downloadDashStream(final DownloadLink downloadLink, boolean videoORAudio) throws Exception {
        final long totalSize = downloadLink.getLongProperty("size", -1l);
        GetRequest request = null;
        final String dashName;
        final String dashChunksProperty;
        final String dashSizeProperty;
        final String dashLoadedProperty;
        final String dashFinishedProperty;
        final long chunkOffset;
        if (videoORAudio) {
            request = new GetRequest(dashVideoURL);
            dashName = getDashVideoFileName(downloadLink);
            dashChunksProperty = DASH_VIDEO_CHUNKS;
            dashSizeProperty = DASH_VIDEO_SIZE;
            dashLoadedProperty = DASH_VIDEO_LOADED;
            dashFinishedProperty = DASH_VIDEO_FINISHED;
            chunkOffset = 0;
        } else {
            request = new GetRequest(dashAudioURL);
            dashName = getDashAudioFileName(downloadLink);
            dashChunksProperty = DASH_AUDIO_CHUNKS;
            dashSizeProperty = DASH_AUDIO_SIZE;
            dashLoadedProperty = DASH_AUDIO_LOADED;
            dashFinishedProperty = DASH_AUDIO_FINISHED;
            chunkOffset = downloadLink.getLongProperty(DASH_VIDEO_SIZE, -1l);
        }
        final String dashPath = new File(downloadLink.getDownloadDirectory(), dashName).getAbsolutePath();
        final DownloadLink dashLink = new DownloadLink(this, dashName, getHost(), request.getUrl(), true);
        dashLink.setLivePlugin(this);
        final LinkStatus videoLinkStatus = new LinkStatus(dashLink);
        Downloadable dashDownloadable = new DownloadLinkDownloadable(dashLink) {

            long[] chunkProgress = null;
            {
                Object ret = downloadLink.getProperty(dashChunksProperty, null);
                if (ret != null && ret instanceof long[]) {
                    chunkProgress = (long[]) ret;
                } else {
                    if (ret != null && ret instanceof List) {
                        /* restored json-object */
                        List<Object> list = ((List<Object>) ret);
                        long[] ret2 = new long[list.size()];
                        for (int i = 0; i < ret2.length; i++) {
                            ret2[i] = Long.valueOf(list.get(0).toString());
                        }
                        chunkProgress = ret2;
                    }
                }
            }

            @Override
            public void setResumeable(boolean value) {
                downloadLink.setResumeable(value);
            }

            @Override
            public boolean isResumable() {
                return true;
            }

            @Override
            public void addDownloadTime(long ms) {
                downloadLink.addDownloadTime(ms);
            }

            @Override
            public void setHashResult(HashResult result) {
            }

            @Override
            public void setFinalFileOutput(String absolutePath) {
            }

            @Override
            public String getFinalFileOutput() {
                return dashPath;
            }

            @Override
            public void lockFiles(File... files) throws FileIsLockedException {
                /**
                 * do nothing, handleDownload does all the locking
                 */
            }

            @Override
            public void unlockFiles(File... files) {
                /**
                 * do nothing, handleDownload does all the locking
                 */
            }

            @Override
            public void waitForNextConnectionAllowed() throws InterruptedException {
                YoutubeDash.this.waitForNextConnectionAllowed(downloadLink);
            }

            @Override
            public String getFileOutput() {
                return dashPath;
            }

            @Override
            public int getLinkStatus() {
                return videoLinkStatus.getStatus();
            }

            @Override
            public long getVerifiedFileSize() {
                return downloadLink.getLongProperty(dashSizeProperty, -1l);
            }

            @Override
            public void setDownloadTotalBytes(long l) {
            }

            @Override
            public long[] getChunksProgress() {
                return chunkProgress;
            }

            @Override
            public void setChunksProgress(long[] ls) {
                chunkProgress = ls;
                if (ls == null) {
                    downloadLink.setProperty(dashChunksProperty, Property.NULL);
                } else {
                    downloadLink.setProperty(dashChunksProperty, ls);
                }
            }

            @Override
            public void setLinkStatus(int finished) {
                if (LinkStatus.FINISHED == finished) {
                    downloadLink.setProperty(dashFinishedProperty, true);
                } else {
                    downloadLink.setProperty(dashFinishedProperty, Property.NULL);
                }
            }

            @Override
            public void setVerifiedFileSize(long length) {
                if (length >= 0) {
                    downloadLink.setProperty(dashSizeProperty, length);
                } else {
                    downloadLink.setProperty(dashSizeProperty, Property.NULL);
                }
            }

            @Override
            public String getFinalFileName() {
                return dashName;
            }

            @Override
            public void setFinalFileName(String newfinalFileName) {
            }

            @Override
            public long getDownloadTotalBytes() {
                return downloadLink.getLongProperty(dashLoadedProperty, -1);
            }

            @Override
            public boolean isDoFilesizeCheckEnabled() {
                return true;
            }

            @Override
            public void setDownloadBytesLoaded(long bytes) {
                if (bytes < 0) {
                    downloadLink.setProperty(dashLoadedProperty, Property.NULL);
                } else {
                    downloadLink.setProperty(dashLoadedProperty, bytes);
                }
                downloadLink.setDownloadCurrent(chunkOffset + bytes);
            }

            @Override
            public boolean isHashCheckEnabled() {
                return false;
            }

            @Override
            public SingleDownloadController getDownloadLinkController() {
                return downloadLink.getDownloadLinkController();
            }

            @Override
            public void setPluginProgress(final PluginProgress progress) {
                if (progress != null && progress instanceof DownloadPluginProgress) {
                    DownloadPluginProgress dashVideoProgress = new DownloadPluginProgress(this, (DownloadInterface) progress.getProgressSource(), progress.getColor()) {
                        @Override
                        public long getCurrent() {
                            return chunkOffset + ((DownloadInterface) progress.getProgressSource()).getTotalLinkBytesLoadedLive();
                        };

                        @Override
                        public long getTotal() {
                            return totalSize;
                        }

                        public long getDuration() {
                            return System.currentTimeMillis() - startTimeStamp;
                        }

                        public long getSpeed() {
                            return ((DownloadInterface) progress.getProgressSource()).getManagedConnetionHandler().getSpeed();
                        }
                    };
                    downloadLink.setPluginProgress(dashVideoProgress);
                    return;
                }
                downloadLink.setPluginProgress(progress);
            }
        };
        dl = BrowserAdapter.openDownload(br, dashDownloadable, request, true, 0);
        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !this.dl.getConnection().getContentType().startsWith("application")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dl.startDownload();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("DASH", false) == false) {
            super.handleFree(downloadLink);
            return;
        }
        handleDownload(downloadLink, null);
    }

    public void handleDownload(final DownloadLink downloadLink, Account account) throws Exception {
        FFmpeg ffmpeg = new FFmpeg();
        if (!ffmpeg.isAvailable()) {
            FFMpegInstallProgress progress = new FFMpegInstallProgress();
            downloadLink.setPluginProgress(progress);
            try {
                FFmpegProvider.getInstance().install(progress);
            } finally {
                downloadLink.setPluginProgress(null);
            }
            ffmpeg.setPath(JsonConfig.create(FFmpegSetup.class).getBinaryPath());
            if (!ffmpeg.isAvailable()) {
                //
                throw new PluginException(LinkStatus.ERROR_FATAL, _GUI._.YoutubeDash_handleFree_ffmpegmissing());
            }
        }
        if (account != null) {
            this.login(account, this.br, false, false);
            prem = true;
        } else {
            prem = false;
        }
        requestFileInformation(downloadLink);

        final SingleDownloadController dlc = downloadLink.getDownloadLinkController();
        List<File> locks = new ArrayList<File>();
        locks.addAll(deleteDownloadLink(downloadLink));
        try {
            for (File lock : locks) {
                dlc.lockFile(lock);
            }
            if (new File(getVideoStreamPath(downloadLink)).exists()) {
                downloadLink.setProperty(DASH_VIDEO_FINISHED, true);
            }
            boolean loadVideo = !downloadLink.getBooleanProperty(DASH_VIDEO_FINISHED, false);
            loadVideo |= !new File(getVideoStreamPath(downloadLink)).exists();
            if (loadVideo) {
                /* videoStream not finished yet, resume/download it */
                if (!downloadDashStream(downloadLink, true)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }
            if (new File(getAudioStreamPath(downloadLink)).exists()) {
                downloadLink.setProperty(DASH_AUDIO_FINISHED, true);
            }
            /* videoStream is finished */
            boolean loadAudio = !downloadLink.getBooleanProperty(DASH_AUDIO_FINISHED, false);
            loadAudio |= !new File(getAudioStreamPath(downloadLink)).exists();
            if (loadAudio) {
                /* audioStream not finished yet, resume/download it */
                if (!downloadDashStream(downloadLink, false)) {

                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }

            if (new File(getAudioStreamPath(downloadLink)).exists() && new File(getVideoStreamPath(downloadLink)).exists() && !new File(downloadLink.getFileOutput()).exists()) {
                /* audioStream also finished */
                FFMpegProgress progress = new FFMpegProgress();
                downloadLink.setPluginProgress(progress);
                try {
                    if (ffmpeg.merge(progress, downloadLink.getFileOutput(), getVideoStreamPath(downloadLink), getAudioStreamPath(downloadLink))) {
                        downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                        new File(getVideoStreamPath(downloadLink)).delete();
                        new File(getAudioStreamPath(downloadLink)).delete();
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                    }
                } finally {
                    downloadLink.setPluginProgress(null);
                }
            }
        } catch (final FileIsLockedException e) {
            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
        } finally {
            for (File lock : locks) {
                dlc.unlockFile(lock);
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getBooleanProperty("DASH", false) == false) {
            super.handlePremium(downloadLink, account);
            return;
        }
        handleDownload(downloadLink, account);
    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
        downloadLink.setProperty("DASH_VIDEO_CHUNKS", Property.NULL);
        downloadLink.setProperty("DASH_AUDIO_CHUNKS", Property.NULL);
        downloadLink.setProperty("DASH_VIDEO_LOADED", Property.NULL);
        downloadLink.setProperty("DASH_AUDIO_LOADED", Property.NULL);
        downloadLink.setProperty("DASH_VIDEO_FINISHED", Property.NULL);
        downloadLink.setProperty("DASH_AUDIO_FINISHED", Property.NULL);
        super.resetDownloadlink(downloadLink);
    }

    @Override
    public List<File> deleteDownloadLink(DownloadLink link) {
        List<File> ret = super.deleteDownloadLink(link);
        ret.add(new File(getVideoStreamPath(link)));
        ret.add(new File(getAudioStreamPath(link)));
        ret.add(new File(getVideoStreamPath(link) + ".part"));
        ret.add(new File(getAudioStreamPath(link) + ".part"));
        return ret;
    }

    public String getAudioStreamPath(DownloadLink link) {
        return new File(link.getDownloadDirectory(), getDashAudioFileName(link)).getAbsolutePath();
    }

    public String getDashAudioFileName(DownloadLink link) {
        return link.getStringProperty("ytID", null) + link.getProperty(DASH_AUDIO) + ".dashAudio";
    }

    public String getVideoStreamPath(DownloadLink link) {
        return new File(link.getDownloadDirectory(), getDashVideoFileName(link)).getAbsolutePath();
    }

    public String getDashVideoFileName(DownloadLink link) {
        return link.getStringProperty("ytID", null) + link.getProperty(DASH_VIDEO) + ".dashVideo";
    }

    @Override
    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        return variants.get(downloadLink.getIntegerProperty("VARIANT", 0));
    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        downloadLink.setProperty("VARIANT", variants.indexOf(variant));
    }

    public boolean hasVariantToChooseFrom(DownloadLink downloadLink) {
        return true;
    }

    @Override
    public List<LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
        return variants;
    }

    @Override
    public void extendLinkgrabberContextMenu(JComponent parent, PluginView<CrawledLink> pv) {
        super.extendLinkgrabberContextMenu(parent, pv);
    }
}
