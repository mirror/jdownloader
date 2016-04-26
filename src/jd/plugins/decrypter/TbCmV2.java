//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.awt.Dialog.ModalityType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.plugins.components.youtube.ClipDataCache;
import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAVideoAndPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.YoutubeVariantCollection;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioInterface;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youtube.com", "youtube.com", "youtube.com" }, urls = { "https?://([a-z]+\\.)?yt\\.not\\.allowed/.+", "https?://([a-z]+\\.)?youtube\\.com/(embed/|.*?watch.*?v(%3D|=)|view_play_list\\?p=|playlist\\?(p|list)=|.*?g/c/|.*?grid/user/|v/|user/|channel/|course\\?list=)[A-Za-z0-9\\-_]+(.*?page=\\d+)?(.*?list=[A-Za-z0-9\\-_]+)?(\\#variant=\\S++)?|watch_videos\\?.*?video_ids=.+", "https?://youtube\\.googleapis\\.com/(v/|user/|channel/)[A-Za-z0-9\\-_]+(\\#variant=\\S+)?" }, flags = { 0, 0, 0 })
public class TbCmV2 extends PluginForDecrypt {

    private static final int DDOS_WAIT_MAX        = Application.isJared(null) ? 1000 : 10;

    private static final int DDOS_INCREASE_FACTOR = 15;

    public TbCmV2(PluginWrapper wrapper) {
        super(wrapper);
    };

    /**
     * Returns host from provided String.
     */
    static String getBase() {

        return "https://www.youtube.com";

    }

    /**
     * Returns a ListID from provided String.
     */
    private String getListIDByUrls(String originUrl) {
        // String list = null;
        // http://www.youtube.com/user/wirypodge#grid/user/41F2A8E7EBF86D7F
        // list = new Regex(originUrl, "(g/c/|grid/user/)([A-Za-z0-9\\-_]+)").getMatch(1);
        // /user/
        // http://www.youtube.com/user/Gronkh
        // if (list == null) list = new Regex(originUrl, "/user/([A-Za-z0-9\\-_]+)").getMatch(0);
        // play && course
        // http://www.youtube.com/playlist?list=PL375B54C39ED612FC

        return new Regex(originUrl, "list=([A-Za-z0-9\\-_]+)").getMatch(0);
    }

    private String getVideoIDByUrl(String URL) {
        String vuid = new Regex(URL, "v=([A-Za-z0-9\\-_]+)").getMatch(0);
        if (vuid == null) {
            vuid = new Regex(URL, "v/([A-Za-z0-9\\-_]+)").getMatch(0);
            if (vuid == null) {
                vuid = new Regex(URL, "embed/(?!videoseries\\?)([A-Za-z0-9\\-_]+)").getMatch(0);
            }
        }
        return vuid;
    }

    private HashSet<String> dupeCheckSet;

    private YoutubeConfig   cfg;

    private static Object   DIALOGLOCK = new Object();

    private String          videoID;
    private String          watch_videos;
    private String          playlistID;
    private String          channelID;
    private String          userID;

    private AbstractVariant requestedVariant;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {

        cfg = PluginJsonConfig.get(YoutubeConfig.class);
        String cryptedLink = param.getCryptedUrl();
        if (StringUtils.containsIgnoreCase(cryptedLink, "yt.not.allowed")) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            if (cfg.isAndroidSupportEnabled()) {
                if (cryptedLink.matches("https?://[\\w\\.]*yt\\.not\\.allowed/[a-z_A-Z0-9\\-]+")) {
                    cryptedLink = cryptedLink.replaceFirst("yt\\.not\\.allowed", "youtu.be");
                } else {
                    cryptedLink = cryptedLink.replaceFirst("yt\\.not\\.allowed", "youtube.com");
                }
                final DownloadLink link = createDownloadlink(cryptedLink);
                link.setContainerUrl(cryptedLink);
                ret.add(link);
            }
            return ret;
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                distribute(e);
                return super.add(e);
            }
        };
        dupeCheckSet = new HashSet<String>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("youtube.com");
        br.setCookie("http://youtube.com", "PREF", "hl=en-GB");

        String cleanedurl = Encoding.urlDecode(cryptedLink, false);
        cleanedurl = cleanedurl.replace("youtube.jd", "youtube.com");

        String requestedVariantString = new Regex(cleanedurl, "\\#variant=(\\S*)").getMatch(0);
        cleanedurl = cleanedurl.replaceAll("\\#variant=\\S+", "");
        cleanedurl = cleanedurl.replace("/embed/", "/watch?v=");
        videoID = getVideoIDByUrl(cleanedurl);
        // for watch_videos, found within youtube.com music
        watch_videos = new Regex(cleanedurl, "video_ids=([a-zA-Z0-9\\-_,]+)").getMatch(0);
        if (watch_videos != null) {
            // first uid in array is the video the user copy url on.
            videoID = new Regex(watch_videos, "([a-zA-Z0-9\\-_]+)").getMatch(0);
        }
        YoutubeHelper helper = new YoutubeHelper(br, getLogger());

        playlistID = getListIDByUrls(cleanedurl);
        userID = new Regex(cleanedurl, "/user/([A-Za-z0-9\\-_]+)").getMatch(0);
        channelID = new Regex(cleanedurl, "/channel/([A-Za-z0-9\\-_]+)").getMatch(0);

        // if (requestedVariantString != null) {
        // boolean worst = false;
        // if (requestedVariantString.startsWith("-")) {
        // worst = true;
        // requestedVariantString = requestedVariantString.substring(1);
        //
        // }
        // if (requestedVariantString.startsWith("+")) {
        // worst = false;
        // requestedVariantString = requestedVariantString.substring(1);
        //
        // }
        // VariantGroup group = null;
        // try {
        // group = VariantGroup.valueOf(requestedVariantString);
        // } catch (Throwable e) {
        //
        // }
        // if (group != null) {
        // addExtraSubtitles = false;
        // addExtraVariants = false;
        // addAllVariantsInUngroupedMode = false;
        // addBest3DVideo = false;
        // addBestImage = false;
        // addBestVideo = false;
        // addBestAudio = false;
        // addAllSubtitles = false;
        // addBestSubtitle = false;
        // addDescription = false;
        // switch (group) {
        // case AUDIO:
        // if (worst) {
        // addWorstAudio = true;
        // } else {
        // addBestAudio = true;
        // }
        //
        // break;
        // case DESCRIPTION:
        //
        // addDescription = true;
        // break;
        // case IMAGE:
        //
        // if (worst) {
        // addWorstImage = true;
        // } else {
        // addBestImage = true;
        // }
        //
        // break;
        // case SUBTITLES:
        // addBestSubtitle = true;
        // break;
        // case VIDEO:
        //
        // if (worst) {
        // addWorstVideo = true;
        // } else {
        // addBestVideo = true;
        // }
        // break;
        // // case VIDEO_3D:
        // //
        // // if (worst) {
        // // addWorst3DVideo = true;
        // // } else {
        // // addBest3DVideo = true;
        // // }
        // //
        // // break;
        // }
        // } else {
        //
        // requestedVariant = AbstractVariant.get(requestedVariantString);
        // if (requestedVariant == null) {
        // requestedVariant = AbstractVariant.get(new String(Base64.decode(requestedVariantString), "UTF-8"));
        //
        // }
        // addExtraSubtitles = false;
        // addExtraVariants = false;
        // addAllVariantsInUngroupedMode = false;
        // addBest3DVideo = false;
        // addBestImage = false;
        // addBestVideo = false;
        // addBestAudio = false;
        // addAllSubtitles = false;
        // addBestSubtitle = false;
        // addDescription = false;
        //
        // }
        //
        // }

        helper.login(false, false);
        synchronized (DIALOGLOCK) {
            if (this.isAbort()) {
                logger.info("Thread Aborted!");
                return decryptedLinks;
            }
            {
                // Prevents accidental decrypting of entire Play-List or Channel-List or User-List.
                IfUrlisAPlaylistAction playListAction = cfg.getLinkIsPlaylistUrlAction();
                if ((StringUtils.isNotEmpty(playlistID) || StringUtils.isNotEmpty(channelID) || StringUtils.isNotEmpty(userID)) && StringUtils.isEmpty(videoID)) {

                    if (playListAction == IfUrlisAPlaylistAction.ASK) {
                        ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, cleanedurl, JDL.L("plugins.host.youtube.isplaylist.question.message", "This link is a Play-List or Channel-List or User-List. What would you like to do?"), null, JDL.L("plugins.host.youtube.isplaylist.question.onlyplaylist", "Process Playlist?"), JDL.L("plugins.host.youtube.isvideoandplaylist.question.nothing", "Do Nothing?")) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }

                            @Override
                            public boolean isRemoteAPIEnabled() {
                                return true;
                            }
                        };
                        try {
                            UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                            playListAction = IfUrlisAPlaylistAction.PROCESS;
                        } catch (DialogCanceledException e) {
                            playListAction = IfUrlisAPlaylistAction.NOTHING;
                        } catch (DialogClosedException e) {
                            playListAction = IfUrlisAPlaylistAction.NOTHING;
                        }

                    }
                    switch (playListAction) {
                    case PROCESS:
                        break;
                    case NOTHING:
                    default:
                        return decryptedLinks;
                    }
                }
            }
            {

                // Check if link contains a video and a playlist
                IfUrlisAVideoAndPlaylistAction PlaylistVideoAction = cfg.getLinkIsVideoAndPlaylistUrlAction();
                if ((StringUtils.isNotEmpty(playlistID) || StringUtils.isNotEmpty(watch_videos)) && StringUtils.isNotEmpty(videoID)) {

                    if (PlaylistVideoAction == IfUrlisAVideoAndPlaylistAction.ASK) {
                        ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, cleanedurl, JDL.L("plugins.host.youtube.isvideoandplaylist.question.message", "The Youtube link contains a video and a playlist. What do you want do download?"), null, JDL.L("plugins.host.youtube.isvideoandplaylist.question.onlyvideo", "Only video"), JDL.L("plugins.host.youtube.isvideoandplaylist.question.playlist", "Complete playlist")) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }

                            @Override
                            public boolean isRemoteAPIEnabled() {
                                return true;
                            }
                        };
                        try {
                            UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                            PlaylistVideoAction = IfUrlisAVideoAndPlaylistAction.VIDEO_ONLY;
                        } catch (DialogCanceledException e) {
                            PlaylistVideoAction = IfUrlisAVideoAndPlaylistAction.PLAYLIST_ONLY;
                        } catch (DialogClosedException e) {
                            PlaylistVideoAction = IfUrlisAVideoAndPlaylistAction.NOTHING;
                        }

                    }
                    switch (PlaylistVideoAction) {

                    case PLAYLIST_ONLY:
                        // videoID = null;
                        break;
                    case VIDEO_ONLY:
                        playlistID = null;
                        watch_videos = null;
                        break;
                    default:
                        return decryptedLinks;
                    }
                }
            }
        }
        ArrayList<YoutubeClipData> videoIdsToAdd = new ArrayList<YoutubeClipData>();
        try {
            boolean userWorkaround = false;
            boolean channelWorkaround = false;
            if (StringUtils.isNotEmpty(userID) && StringUtils.isEmpty(playlistID)) {

                // the user channel parser only parses 1050 videos. this workaround finds the user channel playlist and parses this
                // playlist
                // instead
                br.getPage("https://www.youtube.com/user/" + userID + "/featured");

                playlistID = br.getRegex(">Uploads</span>.*?list=([A-Za-z0-9\\-_]+)\".+?play-all-icon-btn").getMatch(0);
                userWorkaround = StringUtils.isNotEmpty(playlistID);
            }
            if (StringUtils.isNotEmpty(channelID) && StringUtils.isEmpty(playlistID)) {

                // the user channel parser only parses 1050 videos. this workaround finds the user channel playlist and parses this
                // playlist
                // instead
                br.getPage("https://www.youtube.com/channel/" + channelID);

                playlistID = br.getRegex("list=([A-Za-z0-9\\-_]+)\".+?play-all-icon-btn").getMatch(0);
                if (StringUtils.isEmpty(playlistID) && channelID.startsWith("UC")) {
                    // channel has no play all button.
                    // like https://www.youtube.com/channel/UCbmRs17gtQxFXQyvIo5k6Ag/feed
                    playlistID = "UU" + channelID.substring(2);
                }
                channelWorkaround = StringUtils.isNotEmpty(playlistID);
            }

            ArrayList<YoutubeClipData> playlist;
            videoIdsToAdd.addAll(playlist = parsePlaylist(playlistID));
            if (channelWorkaround && playlist.size() == 0) {

                // failed
                channelWorkaround = false;
            }
            if (userWorkaround && playlist.size() == 0) {
                // failed
                userWorkaround = false;
            }
            if (!channelWorkaround) {
                videoIdsToAdd.addAll(parseChannelgrid(channelID));
            }
            if (!userWorkaround) {
                videoIdsToAdd.addAll(parseUsergrid(userID));
            }
            // some unknown playlist type?
            if (videoIdsToAdd.size() == 0 && StringUtils.isNotEmpty(playlistID)) {
                videoIdsToAdd.addAll(parseGeneric(cleanedurl));
            }
            videoIdsToAdd.addAll(parseVideoIds(watch_videos));

            if (StringUtils.isNotEmpty(videoID) && dupeCheckSet.add(videoID)) {
                videoIdsToAdd.add(new org.jdownloader.plugins.components.youtube.YoutubeClipData(videoID));
            }
            if (videoIdsToAdd.size() == 0) {
                videoIdsToAdd.addAll(parseGeneric(cleanedurl));
            }

        } catch (InterruptedException e) {
            return decryptedLinks;
        }

        // HashSet<YoutubeBasicVariant> blacklistedVariants = new HashSet<YoutubeBasicVariant>();
        // HashSet<BlackOrWhitelistEntry> blacklistedStrings = new HashSet<BlackOrWhitelistEntry>();
        // HashSet<BlackOrWhitelistEntry> extraStrings = new HashSet<BlackOrWhitelistEntry>();

        for (YoutubeClipData vid : videoIdsToAdd) {
            if (this.isAbort()) {
                throw new InterruptedException();
            }
            HashMap<String, List<VariantInfo>> groups = new HashMap<String, List<VariantInfo>>();
            HashMap<String, List<VariantInfo>> groupsExcluded = new HashMap<String, List<VariantInfo>>();
            HashMap<VariantBase, VariantInfo> allVariants = new HashMap<VariantBase, VariantInfo>();
            HashMap<String, VariantInfo> idMap = new HashMap<String, VariantInfo>();

            try {
                // make sure that we reload the video
                // ClipDataCache.clearCache(vid.videoID);
                boolean hasCache = ClipDataCache.hasCache(helper, vid.videoID);
                try {
                    vid = ClipDataCache.get(helper, vid.videoID);
                } catch (Exception e) {
                    if (hasCache) {
                        ClipDataCache.clearCache(vid.videoID);

                        vid = ClipDataCache.get(helper, vid.videoID);
                    } else {
                        throw e;
                    }
                }
            } catch (Exception e) {
                String emsg = null;
                try {
                    emsg = e.getMessage().toString();
                } catch (NullPointerException npe) {
                    // e.message can be null...
                }
                if (emsg != null && StringUtils.isEmpty(vid.error)) {
                    vid.error = emsg;
                }
                if (vid.streams == null || StringUtils.isNotEmpty(vid.error)) {
                    decryptedLinks.add(createOfflinelink("http://youtube.com/watch?v=" + vid.videoID, "Error - " + vid.videoID + (vid.title != null ? " [" + vid.title + "]:" : "") + " " + vid.error, vid.error));

                    continue;

                }
            }
            if (vid.streams == null || StringUtils.isNotEmpty(vid.error)) {
                decryptedLinks.add(createOfflinelink("http://youtube.com/watch?v=" + vid.videoID, "Error - " + vid.videoID + (vid.title != null ? " [" + vid.title + "]:" : "") + " " + vid.error, vid.error));
                if (vid.streams == null) {
                    continue;
                }
            }
            // get best video resolution
            YoutubeITAG bestVideoResolution = null;
            for (Entry<YoutubeITAG, List<YoutubeStreamData>> es : vid.streams.entrySet()) {
                if (es.getKey().getVideoResolution() != null) {
                    if (bestVideoResolution == null || bestVideoResolution.getQualityRating() < es.getKey().getQualityRating()) {
                        bestVideoResolution = es.getKey();
                    }
                }

            }
            vid.bestVideoItag = bestVideoResolution;
            if (!cfg.isExternMultimediaToolUsageEnabled()) {
                getLogger().info("isDashEnabledEnabled=false");
            }

            List<AbstractVariant> enabledVariants = new ArrayList<AbstractVariant>(AbstractVariant.listVariants());
            List<VariantIDStorable> disabled = CFG_YOUTUBE.CFG.getDisabledVariants();
            HashSet<String> disabledIds = new HashSet<String>();
            if (disabled != null) {
                for (VariantIDStorable v : disabled) {
                    disabledIds.add(v.createUniqueID());
                }
            }
            HashSet<AudioBitrate> disabledAudioBitrates = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedAudioBitrates());
            HashSet<AudioCodec> disabledAudioCodecs = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedAudioCodecs());
            HashSet<FileContainer> disabledFileContainers = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedFileContainers());
            HashSet<VariantGroup> disabledGroups = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedGroups());
            HashSet<Projection> disabledProjections = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedProjections());
            HashSet<VideoResolution> disabledResolutions = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedResolutions());
            HashSet<VideoCodec> disabledVideoCodecs = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedVideoCodecs());
            HashSet<VideoFrameRate> disabledFramerates = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedVideoFramerates());
            HashSet<VariantGroup> enabledVariantGroups = new HashSet<VariantGroup>();
            for (final Iterator<AbstractVariant> it = enabledVariants.iterator(); it.hasNext();) {
                AbstractVariant cur = it.next();
                if (disabledGroups.contains(cur.getGroup())) {
                    it.remove();
                    continue;
                }
                if (disabledFileContainers.contains(cur.getContainer())) {
                    it.remove();
                    continue;
                }
                if (cur instanceof AudioInterface) {
                    if (disabledAudioBitrates.contains(((AudioInterface) cur).getAudioBitrate())) {
                        it.remove();
                        continue;
                    }
                    if (disabledAudioCodecs.contains(((AudioInterface) cur).getAudioCodec())) {
                        it.remove();
                        continue;
                    }
                }
                if (cur instanceof VideoVariant) {
                    if (disabledVideoCodecs.contains(((VideoVariant) cur).getVideoCodec())) {
                        it.remove();
                        continue;
                    }
                    if (disabledResolutions.contains(((VideoVariant) cur).getVideoResolution())) {
                        it.remove();
                        continue;
                    }
                    if (disabledFramerates.contains(((VideoVariant) cur).getiTagVideo().getVideoFrameRate())) {
                        it.remove();
                        continue;
                    }
                    if (disabledProjections.contains(((VideoVariant) cur).getProjection())) {
                        it.remove();
                        continue;
                    }
                }
                if (cur instanceof ImageVariant) {
                    if (disabledResolutions.contains(VideoResolution.getByHeight(((ImageVariant) cur).getHeight()))) {
                        it.remove();
                        continue;
                    }
                }
                if (disabledIds.contains(new AbstractVariantWrapper(cur).getVariableIDStorable().createUniqueID())) {
                    it.remove();
                    continue;
                }
                enabledVariantGroups.add(cur.getGroup());

            }

            // write all available variants to groups and allVariants
            List<VariantInfo> variants = vid.findVariants();
            List<VariantInfo> subtitles = enabledVariantGroups.contains(VariantGroup.SUBTITLES) ? vid.findSubtitleVariants() : new ArrayList<VariantInfo>();

            ArrayList<VariantInfo> descriptions = enabledVariantGroups.contains(VariantGroup.DESCRIPTION) ? vid.findDescriptionVariant() : new ArrayList<VariantInfo>();
            if (subtitles != null) {
                variants.addAll(subtitles);
            }
            if (descriptions != null) {
                variants.addAll(descriptions);
            }
            boolean found = false;
            // if (requestedVariant != null) {
            // List<VariantInfo> sortedGroupList = groups.get(requestedVariant.getGroup().name());
            // switch (requestedVariant.getGroup()) {
            // case DESCRIPTION:
            // addDescription = true;
            // break;
            // case SUBTITLES:
            // if (sortedGroupList != null) {
            // // subtitles available
            // for (VariantInfo vi : sortedGroupList) {
            // if (vi.getVariant() instanceof SubtitleVariant) {
            // if (StringUtils.equals(((SubtitleVariant) vi.getVariant())._getUniqueId(), requestedVariant._getUniqueId())) {
            // decryptedLinks.add(createLink(vi, sortedGroupList));
            // found = true;
            // break;
            // }
            // }
            //
            // }
            // if (!found) {
            //
            // for (VariantInfo vi : sortedGroupList) {
            // if (vi.getVariant() instanceof SubtitleVariant) {
            // if (StringUtils.equals(((SubtitleVariant) vi.getVariant()).getGenericInfo().getLanguage(), ((SubtitleVariant)
            // requestedVariant).getGenericInfo().getLanguage())) {
            // decryptedLinks.add(createLink(vi, sortedGroupList));
            // found = true;
            // break;
            // }
            // }
            //
            // }
            // }
            // }
            // if (!found) {
            // addBestSubtitle = true;
            // }
            //
            // break;
            // default:
            // if (sortedGroupList != null) {
            // Collections.sort(sortedGroupList);
            // List<VariantInfo> groupList = new ArrayList<VariantInfo>(sortedGroupList);
            // Collections.sort(groupList, new Comparator<VariantInfo>() {
            // public int compare(boolean x, boolean y) {
            // return (x == y) ? 0 : (x ? 1 : -1);
            // }
            //
            // @Override
            // public int compare(VariantInfo o1, VariantInfo o2) {
            // double diff1 = Math.abs(requestedVariant.getQualityRating() - o1.getVariant().getQualityRating());
            // double diff2 = Math.abs(requestedVariant.getQualityRating() - o2.getVariant().getQualityRating());
            // if (requestedVariant.getContainer() != o2.getVariant().getContainer()) {
            // diff2 += 100d;
            // }
            // if (requestedVariant.getContainer() != o1.getVariant().getContainer()) {
            // diff1 += 100d;
            // }
            // // int ret = compare(diff1 > 0, diff2 > 0);
            // // if (ret == 0) {
            // return Double.compare(diff1, diff2);
            // // }
            // // return ret;
            // }
            //
            // });
            //
            // decryptedLinks.add(createLink(groupList.get(0), sortedGroupList));
            // System.out.println("Sorted " + requestedVariant.getQualityRating());
            // if (requestedVariant.getQualityRating() - groupList.get(0).getVariant().getQualityRating() != 0) {
            // Toolkit.getDefaultToolkit().beep();
            // }
            // for (VariantInfo vi : groupList) {
            // System.out.println(vi + " - " + (requestedVariant.getQualityRating() - vi.getVariant().getQualityRating()));
            // }
            // }
            // }
            //
            // // for (VariantInfo v : allVariants.values()) {
            // // if (v.getVariant().getTypeId().equals(requestedVariant)) {
            // // String groupID = getGroupID(v.getVariant());
            // // List<VariantInfo> fromGroup = groups.get(groupID);
            // // decryptedLinks.add(createLink(v, fromGroup));
            // // found = true;
            // // }
            // // }
            // // for (VariantInfo vi : allSubtitles) {
            // // if (StringUtils.equalsIgnoreCase(vi.getVariant()._getUniqueId(), requestedVariantString)) {
            // // decryptedLinks.add(createLink(vi, allSubtitles));
            // // found = true;
            // // }
            // // }
            // // for (VariantInfo vi : descriptions) {
            // // if (StringUtils.equalsIgnoreCase(vi.getVariant()._getUniqueId(), requestedVariantString)) {
            // // decryptedLinks.add(createLink(vi, descriptions));
            // // found = true;
            // // }
            // // }
            // // if (!found) {
            // // if(reVar!=null){
            // // ArrayList<VariantInfo> lst = new ArrayList<VariantInfo>( groups.get(reVar.getGroup().name()));
            // // VariantInfo best;
            // //
            // // for(VariantInfo vi:lst){
            // // vi.getVariant().getQualityRating()-reVar.getQualityRating()
            // // }
            // // }
            // // }
            //
            // }

            List<YoutubeVariantCollection> links = YoutubeVariantCollection.load();
            // enabledVariants
            // new VariantIDStorable(variant).
            HashSet<String> allowedVariantsSet = new HashSet<String>();
            for (AbstractVariant v : enabledVariants) {
                VariantIDStorable storable = new VariantIDStorable(v);

                allowedVariantsSet.add(storable.createUniqueID());
            }
            HashMap<VariantInfo, VariantIDStorable> storables = new HashMap<VariantInfo, VariantIDStorable>();
            for (VariantInfo v : variants) {
                System.out.println(v.getVariant());
                VariantIDStorable storable = new VariantIDStorable(v.getVariant());
                storables.put(v, storable);
            }

            // HashMap<Link, List<VariantInfo>> linkMap = new HashMap<Link, List<VariantInfo>>();
            for (YoutubeVariantCollection l : links) {
                if (!l.isEnabled()) {
                    continue;
                }
                ArrayList<VariantInfo> linkVariants = new ArrayList<VariantInfo>();
                if (StringUtils.isNotEmpty(l.getGroupingID())) {

                    for (VariantInfo v : variants) {
                        VariantIDStorable vi = storables.get(v);
                        if (StringUtils.equals(l.getGroupingID(), vi.createGroupingID())) {
                            if (allowedVariantsSet.contains(vi.createUniqueID())) {
                                linkVariants.add(v);
                            }
                        } else if (StringUtils.equals(l.getGroupingID(), vi.getContainer())) {
                            if (allowedVariantsSet.contains(vi.createUniqueID())) {
                                linkVariants.add(v);
                            }
                        }
                    }

                } else if (l.getVariants() != null && l.getVariants().size() > 0) {

                    HashSet<String> idSet = l.createUniqueIDSet();
                    for (VariantInfo v : variants) {
                        VariantIDStorable vi = storables.get(v);
                        if (idSet.contains(vi.createUniqueID())) {
                            if (allowedVariantsSet.contains(vi.createUniqueID())) {
                                linkVariants.add(v);
                            }
                        }
                    }

                } else {
                    continue;
                }

                Collections.sort(linkVariants, new Comparator<VariantInfo>() {
                    private HashMap<String, Integer> prefSubtitles = new HashMap<String, Integer>();

                    {
                        String[] prefs = CFG_YOUTUBE.CFG.getPreferedSubtitleLanguages();
                        int prefID = 0;

                        if (prefs != null) {

                            for (int i = 0; i < prefs.length; i++) {
                                if (prefs[i] != null) {
                                    prefSubtitles.put(prefs[i].toLowerCase(Locale.ENGLISH), prefID++);
                                }

                            }
                        }
                        prefSubtitles.put(TranslationFactory.getDesiredLanguage().toLowerCase(Locale.ENGLISH), prefID++);
                        prefSubtitles.put(Locale.getDefault().getLanguage().toLowerCase(Locale.ENGLISH), prefID++);

                        prefSubtitles.put("en", prefID++);
                    }

                    @Override
                    public int compare(VariantInfo o1, VariantInfo o2) {

                        if (o1.getVariant() instanceof SubtitleVariant && o2.getVariant() instanceof SubtitleVariant) {
                            Integer pref1 = prefSubtitles.get(((SubtitleVariant) o1.getVariant()).getLanguageCode());
                            Integer pref2 = prefSubtitles.get(((SubtitleVariant) o2.getVariant()).getLanguageCode());
                            if (pref1 == null) {
                                pref1 = Integer.MAX_VALUE;
                            }
                            if (pref2 == null) {
                                pref2 = Integer.MAX_VALUE;
                            }
                            int ret = CompareUtils.compare(pref1, pref2);
                            if (ret == 0) {
                                ret = CompareUtils.compare(o2.getVariant().getQualityRating(), o1.getVariant().getQualityRating());
                            }
                            if (ret == 0) {
                                ret = CompareUtils.compare(((SubtitleVariant) o1.getVariant()).getDisplayLanguage(), ((SubtitleVariant) o2.getVariant()).getDisplayLanguage());
                            }
                            return ret;

                        }
                        return CompareUtils.compare(o2.getVariant().getQualityRating(), o1.getVariant().getQualityRating());
                    }
                });
                // remove dupes
                // System.out.println("Link " + l.getName());
                VariantInfo last = null;
                for (final Iterator<VariantInfo> it = linkVariants.iterator(); it.hasNext();) {
                    VariantInfo cur = it.next();
                    if (last != null) {
                        if (StringUtils.equals(cur.getVariant().getTypeId(), last.getVariant().getTypeId())) {
                            it.remove();
                            continue;
                        }
                    }
                    last = cur;
                }

                if (linkVariants.size() > 0) {
                    DownloadLink lnk = createLink(l, linkVariants.get(0), linkVariants);

                    decryptedLinks.add(lnk);
                }

            }
            // main: for (Entry<String, List<VariantInfo>> e : groups.entrySet()) {
            // if (e.getValue().size() == 0) {
            // continue;
            // }
            // Collections.sort(e.getValue());
            // if (e.getKey().equals(VariantGroup.DESCRIPTION.name()) || e.getKey().equalsIgnoreCase(FileContainer.TXT.name())) {
            // // add description links
            // if (addDescription) {
            // for (VariantInfo vi : descriptions) {
            // decryptedLinks.add(createLink(vi, descriptions));
            // }
            // }
            //
            // } else if (e.getKey().equals(VariantGroup.SUBTITLES.name()) || e.getKey().equalsIgnoreCase(FileContainer.SRT.name())) {
            // // add subtitle links
            // if (addBestSubtitle) {
            // // special handling for subtitles
            // final String[] keys = cfg.getPreferedSubtitleLanguages();
            // // first try the users prefered list
            // if (keys != null) {
            // for (final String locale : keys) {
            // try {
            // for (VariantInfo vi : e.getValue()) {
            // if (StringUtils.equalsIgnoreCase(locale, ((SubtitleVariant) vi.getVariant()).getGenericInfo().getLanguage())) {
            // decryptedLinks.add(createLink(vi, e.getValue()));
            // continue main;
            // }
            // }
            // } catch (Throwable e1) {
            // getLogger().log(e1);
            // }
            // }
            // }
            // try {
            // // try to find the users locale
            // final String desiredLocale = TranslationFactory.getDesiredLanguage().toLowerCase(Locale.ENGLISH);
            // for (final VariantInfo vi : e.getValue()) {
            //
            // if (StringUtils.equalsIgnoreCase(desiredLocale, ((SubtitleVariant) vi.getVariant()).getGenericInfo().getLanguage())) {
            //
            // decryptedLinks.add(createLink(vi, e.getValue()));
            // continue main;
            // }
            // }
            // } catch (Exception e1) {
            // // try english
            // getLogger().log(e1);
            // for (final VariantInfo vi : e.getValue()) {
            // if (StringUtils.equalsIgnoreCase("en", ((SubtitleVariant) vi.getVariant()).getGenericInfo().getLanguage())) {
            //
            // decryptedLinks.add(createLink(vi, e.getValue()));
            // continue main;
            // }
            // }
            // }
            // // fallback: use the first
            // decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
            // } else {
            // if (addAllSubtitles) {
            // for (VariantInfo vi : e.getValue()) {
            // decryptedLinks.add(createLink(vi, e.getValue()));
            // }
            // }
            // }
            // } else {
            // if (cfg.getGroupLogic() != GroupLogic.NO_GROUP) {
            // switch (e.getValue().get(0).getVariant().getGroup()) {
            // case AUDIO:
            // if (addBestAudio) {
            // decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
            // }
            // if (addWorstAudio) {
            // decryptedLinks.add(createLink(e.getValue().get(e.getValue().size() - 1), e.getValue()));
            // }
            // break;
            // case IMAGE:
            // if (addBestImage) {
            // decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
            // }
            // if (addWorstImage) {
            // decryptedLinks.add(createLink(e.getValue().get(e.getValue().size() - 1), e.getValue()));
            // }
            // break;
            // case VIDEO:
            // if (addBestVideo) {
            // VariantInfo best = null;
            // if (cfg.isBestVideoVariant1080pLimitEnabled()) {
            // for (VariantInfo vv : e.getValue()) {
            // try {
            // VideoVariant videoVariant = ((VideoVariant) vv.getVariant());
            //
            // if (Math.min(videoVariant.getVideoWidth(), videoVariant.getVideoHeight()) <= 1080) {
            // if (best == null) {
            // best = vv;
            // } else if (best.getVariant().getQualityRating() < vv.getVariant().getQualityRating()) {
            // best = vv;
            // }
            // }
            // } catch (Throwable ee) {
            // }
            // }
            // }
            // if (best != null) {
            // decryptedLinks.add(createLink(best, e.getValue()));
            // } else {
            // decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
            // }
            // }
            // if (addWorstVideo) {
            // decryptedLinks.add(createLink(e.getValue().get(e.getValue().size() - 1), e.getValue()));
            // }
            // break;
            // // case VIDEO_3D:
            // // if (addBest3DVideo) {
            // //
            // // VariantInfo best = null;
            // // if (cfg.isBestVideoVariant1080pLimitEnabled()) {
            // // for (VariantInfo vv : e.getValue()) {
            // // try {
            // // VideoVariant videoVariant = ((VideoVariant) vv.getVariant());
            // //
            // // if (Math.min(videoVariant.getVideoWidth(), videoVariant.getVideoHeight()) <= 1080) {
            // // if (best == null) {
            // // best = vv;
            // // } else if (best.getVariant().getQualityRating() < vv.getVariant().getQualityRating()) {
            // // best = vv;
            // // }
            // // }
            // // } catch (Throwable ee) {
            // // }
            // // }
            // // }
            // // if (best != null) {
            // // decryptedLinks.add(createLink(best, e.getValue()));
            // // } else {
            // // decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
            // // }
            // // }
            // // if (addWorst3DVideo) {
            // // decryptedLinks.add(createLink(e.getValue().get(e.getValue().size() - 1), e.getValue()));
            // // }
            // // break;
            // default:
            // throw new WTFException();
            // }
            // } else {
            // if (addAllVariantsInUngroupedMode) {
            // decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
            // }
            // }
            // }
            // }
            //
            // if (extra != null && extra.size() > 0 && addExtraVariants) {
            // main: for (VariantInfo v : allVariants.values()) {
            // for (VariantIDStorable s : extra) {
            // // if (s.matches(v.getVariant())) {
            // // String groupID = getGroupingID(v.getVariant());
            // // List<VariantInfo> fromGroup = groups.get(groupID);
            // // decryptedLinks.add(createLink(v, fromGroup));
            // // continue main;
            // // }
            // }
            // }
            //
            // }
            // ArrayList<String> extraSubtitles = cfg.getExtraSubtitles();
            // if (extraSubtitles != null && addExtraSubtitles) {
            // for (String v : extraSubtitles) {
            // if (v != null) {
            // for (VariantInfo vi : allSubtitles) {
            // if (StringUtils.equalsIgnoreCase(vi.getVariant()._getUniqueId(), v)) {
            //
            // decryptedLinks.add(createLink(vi, allSubtitles));
            // }
            // }
            // }
            // }
            //
            // }

        }
        for (

        DownloadLink dl : decryptedLinks)

        {
            dl.setContainerUrl(cryptedLink);
        }
        return decryptedLinks;

    }

    private <T> HashSet<T> createHashSet(List<T> list) {
        HashSet<T> ret = new HashSet<T>();
        if (list != null) {
            ret.addAll(list);
        }
        return ret;
    }

    private Collection<? extends YoutubeClipData> parseGeneric(String cryptedUrl) throws InterruptedException, IOException {
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        if (StringUtils.isNotEmpty(cryptedUrl)) {
            int page = 1;
            int counter = 1;
            while (true) {
                if (this.isAbort()) {
                    throw new InterruptedException();
                }

                // br.getHeaders().put("Cookie", "");
                br.getPage(cryptedUrl);
                checkErrors(br);

                String[] videos = br.getRegex("data\\-video\\-id=\"([^\"]+)").getColumn(0);
                if (videos != null) {
                    for (String id : videos) {

                        if (dupeCheckSet.add(id)) {
                            ret.add(new YoutubeClipData(id, counter++));
                        }
                    }
                }
                if (ret.size() == 0) {
                    videos = br.getRegex("href=\"(/watch\\?v=[A-Za-z0-9\\-_]+)\\&amp;list=[A-Z0-9]+").getColumn(0);
                    if (videos != null) {
                        for (String relativeUrl : videos) {
                            String id = getVideoIDByUrl(relativeUrl);
                            if (dupeCheckSet.add(id)) {
                                ret.add(new YoutubeClipData(id, counter++));
                            }
                        }
                    }
                }
                break;
                // Several Pages: http://www.youtube.com/playlist?list=FL9_5aq5ZbPm9X1QH0K6vOLQ
                // String nextPage = br.getRegex("<a href=\"/playlist\\?list=" + playlistID +
                // "\\&amp;page=(\\d+)\"[^\r\n]+>Next").getMatch(0);
                // if (nextPage != null) {
                // page = Integer.parseInt(nextPage);
                // // anti ddos
                // Thread.sleep(500);
                // } else {
                // break;
                // }
            }

        }
        return ret;
    }

    // protected String getGroupingID(VariantBase v) {
    // String groupID;
    // switch (cfg.getGroupLogic()) {
    // case BY_FILE_TYPE:
    // groupID = v.getFileExtension();
    // break;
    // case NO_GROUP:
    // groupID = v.name();
    // break;
    // case BY_MEDIA_TYPE:
    // groupID = v.getGroup().name();
    // break;
    // default:
    // throw new WTFException("Unknown Grouping");
    // }
    // return groupID;
    // }

    // protected String getGroupingID(AbstractVariant v) {
    // String groupID;
    // switch (cfg.getGroupLogic()) {
    // case BY_FILE_TYPE:
    // groupID = v.getContainer().name();
    // break;
    // case NO_GROUP:
    // groupID = v._getUniqueId();
    // break;
    // case BY_MEDIA_TYPE:
    // if (v instanceof VideoVariant) {
    // groupID = v.getGroup().name() + "_" + ((VideoVariant) v).getProjection();
    // } else {
    // groupID = v.getGroup().name();
    // }
    // break;
    // default:
    // throw new WTFException("Unknown Grouping");
    // }
    // return groupID;
    // }

    private DownloadLink createLink(YoutubeVariantCollection l, VariantInfo variantInfo, List<VariantInfo> alternatives) {
        try {
            System.out.println("Add  Link " + l.getName());
            YoutubeClipData clip = null;
            if (clip == null && variantInfo.getVideoStreams() != null) {
                clip = variantInfo.getVideoStreams().get(0).getClip();

            }
            if (clip == null && variantInfo.getAudioStreams() != null) {
                clip = variantInfo.getAudioStreams().get(0).getClip();

            }
            if (clip == null && variantInfo.getDataStreams() != null) {
                clip = variantInfo.getDataStreams().get(0).getClip();

            }

            boolean hasVariants = false;
            ArrayList<String> altIds = new ArrayList<String>();
            if (alternatives != null) {

                for (VariantInfo vi : alternatives) {

                    if (!StringUtils.equals(variantInfo.getVariant()._getUniqueId(), vi.getVariant()._getUniqueId())) {

                        hasVariants = true;
                    }
                    altIds.add(vi.getVariant().getStorableString());
                }

            }
            DownloadLink thislink;
            thislink = createDownloadlink(YoutubeHelper.createLinkID(clip.videoID, variantInfo.getVariant(), altIds));

            YoutubeHelper helper;
            ClipDataCache.referenceLink(helper = new YoutubeHelper(br, getLogger()), thislink, clip);
            // thislink.setAvailable(true);

            if (cfg.isSetCustomUrlEnabled()) {
                thislink.setCustomURL(getBase() + "/watch?v=" + clip.videoID);
            }
            thislink.setContentUrl(getBase() + "/watch?v=" + clip.videoID + "&variant=" + Encoding.urlEncode(variantInfo.getVariant()._getUniqueId()));

            // thislink.setProperty(key, value)
            thislink.setProperty(YoutubeHelper.YT_ID, clip.videoID);
            thislink.setProperty(YoutubeHelper.YT_COLLECTION, l.getName());

            thislink.setProperty(YoutubeHelper.YT_EXT, variantInfo.getVariant().getContainer().getExtension());

            clip.copyToDownloadLink(thislink);
            // thislink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT_INFO, variantInfo);

            thislink.setVariantSupport(hasVariants);
            thislink.setProperty(YoutubeHelper.YT_VARIANTS, altIds);
            // Object cache = downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANTS, null);
            // thislink.setProperty(YoutubeHelper.YT_VARIANT, variantInfo.getVariant()._getUniqueId());

            YoutubeHelper.writeVariantToDownloadLink(thislink, variantInfo.getVariant());

            // variantInfo.fillExtraProperties(thislink, alternatives);
            String filename;
            thislink.setFinalFileName(filename = helper.createFilename(thislink));

            thislink.setLinkID(YoutubeHelper.createLinkID(clip.videoID, variantInfo.getVariant(), altIds));

            FilePackage fp = FilePackage.getInstance();

            final String fpName = helper.replaceVariables(thislink, helper.getConfig().getPackagePattern());
            // req otherwise returned "" value = 'various', regardless of user settings for various!
            if (StringUtils.isNotEmpty(fpName)) {
                fp.setName(fpName);
                // let the packagizer merge several packages that have the same name
                fp.setProperty("ALLOW_MERGE", true);
                fp.add(thislink);
            }

            return thislink;
        } catch (Exception e) {
            getLogger().log(e);
            return null;
        }

    }

    @Override
    public void setBrowser(Browser brr) {

        if (CFG_YOUTUBE.CFG.isProxyEnabled()) {
            final HTTPProxyStorable proxy = CFG_YOUTUBE.CFG.getProxy();

            if (proxy != null) {
                HTTPProxy prxy = HTTPProxy.getHTTPProxy(proxy);
                if (prxy != null) {
                    this.br.setProxy(prxy);
                } else {

                }
                return;
            }

        }
        super.setBrowser(brr);

    }

    /**
     * Parse a playlist id and return all found video ids
     *
     * @param decryptedLinks
     * @param dupeCheckSet
     * @param base
     * @param playlistID
     * @param videoIdsToAdd
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public ArrayList<YoutubeClipData> parsePlaylist(final String playlistID) throws IOException, InterruptedException {
        // this returns the html5 player
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        if (StringUtils.isNotEmpty(playlistID)) {
            Browser pbr = new Browser();
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("youtube.com");
            JDUtilities.getPluginForHost("mediafire.com");
            br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
            br.getHeaders().put("Accept-Charset", null);
            br.getPage(getBase() + "/playlist?list=" + playlistID);

            final String yt_page_cl = br.getRegex("'PAGE_CL': (\\d+)").getMatch(0);
            final String yt_page_ts = br.getRegex("'PAGE_BUILD_TIMESTAMP': \"(.*?)\"").getMatch(0);
            pbr = br.cloneBrowser();
            int counter = 1;
            int round = 0;
            while (true) {
                if (this.isAbort()) {
                    throw new InterruptedException();
                }

                checkErrors(pbr);
                String[] videos = pbr.getRegex("href=(\"|')(/watch\\?v=[A-Za-z0-9\\-_]+.*?)\\1").getColumn(1);
                int before = dupeCheckSet.size();
                if (videos != null) {
                    for (String relativeUrl : videos) {
                        if (relativeUrl.contains("list=" + playlistID)) {
                            String id = getVideoIDByUrl(relativeUrl);
                            if (dupeCheckSet.add(id)) {
                                ret.add(new YoutubeClipData(id, counter++));
                            }
                        }
                    }
                }
                if (dupeCheckSet.size() == before) {
                    // no videos in the last round. we are probably done here
                    break;
                }
                // Several Pages: http://www.youtube.com/playlist?list=FL9_5aq5ZbPm9X1QH0K6vOLQ
                String jsonPage = pbr.getRegex("/browse_ajax\\?action_continuation=\\d+&amp;continuation=[a-zA-Z0-9%]+").getMatch(-1);
                String nextPage = pbr.getRegex("<a href=(\"|')(/playlist\\?list=" + playlistID + "\\&amp;page=\\d+)\\1[^\r\n]+>Next").getMatch(1);
                if (jsonPage != null) {
                    jsonPage = HTMLEntities.unhtmlentities(jsonPage);
                    pbr = br.cloneBrowser();
                    if (yt_page_cl != null) {
                        pbr.getHeaders().put("X-YouTube-Page-CL", yt_page_cl);
                    }
                    if (yt_page_ts != null) {
                        pbr.getHeaders().put("X-YouTube-Page-Timestamp", yt_page_ts);
                    }
                    // anti ddos
                    round = antiDdosSleep(round);
                    pbr.getPage(jsonPage);
                    String output = pbr.toString().replace("\\n", " ");
                    output = jd.nutils.encoding.Encoding.unescapeYoutube(output);
                    output = output.replaceAll("[ ]{2,}", "");
                    pbr.getRequest().setHtmlCode(output);
                } else if (nextPage != null) {
                    // OLD! doesn't always present. Depends on server playlist backend code.!
                    nextPage = HTMLEntities.unhtmlentities(nextPage);
                    round = antiDdosSleep(round);
                    pbr.getPage(nextPage);
                } else {
                    break;
                }
            }

        }
        return ret;
    }

    /**
     * @param round
     * @return
     * @throws InterruptedException
     */
    protected int antiDdosSleep(int round) throws InterruptedException {
        Thread.sleep((DDOS_WAIT_MAX * (Math.min(DDOS_INCREASE_FACTOR, round++))) / DDOS_INCREASE_FACTOR);
        return round;
    }

    public ArrayList<YoutubeClipData> parseChannelgrid(String channelID) throws IOException, InterruptedException {
        // http://www.youtube.com/user/Gronkh/videos
        // channel: http://www.youtube.com/channel/UCYJ61XIK64sp6ZFFS8sctxw
        Browser li = br.cloneBrowser();
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        int counter = 1;
        int round = 0;
        if (StringUtils.isNotEmpty(channelID)) {
            String pageUrl = null;
            while (true) {
                round++;
                if (this.isAbort()) {
                    throw new InterruptedException();
                }
                String content = null;
                if (pageUrl == null) {
                    // this returns the html5 player
                    br.getPage(getBase() + "/channel/" + channelID + "/videos?view=0");
                    checkErrors(br);
                    content = br.toString();
                } else {
                    li = br.cloneBrowser();
                    li.getPage(pageUrl);
                    checkErrors(li);
                    content = jd.nutils.encoding.Encoding.unescapeYoutube(li.toString());
                }

                String[] videos = new Regex(content, "href=\"(/watch\\?v=[A-Za-z0-9\\-_]+)").getColumn(0);
                if (videos != null) {
                    for (String relativeUrl : videos) {
                        String id = getVideoIDByUrl(relativeUrl);
                        if (dupeCheckSet.add(id)) {
                            ret.add(new YoutubeClipData(id, counter++));

                        }
                    }
                }
                // Several Pages: http://www.youtube.com/playlist?list=FL9_5aq5ZbPm9X1QH0K6vOLQ
                String nextPage = Encoding.htmlDecode(new Regex(content, "data-uix-load-more-href=\"(/[^<>\"]*?)\"").getMatch(0));

                if (nextPage != null) {
                    pageUrl = getBase() + nextPage;
                    // anti ddos
                    round = antiDdosSleep(round);
                } else {
                    break;
                }
            }

        }
        return ret;
    }

    public ArrayList<YoutubeClipData> parseUsergrid(String userID) throws IOException, InterruptedException {
        // http://www.youtube.com/user/Gronkh/videos
        // channel: http://www.youtube.com/channel/UCYJ61XIK64sp6ZFFS8sctxw
        if (false && userID != null) {
            /** TEST CODE for 1050 playlist max size issue. below comment is incorrect, both grid and channelid return 1050. raztoki **/
            // this format only ever returns 1050 results, its a bug on youtube end. We can resolve this by finding the youtube id and let
            // parseChannelgrid(channelid) find the results.
            ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
            Browser li = br.cloneBrowser();
            li.getPage(getBase() + "/user/" + userID + "/videos?view=0");
            this.channelID = li.getRegex("'CHANNEL_ID', \"(UC[^\"]+)\"").getMatch(0);
            if (StringUtils.isNotEmpty(this.channelID)) {
                return ret;
            }
        }

        Browser li = br.cloneBrowser();
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        int counter = 1;
        if (StringUtils.isNotEmpty(userID)) {
            String pageUrl = null;
            int round = 0;
            while (true) {
                if (this.isAbort()) {
                    throw new InterruptedException();
                }
                String content = null;
                if (pageUrl == null) {
                    // this returns the html5 player
                    br.getPage(getBase() + "/user/" + userID + "/videos?view=0");

                    checkErrors(br);
                    content = br.toString();
                } else {
                    try {
                        li = br.cloneBrowser();
                        li.getPage(pageUrl);
                    } catch (final BrowserException b) {
                        if (li.getHttpConnection() != null && li.getHttpConnection().getResponseCode() == 400) {
                            logger.warning("Youtube issue!");
                            return ret;
                        } else {
                            throw b;
                        }
                    }
                    checkErrors(li);
                    content = jd.nutils.encoding.Encoding.unescapeYoutube(li.toString());
                }

                String[] videos = new Regex(content, "href=\"(/watch\\?v=[A-Za-z0-9\\-_]+)").getColumn(0);
                if (videos != null) {
                    for (String relativeUrl : videos) {
                        String id = getVideoIDByUrl(relativeUrl);
                        if (dupeCheckSet.add(id)) {
                            ret.add(new YoutubeClipData(id, counter++));

                        }
                    }
                }
                // Several Pages: http://www.youtube.com/playlist?list=FL9_5aq5ZbPm9X1QH0K6vOLQ
                String nextPage = Encoding.htmlDecode(new Regex(content, "data-uix-load-more-href=\"(/[^<>\"]+)\"").getMatch(0));

                if (nextPage != null) {
                    pageUrl = getBase() + nextPage;
                    round = antiDdosSleep(round);
                } else {
                    break;
                }
            }

        }
        return ret;
    }

    /**
     * parses 'video_ids=' array, primarily used with watch_videos link
     */
    public ArrayList<YoutubeClipData> parseVideoIds(String video_ids) throws IOException, InterruptedException {
        // /watch_videos?title=Trending&video_ids=0KSOMA3QBU0,uT3SBzmDxGk,X7Xf8DsTWgs,72WhEqeS6AQ,Qc9c12q3mrc,6l7J1i1OkKs,zeu2tI-tqvs,o3mP3mJDL2k,jYdaQJzcAcw&feature=c4-overview&type=0&more_url=
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        int counter = 1;
        if (StringUtils.isNotEmpty(video_ids)) {
            String[] videos = new Regex(video_ids, "([A-Za-z0-9\\-_]+)").getColumn(0);
            if (videos != null) {
                for (String vid : videos) {
                    if (dupeCheckSet.add(vid)) {
                        ret.add(new YoutubeClipData(vid, counter++));
                    }
                }
            }
        }
        return ret;
    }

    private void checkErrors(Browser br) throws InterruptedException {
        if (br.containsHTML(">404 Not Found<")) {
            throw new InterruptedException("404 Not Found");
        } else if (br.containsHTML("iframe style=\"display:block;border:0;\" src=\"/error")) {
            throw new InterruptedException("Unknown Error");
        } else if (br.containsHTML("<h2>\\s*This channel does not exist\\.\\s*</h2>")) {
            throw new InterruptedException("Channel does not exist.");
        }

    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 100);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}