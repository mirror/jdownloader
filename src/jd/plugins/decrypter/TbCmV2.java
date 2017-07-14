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
import java.util.Map.Entry;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
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
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.YoutubeVariantCollection;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioInterface;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
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
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youtube.com", "youtube.com", "youtube.com" }, urls = { "https?://([a-z]+\\.)?yt\\.not\\.allowed/.+", "https?://([a-z]+\\.)?youtube\\.com/(embed/|.*?watch.*?v(%3D|=)|view_play_list\\?p=|playlist\\?(p|list)=|.*?g/c/|.*?grid/user/|v/|user/|channel/|c/|course\\?list=)[A-Za-z0-9\\-_]+(.*?page=\\d+)?(.*?list=[A-Za-z0-9\\-_]+)?(\\#variant=\\S++)?|watch_videos\\?.*?video_ids=.+", "https?://youtube\\.googleapis\\.com/(v/|user/|channel/|c/)[A-Za-z0-9\\-_]+(\\#variant=\\S+)?" })
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

    private HashSet<String>         dupeCheckSet;
    private YoutubeConfig           cfg;
    private static Object           DIALOGLOCK = new Object();
    private String                  videoID;
    private String                  watch_videos;
    private String                  playlistID;
    private String                  channelID;
    private String                  userID;
    private AbstractVariant         requestedVariant;
    private HashMap<String, Object> globalPropertiesForDownloadLink;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        // nullify, for debugging purposes!
        videoID = null;
        watch_videos = null;
        playlistID = null;
        channelID = null;
        userID = null;
        dupeCheckSet = new HashSet<String>();
        globalPropertiesForDownloadLink = new HashMap<String, Object>();
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
        br = new Browser();
        br.setFollowRedirects(true);
        br.setCookie("http://youtube.com", "PREF", "hl=en-GB");
        String cleanedurl = Encoding.urlDecode(cryptedLink, false);
        cleanedurl = cleanedurl.replace("youtube.jd", "youtube.com");
        String requestedVariantString = new Regex(cleanedurl, "\\#variant=(\\S*)").getMatch(0);
        if (StringUtils.isNotEmpty(requestedVariantString)) {
            requestedVariant = AbstractVariant.get(Base64.decodeToString(requestedVariantString));
        }
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
        /*
         * you can not use this with /c or /channel based urls, it will pick up false positives. see
         * https://www.youtube.com/channel/UCOSGEokQQcdAVFuL_Aq8dlg, it will find list=PLc-T0ryHZ5U_FtsfHQopuvQugBvRoVR3j which only
         * contains 27 videos not the entire channels 112
         */
        if (!cleanedurl.matches(".+youtube\\.com/(?:channel/|c/).+")) {
            playlistID = getListIDByUrls(cleanedurl);
        }
        String userChannel = new Regex(cleanedurl, "/c/([A-Za-z0-9\\-_]+)").getMatch(0);
        userID = new Regex(cleanedurl, "/user/([A-Za-z0-9\\-_]+)").getMatch(0);
        channelID = new Regex(cleanedurl, "/channel/([A-Za-z0-9\\-_]+)").getMatch(0);
        if (StringUtils.isEmpty(channelID) && StringUtils.isNotEmpty(userChannel)) {
            br.getPage("https://www.youtube.com/c/" + userChannel);
            channelID = br.getRegex("/channel/(UC[A-Za-z0-9\\-_]+)/videos").getMatch(0);
            if (StringUtils.isEmpty(channelID)) {
                // its within meta tags multiple times (ios/ipad/iphone) also
                channelID = br.getRegex("<meta itemprop=\"channelId\" content=\"(UC[A-Za-z0-9\\-_]+)\"").getMatch(0);
            }
        }
        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_PLAYLIST_ID, playlistID);
        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_CHANNEL_ID, channelID);
        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_USER_ID, userID);
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
        boolean reversePlaylistNumber = false;
        try {
            Boolean userWorkaround = null;
            Boolean channelWorkaround = null;
            if (StringUtils.isNotEmpty(userID) && StringUtils.isEmpty(playlistID)) {
                /*
                 * the user channel parser only parses 1050 videos. this workaround finds the user channel playlist and parses this playlist
                 * instead
                 */
                br.getPage("https://www.youtube.com/user/" + userID + "/featured");
                // channel title isn't user_name. user_name is /user/ reference. check logic in YoutubeHelper.extractData()!
                globalPropertiesForDownloadLink.put(YoutubeHelper.YT_CHANNEL_TITLE, extractWebsiteTitle());
                globalPropertiesForDownloadLink.put(YoutubeHelper.YT_USER_NAME, userID);
                playlistID = br.getRegex(">Uploads</span>.*?list=([A-Za-z0-9\\-_]+)\".+?play-all-icon-btn").getMatch(0);
                userWorkaround = Boolean.valueOf(StringUtils.isNotEmpty(playlistID));
            }
            if (StringUtils.isNotEmpty(channelID) && StringUtils.isEmpty(playlistID)) {
                /*
                 * you can not use this with /c or /channel based urls, it will pick up false positives. see
                 * https://www.youtube.com/channel/UCOSGEokQQcdAVFuL_Aq8dlg, it will find list=PLc-T0ryHZ5U_FtsfHQopuvQugBvRoVR3j which only
                 * contains 27 videos not the entire channels 112
                 */
                if (!cleanedurl.matches(".+youtube\\.com/(?:channel/|c/).+")) {
                    /*
                     * the user channel parser only parses 1050 videos. this workaround finds the user channel playlist and parses this
                     * playlist instead
                     */
                    br.getPage("https://www.youtube.com/channel/" + channelID);
                    playlistID = br.getRegex("list=([A-Za-z0-9\\-_]+)\"[^<>]+play-all-icon-btn").getMatch(0);
                }
                if (StringUtils.isEmpty(playlistID) && channelID.startsWith("UC")) {
                    // channel has no play all button.
                    // like https://www.youtube.com/channel/UCbmRs17gtQxFXQyvIo5k6Ag/feed
                    playlistID = "UU" + channelID.substring(2);
                }
                channelWorkaround = Boolean.valueOf(StringUtils.isNotEmpty(playlistID));
            }
            ArrayList<YoutubeClipData> playlist;
            videoIdsToAdd.addAll(playlist = parsePlaylist(playlistID));
            if (Boolean.TRUE.equals(channelWorkaround) && playlist.size() == 0) {
                // failed
                channelWorkaround = Boolean.FALSE;
            }
            if (Boolean.TRUE.equals(userWorkaround) && playlist.size() == 0) {
                // failed
                userWorkaround = Boolean.FALSE;
            }
            if (Boolean.FALSE.equals(channelWorkaround)) {
                videoIdsToAdd.addAll(parseChannelgrid(channelID));
            }
            if (Boolean.FALSE.equals(userWorkaround)) {
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
            // /user/username/videos and /channel/[a-zA-Z0-9_-]+/videos are inverted (newest to oldest), we should always return oldest >
            // newest so playlist counter is correct.
            // userworkaround == true == newest to oldest
            // channelworkaround == true == newest to oldest.
            if (Boolean.TRUE.equals(userWorkaround) || Boolean.TRUE.equals(channelWorkaround)) {
                Collections.reverse(videoIdsToAdd);
                reversePlaylistNumber = true;
            }
        } catch (InterruptedException e) {
            return decryptedLinks;
        }
        for (YoutubeClipData vid : videoIdsToAdd) {
            if (this.isAbort()) {
                throw new InterruptedException();
            }
            try {
                // make sure that we reload the video
                boolean hasCache = ClipDataCache.hasCache(helper, vid.videoID);
                try {
                    YoutubeClipData old = vid;
                    vid = ClipDataCache.get(helper, vid.videoID);
                    vid.playlistEntryNumber = reversePlaylistNumber ? videoIdsToAdd.size() - old.playlistEntryNumber + 1 : old.playlistEntryNumber;
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
            if (!cfg.isExternMultimediaToolUsageEnabled()) {
                getLogger().info("isDashEnabledEnabled=false");
            }
            final List<AbstractVariant> enabledVariants = new ArrayList<AbstractVariant>(AbstractVariant.listVariants());
            final HashSet<VariantGroup> enabledVariantGroups = new HashSet<VariantGroup>();
            {
                // nest this, so we don't have variables table full of entries that get called only once
                final List<VariantIDStorable> disabled = CFG_YOUTUBE.CFG.getDisabledVariants();
                final HashSet<String> disabledIds = new HashSet<String>();
                if (disabled != null) {
                    for (VariantIDStorable v : disabled) {
                        disabledIds.add(v.createUniqueID());
                    }
                }
                final HashSet<AudioBitrate> disabledAudioBitrates = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedAudioBitrates());
                final HashSet<AudioCodec> disabledAudioCodecs = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedAudioCodecs());
                final HashSet<FileContainer> disabledFileContainers = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedFileContainers());
                final HashSet<VariantGroup> disabledGroups = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedGroups());
                final HashSet<Projection> disabledProjections = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedProjections());
                final HashSet<VideoResolution> disabledResolutions = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedResolutions());
                final HashSet<VideoCodec> disabledVideoCodecs = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedVideoCodecs());
                final HashSet<VideoFrameRate> disabledFramerates = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedVideoFramerates());
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
            }
            // write all available variants to groups and allVariants
            List<VariantInfo> variants = vid.findVariants();
            VideoVariant bestVideoResolution = null;
            for (VariantInfo vi : variants) {
                if (vi.getVariant() instanceof VideoVariant) {
                    if (bestVideoResolution == null || bestVideoResolution.getVideoHeight() < ((VideoVariant) vi.getVariant()).getVideoHeight()) {
                        bestVideoResolution = (VideoVariant) vi.getVariant();
                    }
                }
            }
            vid.bestVideoItag = bestVideoResolution;
            List<VariantInfo> subtitles = enabledVariantGroups.contains(VariantGroup.SUBTITLES) ? vid.findSubtitleVariants() : new ArrayList<VariantInfo>();
            ArrayList<VariantInfo> descriptions = enabledVariantGroups.contains(VariantGroup.DESCRIPTION) ? vid.findDescriptionVariant() : new ArrayList<VariantInfo>();
            if (subtitles != null) {
                variants.addAll(subtitles);
            }
            if (descriptions != null) {
                variants.addAll(descriptions);
            }
            List<YoutubeVariantCollection> links = YoutubeVariantCollection.load();
            if (requestedVariant != null) {
                // create a dummy collection
                links = new ArrayList<YoutubeVariantCollection>();
                ArrayList<VariantIDStorable> varList = new ArrayList<VariantIDStorable>();
                varList.add(new VariantIDStorable(requestedVariant));
                links.add(new YoutubeVariantCollection("Dummy", varList));
            }
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
            if (CFG_YOUTUBE.CFG.isCollectionMergingEnabled()) {
                for (YoutubeVariantCollection l : links) {
                    if (!l.isEnabled()) {
                        continue;
                    }
                    ArrayList<VariantInfo> linkVariants = new ArrayList<VariantInfo>();
                    ArrayList<VariantInfo> cutLinkVariantsDropdown = new ArrayList<VariantInfo>();
                    HashSet<String> customAlternateSet = l.createUniqueIDSetForDropDownList();
                    if (customAlternateSet.size() > 0) {
                        for (VariantInfo v : variants) {
                            VariantIDStorable vi = storables.get(v);
                            if (customAlternateSet.contains(vi.createUniqueID())) {
                                if (allowedVariantsSet.contains(vi.createUniqueID())) {
                                    cutLinkVariantsDropdown.add(v);
                                    helper.extendedDataLoading(v, variants);
                                }
                            }
                        }
                    }
                    if (StringUtils.isNotEmpty(l.getGroupingID())) {
                        for (VariantInfo v : variants) {
                            VariantIDStorable vi = storables.get(v);
                            if (StringUtils.equals(l.getGroupingID(), vi.createGroupingID())) {
                                if (allowedVariantsSet.contains(vi.createUniqueID())) {
                                    linkVariants.add(v);
                                    helper.extendedDataLoading(v, variants);
                                }
                            } else if (StringUtils.equals(l.getGroupingID(), vi.getContainer())) {
                                if (allowedVariantsSet.contains(vi.createUniqueID())) {
                                    linkVariants.add(v);
                                    helper.extendedDataLoading(v, variants);
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
                                    helper.extendedDataLoading(v, variants);
                                }
                            }
                        }
                    } else {
                        continue;
                    }
                    Collections.sort(cutLinkVariantsDropdown, new Comparator<VariantInfo>() {

                        @Override
                        public int compare(VariantInfo o1, VariantInfo o2) {
                            return o2.compareTo(o1);
                        }
                    });
                    Collections.sort(linkVariants, new Comparator<VariantInfo>() {

                        @Override
                        public int compare(VariantInfo o1, VariantInfo o2) {
                            return o2.compareTo(o1);
                        }
                    });
                    // remove dupes
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
                    last = null;
                    for (final Iterator<VariantInfo> it = cutLinkVariantsDropdown.iterator(); it.hasNext();) {
                        VariantInfo cur = it.next();
                        if (last != null) {
                            if (StringUtils.equals(cur.getVariant().getTypeId(), last.getVariant().getTypeId())) {
                                it.remove();
                                continue;
                            }
                        }
                        last = cur;
                    }
                    // for (final Iterator<VariantInfo> it = linkVariants.iterator(); it.hasNext();) {
                    // VariantInfo cur = it.next();
                    // System.out.println(cur.getVariant().getBaseVariant() + "\t" + cur.getVariant().getQualityRating());
                    // }
                    if (linkVariants.size() > 0) {
                        DownloadLink lnk = createLink(l, linkVariants.get(0), cutLinkVariantsDropdown.size() > 0 ? cutLinkVariantsDropdown : linkVariants);
                        decryptedLinks.add(lnk);
                        if (linkVariants.get(0).getVariant().getGroup() == VariantGroup.SUBTITLES) {
                            ArrayList<String> extras = CFG_YOUTUBE.CFG.getExtraSubtitles();
                            if (extras != null) {
                                for (String s : extras) {
                                    if (s != null) {
                                        VariantInfo lng = null;
                                        for (VariantInfo vi : linkVariants) {
                                            if (vi.getVariant() instanceof SubtitleVariant) {
                                                if (StringUtils.equalsIgnoreCase(((SubtitleVariant) vi.getVariant()).getGenericInfo().getLanguage(), s)) {
                                                    lng = vi;
                                                    break;
                                                }
                                            }
                                        }
                                        if (lng != null) {
                                            lnk = createLink(l, lng, cutLinkVariantsDropdown.size() > 0 ? cutLinkVariantsDropdown : linkVariants);
                                            decryptedLinks.add(lnk);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ArrayList<VariantInfo> linkVariants = new ArrayList<VariantInfo>();
                for (VariantInfo v : variants) {
                    VariantIDStorable vi = storables.get(v);
                    if (allowedVariantsSet.contains(vi.createUniqueID())) {
                        linkVariants.add(v);
                        helper.extendedDataLoading(v, variants);
                    }
                }
                Collections.sort(linkVariants, new Comparator<VariantInfo>() {

                    @Override
                    public int compare(VariantInfo o1, VariantInfo o2) {
                        return o2.compareTo(o1);
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
                for (VariantInfo vi : linkVariants) {
                    ArrayList<VariantInfo> lst = new ArrayList<VariantInfo>();
                    lst.add(vi);
                    DownloadLink lnk = createLink(new YoutubeVariantCollection(), vi, lst);
                    decryptedLinks.add(lnk);
                }
            }
        }
        for (final DownloadLink dl : decryptedLinks) {
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
            thislink.setContentUrl(getBase() + "/watch?v=" + clip.videoID + "#variant=" + Encoding.urlEncode(Base64.encode(variantInfo.getVariant().getStorableString())));
            // thislink.setProperty(key, value)
            thislink.setProperty(YoutubeHelper.YT_ID, clip.videoID);
            thislink.setProperty(YoutubeHelper.YT_COLLECTION, l.getName());
            for (Entry<String, Object> es : globalPropertiesForDownloadLink.entrySet()) {
                if (es.getKey() != null) {
                    thislink.setProperty(es.getKey(), es.getValue());
                }
            }
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
            // firefox gets different result than chrome! lets hope switching wont cause issue.
            br.getHeaders().put("User-Agent", UserAgents.stringUserAgent(BrowserName.Chrome));
            br.getHeaders().put("Accept-Charset", null);
            br.getPage(getBase() + "/playlist?list=" + playlistID);
            // user list it's not a playlist.... just a channel decryption. this can return incorrect information.
            globalPropertiesForDownloadLink.put(YoutubeHelper.YT_PLAYLIST_TITLE, extractWebsiteTitle());
            final String PAGE_CL = br.getRegex("'PAGE_CL': (\\d+)").getMatch(0);
            final String PAGE_BUILD_LABEL = br.getRegex("'PAGE_BUILD_LABEL': \"(.*?)\"").getMatch(0);
            final String VARIANTS_CHECKSUM = br.getRegex("'VARIANTS_CHECKSUM': \"(.*?)\"").getMatch(0);
            final String INNERTUBE_CONTEXT_CLIENT_VERSION = br.getRegex("INNERTUBE_CONTEXT_CLIENT_VERSION: \"(.*?)\"").getMatch(0);
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
                    if (PAGE_CL != null) {
                        pbr.getHeaders().put("X-YouTube-Page-CL", PAGE_CL);
                    }
                    if (PAGE_BUILD_LABEL != null) {
                        pbr.getHeaders().put("X-YouTube-Page-Label", PAGE_BUILD_LABEL);
                    }
                    if (VARIANTS_CHECKSUM != null) {
                        pbr.getHeaders().put("X-YouTube-Variants-Checksum", VARIANTS_CHECKSUM);
                    }
                    if (INNERTUBE_CONTEXT_CLIENT_VERSION != null) {
                        pbr.getHeaders().put("X-YouTube-Client-Version", INNERTUBE_CONTEXT_CLIENT_VERSION);
                    }
                    // anti ddos
                    round = antiDdosSleep(round);
                    pbr.getPage(jsonPage);
                    String output = pbr.toString();
                    output = Encoding.unicodeDecode(output);
                    output = output.replaceAll("\\s+", " ");
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

    protected String extractWebsiteTitle() {
        return br.getRegex("<meta name=\"title\"\\s+[^<>]*content=\"(.*?)(?:\\s*-\\s*Youtube\\s*)?\"").getMatch(0);
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
                    content = Encoding.unicodeDecode(li.toString());
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
                    content = Encoding.unicodeDecode(li.toString());
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