package jd.plugins.hoster;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
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

    public static enum YoutubeVariant implements LinkVariant {
        AAC_48(null, "48kbit/s AAC-Audio"),
        AAC_128(null, "128kbit/s AAC-Audio"),
        AAC_256(null, "256kbit/s AAC-Audio"),

        WEBM_1080(null, "1080p WebM-Video"),
        WEBM_720(null, "720p WebM-Video"),
        WEBM_480(null, "480p WebM-Video"),
        WEBM_360(null, "360p WebM-Video"),

        WEBM_3D_720(null, "720p WebM-3D-Video"),
        WEBM_3D_360_128(null, "360p WebM-3D-Video(128K Audio)"),
        WEBM_3D_360_192(null, "360p WebM-3D-Video(192k Audio)"),

        MP4_360(null, "360p MP4-Video"),
        MP4_480(null, "480p MP4-Video"),
        MP4_720(null, "720p MP4-Video"),
        MP4_1080(null, "1080p MP4-Video"),
        MP4_ORIGINAL(null, "2160p MP4-Video"),

        MP4_3D_240(null, "240p MP4-3D-Video"),
        MP4_3D_360(null, "360p MP4-3D-Video"),
        MP4_3D_520(null, "520p MP4-3D-Video"),
        MP4_3D_720(null, "720p MP4-3D-Video"),

        THREEGP_144(null, "144p 3GP Video"),
        THREEGP_240_LOW(null, "240p 3GP Video(low)"),
        THREEGP_240_HIGH(null, "240p 3GP Video(high)"),

        FLV_240_LOW(null, "240p FLV-Video(low)"),
        FLV_240_HIGH(null, "240p FLV-Video(high)"),
        FLV_360(null, "360p FLV-Video"),
        FLV_480(null, "480p FLV-Video"),

        MP4_DASH_144(null, "144p MP4-Video(dash)"),
        MP4_DASH_240(null, "240p MP4-Video(dash)"),
        MP4_DASH_360(null, "360p MP4-Video(dash)"),
        MP4_DASH_480(null, "480p MP4-Video(dash)"),
        MP4_DASH_720(null, "720p MP4-Video(dash)"),
        MP4_DASH_1080(null, "1080p MP4-Video(dash)"),
        MP4_DASH_ORIGINAL(null, "2160p MP4-Video(dash)");

        private final Icon   icon;
        private final String name;

        private YoutubeVariant(Icon icon, String name) {
            this.icon = icon;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        public String getFileExtension() {
            return null;
        }
    }

    private final String DASH_AUDIO          = "DASH_AUDIO";
    private final String DASH_AUDIO_SIZE     = "DASH_AUDIO_SIZE";
    private final String DASH_AUDIO_LOADED   = "DASH_AUDIO_LOADED";
    private final String DASH_AUDIO_CHUNKS   = "DASH_AUDIO_CHUNKS";
    private final String DASH_AUDIO_FINISHED = "DASH_AUDIO_FINISHED";

    private final String DASH_VIDEO          = "DASH_VIDEO";
    private final String DASH_VIDEO_SIZE     = "DASH_VIDEO_SIZE";
    private final String DASH_VIDEO_LOADED   = "DASH_VIDEO_LOADED";
    private final String DASH_VIDEO_CHUNKS   = "DASH_VIDEO_CHUNKS";
    private final String DASH_VIDEO_FINISHED = "DASH_VIDEO_FINISHED";

    private final String ENABLE_VARIANTS     = "ENABLE_VARIANTS";

    private final String ALLOW_AAC           = "ALLOW_AAC2";

    protected String     dashAudioURL        = null;
    protected String     dashVideoURL        = null;

    public YoutubeDash(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("DASH", false) == false) return super.requestFileInformation(downloadLink);
        downloadLink.setFinalFileName(downloadLink.getStringProperty("name", null));
        downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", -1l));
        final PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("youtube.com");
        if (plugin == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "cannot decrypt videolink"); }
        final HashMap<Integer, String[]> linksFound = ((jd.plugins.decrypter.TbCm) plugin).getLinks(downloadLink.getStringProperty("videolink", null), this.prem, this.br, 0);
        Integer videoItag = downloadLink.getIntegerProperty(DASH_VIDEO, -1);
        Integer audioItag = downloadLink.getIntegerProperty(DASH_AUDIO, -1);
        if (linksFound != null) {
            String[] linkFound = linksFound.get(videoItag);
            if (videoItag >= 0 && linkFound != null && linkFound.length > 0) dashVideoURL = linkFound[0];
            linkFound = linksFound.get(audioItag);
            if (audioItag >= 0 && linkFound != null && linkFound.length > 0) dashAudioURL = linkFound[0];
        }
        if (dashAudioURL == null || dashVideoURL == null && videoItag >= 0) {
            if (this.br.containsHTML("<div\\s+id=\"verify-age-actions\">")) { throw new PluginException(PluginException.VALUE_ID_PREMIUM_ONLY); }
            if (this.br.containsHTML("This video may be inappropriate for some users")) { throw new PluginException(PluginException.VALUE_ID_PREMIUM_ONLY); }
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
            public PluginProgress setPluginProgress(final PluginProgress progress) {
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
                    return downloadLink.setPluginProgress(dashVideoProgress);
                }
                return downloadLink.setPluginProgress(progress);
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
                FFmpegProvider.getInstance().install(progress, _GUI._.YoutubeDash_handleDownload_youtube_dash());
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
        // debug
        Object ytID = downloadLink.getProperty("ytID");
        Object audio = downloadLink.getProperty(DASH_AUDIO);
        Object video = downloadLink.getProperty(DASH_VIDEO);
        requestFileInformation(downloadLink);

        final SingleDownloadController dlc = downloadLink.getDownloadLinkController();
        List<File> locks = new ArrayList<File>();
        locks.addAll(listProcessFiles(downloadLink));
        try {
            for (File lock : locks) {
                logger.info("Lock " + lock);
                dlc.lockFile(lock);
            }
            String videoStreamPath = getVideoStreamPath(downloadLink);
            if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                downloadLink.setProperty(DASH_VIDEO_FINISHED, true);
            }
            boolean loadVideo = !downloadLink.getBooleanProperty(DASH_VIDEO_FINISHED, false);

            if (videoStreamPath == null) {
                /* Skip video if just audio should be downloaded */
                loadVideo = false;
            } else {
                loadVideo |= !new File(videoStreamPath).exists();
            }
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
            if (new File(getAudioStreamPath(downloadLink)).exists() && !new File(downloadLink.getFileOutput()).exists()) {
                /* audioStream also finished */
                /* Do we need an exception here? If a Video is downloaded it is always finished before the audio part. TheCrap */
                if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                    FFMpegProgress progress = new FFMpegProgress();
                    downloadLink.setPluginProgress(progress);
                    try {
                        if (ffmpeg.merge(progress, downloadLink.getFileOutput(), videoStreamPath, getAudioStreamPath(downloadLink))) {
                            downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                            new File(videoStreamPath).delete();
                            new File(getAudioStreamPath(downloadLink)).delete();
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                        }
                    } finally {
                        downloadLink.setPluginProgress(null);
                    }
                } else {
                    /* Just renaming the temp-file didn't work */
                    FFMpegProgress progress = new FFMpegProgress();
                    downloadLink.setPluginProgress(progress);
                    try {
                        if (ffmpeg.generateAac(progress, downloadLink.getFileOutput(), getAudioStreamPath(downloadLink))) {
                            downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                            new File(getAudioStreamPath(downloadLink)).delete();
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                        }
                    } finally {
                        downloadLink.setPluginProgress(null);
                    }

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
    public List<File> listProcessFiles(DownloadLink link) {
        List<File> ret = super.listProcessFiles(link);
        String vs = getVideoStreamPath(link);
        String as = getAudioStreamPath(link);
        if (StringUtils.isNotEmpty(vs)) {
            // aac only does not have video streams
            ret.add(new File(vs));
            ret.add(new File(vs + ".part"));
        }
        ret.add(new File(as));
        ret.add(new File(as + ".part"));

        return ret;
    }

    public String getAudioStreamPath(DownloadLink link) {
        String audioFilenName = getDashAudioFileName(link);
        if (StringUtils.isEmpty(audioFilenName)) return null;
        return new File(link.getDownloadDirectory(), audioFilenName).getAbsolutePath();
    }

    public String getDashAudioFileName(DownloadLink link) {
        if (StringUtils.isEmpty(link.getStringProperty(DASH_AUDIO))) return null;
        // add both - audio and videoid to the path. else we might get conflicts if we download 2 qualities with the same audiostream
        return link.getStringProperty("ytID", null) + "_" + link.getProperty(DASH_VIDEO) + "_" + link.getProperty(DASH_AUDIO) + ".dashAudio";
    }

    public String getVideoStreamPath(DownloadLink link) {
        String videoFileName = getDashVideoFileName(link);
        if (StringUtils.isEmpty(videoFileName)) return null;
        return new File(link.getDownloadDirectory(), videoFileName).getAbsolutePath();
    }

    public String getDashVideoFileName(DownloadLink link) {
        if (StringUtils.isEmpty(link.getStringProperty(DASH_VIDEO))) return null;
        // add both - audio and videoid to the path. else we might get conflicts if we download 2 qualities with the same audiostream
        return link.getStringProperty("ytID", null) + "_" + link.getProperty(DASH_VIDEO) + "_" + link.getProperty(DASH_AUDIO) + ".dashVideo";
    }

    @Override
    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        String name = downloadLink.getStringProperty("VARIANT", null);
        try {
            if (name != null) return YoutubeVariant.valueOf(name);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        if (variant != null && variant instanceof YoutubeVariant) {
            YoutubeVariant v = (YoutubeVariant) variant;
            downloadLink.setProperty("VARIANT", v.name());
        }
    }

    public boolean hasVariantToChooseFrom(DownloadLink downloadLink) {
        return downloadLink.hasProperty("VARIANTS");
    }

    @Override
    public List<LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
        if (hasVariantToChooseFrom(downloadLink) == false) return null;
        Object ret = downloadLink.getTempProperties().getProperty("VARIANTS", null);
        if (ret != null) return (List<LinkVariant>) ret;
        Object ids = downloadLink.getProperty("VARIANTS", null);
        List<LinkVariant> ret2 = new ArrayList<LinkVariant>();
        for (Object id : (List<?>) ids) {
            try {
                ret2.add(YoutubeVariant.valueOf(id.toString()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        downloadLink.getTempProperties().setProperty("VARIANTS", ret2);
        return ret2;
    }

    @Override
    public void extendLinkgrabberContextMenu(JComponent parent, PluginView<CrawledLink> pv) {
        super.extendLinkgrabberContextMenu(parent, pv);
    }

    protected void setConfigElements() {
        super.setConfigElements();
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_AAC, JDL.L("plugins.hoster.youtube.checkaac", "Grab AAC?")).setDefaultValue(true), getConfig().indexOf(mp3ConfigEntry));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_VARIANTS, JDL.L("plugins.hoster.youtube.variants", "Enable Variants Support?")).setDefaultValue(false), getConfig().indexOf(bestEntryConfig));
    }
}
