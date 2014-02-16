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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.YoutubeClipData;
import jd.plugins.components.YoutubeITAG;
import jd.plugins.components.YoutubeStreamData;
import jd.plugins.components.YoutubeSubtitleInfo;
import jd.plugins.components.YoutubeVariant;
import jd.plugins.components.YoutubeVariantInterface;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig.GroupLogic;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig.IfUrlisAVideoAndPlaylistAction;
import jd.utils.locale.JDL;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youtube.com", "youtube.com" }, urls = { "https?://([a-z]+\\.)?youtube\\.com/(embed/|.*?watch.*?v(%3D|=)|view_play_list\\?p=|playlist\\?(p|list)=|.*?g/c/|.*?grid/user/|v/|user/|channel/|course\\?list=)[A-Za-z0-9\\-_]+(.*?page=\\d+)?(.*?list=[A-Za-z0-9\\-_]+)?", "https?://youtube\\.googleapis\\.com/(v/|user/|channel/)[A-Za-z0-9\\-_]+" }, flags = { 0, 0 })
public class TbCmV2 extends PluginForDecrypt {

    public TbCmV2(PluginWrapper wrapper) {
        super(wrapper);
    };

    /**
     * Returns host from provided String.
     */
    static String getBase() {
        YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
        boolean prefers = cfg.isPreferHttpsEnabled();

        if (prefers) {
            return "https://www.youtube.com";
        } else {
            return "http://www.youtube.com";
        }

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
            vuid = new Regex(URL, "(v|embed)/([A-Za-z0-9\\-_]+)").getMatch(1);
        }
        return vuid;
    }

    private final String                playListRegex   = "(\\?|&)(play)?list=[A-Za-z0-9\\-_]+";
    private final String                courseListRegex = "/course\\?list=[A-Za-z0-9\\-_]+";
    private final String                userListRegex   = "/user/[A-Za-z0-9\\-_]+";

    private final LinkedHashSet<String> dupeList        = new LinkedHashSet<String>();
    private HashSet<String>             dupeCheckSet;

    private YoutubeConfig               cfg;
    private YoutubeHelper               cachedHelper;

    public class VariantInfo implements Comparable<VariantInfo> {

        protected final YoutubeVariantInterface variant;
        private final YoutubeStreamData         audioStream;
        private final YoutubeStreamData         videoStream;
        public String                           special = "";
        private final YoutubeStreamData         data;

        public YoutubeStreamData getData() {
            return data;
        }

        @Override
        public String toString() {
            return getIdentifier();
        }

        public String getIdentifier() {
            return variant.getUniqueId();
        }

        public VariantInfo(YoutubeVariantInterface v, YoutubeStreamData audio, YoutubeStreamData video, YoutubeStreamData data) {
            this.variant = v;
            this.audioStream = audio;
            this.videoStream = video;
            this.data = data;
        }

        public void fillExtraProperties(DownloadLink thislink, List<VariantInfo> alternatives) {
        }

        @Override
        public int compareTo(VariantInfo o) {
            return new Double(o.variant.getQualityRating()).compareTo(new Double(variant.getQualityRating()));

        }

    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        cfg = PluginJsonConfig.get(YoutubeConfig.class);

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

        String cleanedurl = Encoding.urlDecode(param.toString(), false);
        cleanedurl = cleanedurl.replace("youtube.jd", "youtube.com");
        String videoID = getVideoIDByUrl(cleanedurl);

        String playlistID = getListIDByUrls(cleanedurl);
        String userID = new Regex(cleanedurl, "/user/([A-Za-z0-9\\-_]+)").getMatch(0);
        String channelID = new Regex(cleanedurl, "/channel/([A-Za-z0-9\\-_]+)").getMatch(0);
        // Some Stable errorhandling

        ArrayList<String[]> linkstodecrypt = new ArrayList<String[]>();
        YoutubeHelper helper = getCachedHelper();

        helper.login(false, false);

        // Check if link contains a video and a playlist

        IfUrlisAVideoAndPlaylistAction action = cfg.getLinkIsVideoAndPlaylistUrlAction();
        if (StringUtils.isNotEmpty(playlistID) && StringUtils.isNotEmpty(videoID)) {

            if (action == IfUrlisAVideoAndPlaylistAction.ASK) {
                ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, cleanedurl, JDL.L("plugins.host.youtube.isvideoandplaylist.question.message", "The Youtube link contains a video and a playlist. What do you want do download?"), null, JDL.L("plugins.host.youtube.isvideoandplaylist.question.onlyvideo", "Only video"), JDL.L("plugins.host.youtube.isvideoandplaylist.question.playlist", "Complete playlist")) {
                    @Override
                    public ModalityType getModalityType() {
                        return ModalityType.MODELESS;
                    }
                };
                try {
                    UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                    action = IfUrlisAVideoAndPlaylistAction.VIDEO_ONLY;
                } catch (DialogCanceledException e) {
                    action = IfUrlisAVideoAndPlaylistAction.PLAYLIST_ONLY;
                } catch (DialogClosedException e) {
                    action = IfUrlisAVideoAndPlaylistAction.NOTHING;
                }

            }
            switch (action) {

            case PLAYLIST_ONLY:
                // videoID = null;
                break;
            case VIDEO_ONLY:
                playlistID = null;
                break;
            default:
                return decryptedLinks;
            }

        }
        ArrayList<YoutubeClipData> videoIdsToAdd = new ArrayList<YoutubeClipData>();
        try {

            videoIdsToAdd.addAll(parsePlaylist(playlistID));
            // some unknown playlist type?
            if (videoIdsToAdd.size() == 0 && StringUtils.isNotEmpty(playlistID)) {
                videoIdsToAdd.addAll(parseGeneric(cleanedurl));
            }
            videoIdsToAdd.addAll(parseUsergrid(userID));
            videoIdsToAdd.addAll(parseChannelgrid(channelID));
            if (StringUtils.isNotEmpty(videoID) && dupeCheckSet.add(videoID)) {
                videoIdsToAdd.add(new jd.plugins.components.YoutubeClipData(videoID));
            }
            if (videoIdsToAdd.size() == 0) {
                videoIdsToAdd.addAll(parseGeneric(cleanedurl));
            }

        } catch (InterruptedException e) {
            return decryptedLinks;
        }

        // HashSet<YoutubeVariantInterface> blacklistedVariants = new HashSet<YoutubeVariantInterface>();
        HashSet<String> blacklistedStrings = new HashSet<String>();
        HashSet<String> extraStrings = new HashSet<String>();

        String[] blacklist = cfg.getBlacklistedVariants();
        if (blacklist != null) {

            for (String ytv : blacklist) {

                YoutubeVariantInterface v = helper.getVariantById(ytv);
                // if (v != null) {
                // blacklistedVariants.add(v);
                // }

                blacklistedStrings.add(ytv);
            }
        }
        String[] extra = cfg.getExtraVariants();
        if (extra != null) {
            for (String s : extra) {
                extraStrings.add(s);
            }
        }
        for (YoutubeClipData vid : videoIdsToAdd) {
            HashMap<String, List<VariantInfo>> groups = new HashMap<String, List<VariantInfo>>();
            HashMap<String, List<VariantInfo>> groupsExcluded = new HashMap<String, List<VariantInfo>>();
            HashMap<YoutubeVariantInterface, VariantInfo> allVariants = new HashMap<YoutubeVariantInterface, VariantInfo>();
            HashMap<String, VariantInfo> idMap = new HashMap<String, VariantInfo>();
            Map<YoutubeITAG, YoutubeStreamData> vc = helper.loadVideo(vid);
            if (vc == null || StringUtils.isNotEmpty(vid.error)) {
                getLogger().info("Error on " + vid.videoID + " (" + vid.title + "): " + vid.error);
            }
            if (vc == null) continue;

            for (YoutubeVariantInterface v : helper.getVariants()) {
                System.out.println("test for " + v);
                String groupID = getGroupID(v);

                YoutubeStreamData audio = null;
                YoutubeStreamData video = null;
                YoutubeStreamData data = null;
                boolean valid = v.getiTagVideo() != null || v.getiTagAudio() != null || v.getiTagData() != null;

                if (v.getiTagVideo() != null) {
                    video = vc.get(v.getiTagVideo());
                    if (video == null) valid = false;
                }
                if (v.getiTagAudio() != null) {
                    audio = vc.get(v.getiTagAudio());
                    if (audio == null) valid = false;
                }
                if (v.getiTagData() != null) {
                    data = vc.get(v.getiTagData());
                    if (data == null) valid = false;
                }

                if (valid) {
                    VariantInfo vi = new VariantInfo(v, audio, video, data);
                    if ((blacklistedStrings.contains(v.getTypeId()) || blacklistedStrings.contains(v.getUniqueId())) && !extraStrings.contains(v.getTypeId()) && !extraStrings.contains(v.getUniqueId())) {
                        logger.info("Variant blacklisted:" + v);
                        List<VariantInfo> list = groupsExcluded.get(groupID);
                        if (list == null) {
                            list = new ArrayList<TbCmV2.VariantInfo>();
                            groupsExcluded.put(groupID, list);
                        }
                        list.add(vi);

                    } else {
                        // if we have several variants with the same id, use the one with the highest rating.
                        // example: mp3 conversion can be done from a high and lower video. audio is the same. we should prefer the lq video
                        VariantInfo mapped = idMap.get(v.getTypeId());

                        if (mapped == null || v.getQualityRating() > mapped.variant.getQualityRating()) {
                            idMap.put(v.getTypeId(), vi);
                            // remove old mapping
                            if (mapped != null) {
                                getLogger().info("Removed Type Dupe: " + mapped);
                                String mappedGroupID = getGroupID(mapped.variant);
                                List<VariantInfo> list = groups.get(mappedGroupID);
                                if (list != null) list.remove(mapped);

                                allVariants.remove(mapped.variant);
                            }

                        } else {
                            // we already have a better quality of this variant id
                            continue;
                        }

                        List<VariantInfo> list = groups.get(groupID);
                        if (list == null) {
                            list = new ArrayList<TbCmV2.VariantInfo>();
                            groups.put(groupID, list);
                        }

                        list.add(vi);
                    }
                    System.out.println("Variant found " + v);

                    allVariants.put(v, vi);
                } else {

                    System.out.println("Variant NOT found " + v);
                }

            }

            List<VariantInfo> allSubtitles = new ArrayList<VariantInfo>();
            if (cfg.isSubtitlesEnabled()) {
                ArrayList<YoutubeSubtitleInfo> subtitles = helper.loadSubtitles(vid);

                ArrayList<String> whitelist = cfg.getSubtitleWhiteList();

                for (final YoutubeSubtitleInfo si : subtitles) {

                    try {

                        String groupID;
                        switch (cfg.getGroupLogic()) {
                        case BY_FILE_TYPE:
                            groupID = "srt";
                            break;
                        case NO_GROUP:
                            groupID = "srt-" + si.getLang();
                            break;
                        case BY_MEDIA_TYPE:
                            groupID = YoutubeVariantInterface.VariantGroup.SUBTITLES.name();
                            break;
                        default:
                            throw new WTFException("Unknown Grouping");
                        }
                        VariantInfo vi = new VariantInfo(YoutubeVariant.SUBTITLES, null, null, new YoutubeStreamData(vid, si._getUrl(vid.videoID), YoutubeITAG.SUBTITLE)) {

                            @Override
                            public void fillExtraProperties(DownloadLink thislink, List<VariantInfo> alternatives) {
                                thislink.setProperty(YoutubeHelper.YT_SUBTITLE_CODE, si.getLang());
                                final ArrayList<String> lngCodes = new ArrayList<String>();
                                for (final VariantInfo si : alternatives) {
                                    lngCodes.add(si.getIdentifier());
                                }
                                thislink.setProperty(YoutubeHelper.YT_SUBTITLE_CODE_LIST, JSonStorage.serializeToJson(lngCodes));
                            }

                            @Override
                            public String getIdentifier() {
                                return si.getLang();
                            }

                            @Override
                            public int compareTo(VariantInfo o) {
                                return this.variant.getName().compareToIgnoreCase(o.variant.getName());

                            }
                        };
                        if (whitelist != null) {
                            if (whitelist.contains(si.getLang())) {
                                List<VariantInfo> list = groups.get(groupID);
                                if (list == null) {
                                    list = new ArrayList<TbCmV2.VariantInfo>();
                                    groups.put(groupID, list);
                                }

                                list.add(vi);
                            } else {
                                List<VariantInfo> list = groupsExcluded.get(groupID);
                                if (list == null) {
                                    list = new ArrayList<TbCmV2.VariantInfo>();
                                    groupsExcluded.put(groupID, list);
                                }

                                list.add(vi);
                            }
                        } else {

                            List<VariantInfo> list = groups.get(groupID);
                            if (list == null) {
                                list = new ArrayList<TbCmV2.VariantInfo>();
                                groups.put(groupID, list);
                            }

                            list.add(vi);
                        }
                        allSubtitles.add(vi);
                    } catch (Exception e) {
                        getLogger().warning("New Subtitle Language: " + si.getLang() + " - " + si.getKind() + " - " + si.getLangOrg() + " - " + si.getName());
                    }

                }
            }

            main: for (Entry<String, List<VariantInfo>> e : groups.entrySet()) {
                if (e.getValue().size() == 0) continue;
                Collections.sort(e.getValue());

                if (e.getKey().equals(YoutubeVariantInterface.VariantGroup.SUBTITLES.name()) || e.getKey().equalsIgnoreCase("srt")) {
                    if (cfg.isCreateBestSubtitleVariantLinkEnabled()) {
                        // special handling for subtitles

                        String[] keys = cfg.getPreferedSubtitleLanguages();
                        // first try the users prefered list
                        if (keys != null) {
                            for (String locale : keys) {
                                try {
                                    for (VariantInfo vi : e.getValue()) {
                                        if (vi.getIdentifier().toLowerCase().startsWith(locale)) {
                                            decryptedLinks.add(createLink(vi, e.getValue()));
                                            continue main;
                                        }
                                    }
                                } catch (Exception e1) {
                                    getLogger().log(e1);
                                }
                            }
                        }

                        try {
                            // try to find the users locale
                            String desiredLocale = TranslationFactory.getDesiredLanguage();
                            for (VariantInfo vi : e.getValue()) {
                                if (vi.getIdentifier().toLowerCase().startsWith(desiredLocale.toLowerCase())) {
                                    decryptedLinks.add(createLink(vi, e.getValue()));
                                    continue main;
                                }
                            }
                        } catch (Exception e1) {
                            // try english
                            getLogger().log(e1);
                            for (VariantInfo vi : e.getValue()) {
                                if (vi.getIdentifier().toLowerCase().startsWith("en")) {
                                    decryptedLinks.add(createLink(vi, e.getValue()));
                                    continue main;
                                }
                            }

                        }
                        // fallback: use the first
                        decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
                    }
                } else {
                    if (cfg.getGroupLogic() != GroupLogic.NO_GROUP) {

                        switch (e.getValue().get(0).variant.getGroup()) {
                        case AUDIO:
                            if (cfg.isCreateBestAudioVariantLinkEnabled()) {
                                decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
                            }
                            break;

                        case IMAGE:
                            if (cfg.isCreateBestImageVariantLinkEnabled()) {
                                decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
                            }

                            break;
                        case VIDEO:
                            if (cfg.isCreateBestVideoVariantLinkEnabled()) {
                                decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
                            }

                            break;
                        case VIDEO_3D:
                            if (cfg.isCreateBest3DVariantLinkEnabled()) {
                                decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
                            }
                            break;
                        }
                    } else {

                        decryptedLinks.add(createLink(e.getValue().get(0), e.getValue()));
                    }
                }

            }

            if (extra != null && extra.length > 0) {
                main: for (VariantInfo v : allVariants.values()) {
                    for (String s : extra) {
                        if (v.variant.getTypeId().equals(s)) {

                            String groupID = getGroupID(v.variant);

                            List<VariantInfo> fromGroup = groups.get(groupID);

                            decryptedLinks.add(createLink(v, fromGroup));
                            continue main;

                        }
                    }
                }

            }

            ArrayList<String> extraSubtitles = cfg.getExtraSubtitles();
            if (extraSubtitles != null) {
                for (String v : extraSubtitles) {
                    if (v != null) {
                        for (VariantInfo vi : allSubtitles) {
                            if (vi.getIdentifier().equalsIgnoreCase(v)) {
                                decryptedLinks.add(createLink(vi, allSubtitles));
                            }

                        }
                    }
                }
            }

        }

        return decryptedLinks;
    }

    private Collection<? extends YoutubeClipData> parseGeneric(String cryptedUrl) throws InterruptedException, IOException {
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        if (StringUtils.isNotEmpty(cryptedUrl)) {
            int page = 1;
            int counter = 1;
            while (true) {
                if (this.isAbort()) { throw new InterruptedException(); }

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

    protected String getGroupID(YoutubeVariantInterface v) {
        String groupID;
        switch (cfg.getGroupLogic()) {
        case BY_FILE_TYPE:
            groupID = v.getFileExtension();
            break;
        case NO_GROUP:
            groupID = v.getUniqueId();
            break;
        case BY_MEDIA_TYPE:
            groupID = v.getMediaTypeID();
            break;
        default:
            throw new WTFException("Unknown Grouping");
        }
        return groupID;
    }

    private DownloadLink createLink(VariantInfo variantInfo, List<VariantInfo> alternatives) {
        try {
            YoutubeClipData clip = null;
            if (clip == null && variantInfo.videoStream != null) {
                clip = variantInfo.videoStream.getClip();

            }
            if (clip == null && variantInfo.audioStream != null) {
                clip = variantInfo.audioStream.getClip();

            }
            if (clip == null && variantInfo.data != null) {
                clip = variantInfo.data.getClip();

            }
            DownloadLink thislink;
            thislink = createDownloadlink("youtubev2://" + variantInfo.variant + "/" + clip.videoID + "/");

            // thislink.setAvailable(true);

            thislink.setBrowserUrl(getBase() + "/watch?v=" + clip.videoID);

            // thislink.setProperty(key, value)
            thislink.setProperty(YoutubeHelper.YT_EXT, variantInfo.variant.getFileExtension());

            thislink.setProperty(YoutubeHelper.YT_TITLE, clip.title);
            thislink.setProperty(YoutubeHelper.YT_PLAYLIST_INT, clip.playlistEntryNumber);
            thislink.setProperty(YoutubeHelper.YT_ID, clip.videoID);
            thislink.setProperty(YoutubeHelper.YT_AGE_GATE, clip.ageCheck);
            thislink.setProperty(YoutubeHelper.YT_CHANNEL, clip.channel);
            thislink.setProperty(YoutubeHelper.YT_USER, clip.user);
            thislink.setProperty(YoutubeHelper.YT_DATE, clip.date);
            thislink.setProperty(YoutubeHelper.YT_LENGTH_SECONDS, clip.length);
            thislink.setProperty(YoutubeHelper.YT_GOOGLE_PLUS_ID, clip.userGooglePlusID);
            thislink.setProperty(YoutubeHelper.YT_CHANNEL_ID, clip.channelID);
            thislink.setProperty(YoutubeHelper.YT_DURATION, clip.duration);
            thislink.setProperty(YoutubeHelper.YT_DATE_UPDATE, clip.dateUpdated);
            if (variantInfo.videoStream != null) {

                thislink.setProperty(YoutubeHelper.YT_STREAMURL_VIDEO, variantInfo.videoStream.getUrl());
            }
            if (variantInfo.audioStream != null) {

                thislink.setProperty(YoutubeHelper.YT_STREAMURL_AUDIO, variantInfo.audioStream.getUrl());
            }
            if (variantInfo.data != null) {

                thislink.setProperty(YoutubeHelper.YT_STREAMURL_DATA, variantInfo.data.getUrl());
            }
            ArrayList<String> variants = new ArrayList<String>();
            boolean has = false;
            if (alternatives != null) {

                for (VariantInfo vi : alternatives) {
                    // if (vi.variant != variantInfo.variant) {
                    variants.add(vi.variant.getUniqueId());
                    if (variantInfo.getIdentifier().equals(vi.getIdentifier())) {
                        has = true;
                    }
                    // }
                }

                if (!has) variants.add(0, variantInfo.variant.getUniqueId());
            }

            thislink.setVariantSupport(variants.size() > 1);
            thislink.setProperty(YoutubeHelper.YT_VARIANTS, JSonStorage.serializeToJson(variants));
            thislink.setProperty(YoutubeHelper.YT_VARIANT, variantInfo.variant.getUniqueId());
            variantInfo.fillExtraProperties(thislink, alternatives);
            String filename;
            thislink.setFinalFileName(filename = getCachedHelper().createFilename(thislink));
            thislink.setLinkID("youtubev2://" + variantInfo.variant + "/" + clip.videoID + "/" + URLEncode.encodeRFC2396(filename));
            FilePackage fp = FilePackage.getInstance();
            YoutubeHelper helper = getCachedHelper();
            fp.setName(helper.replaceVariables(thislink, helper.getConfig().getPackagePattern()));
            // let the packagizer merge several packages that have the same name
            fp.setProperty("ALLOW_MERGE", true);
            fp.add(thislink);

            return thislink;
        } catch (Exception e) {
            getLogger().log(e);
            return null;
        }

    }

    private YoutubeHelper getCachedHelper() {
        YoutubeHelper ret = cachedHelper;
        if (ret == null || ret.getBr() != this.br) {
            ret = new YoutubeHelper(br, PluginJsonConfig.get(YoutubeConfig.class), getLogger());

        }
        ret.setupProxy();
        return ret;
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
    public ArrayList<YoutubeClipData> parsePlaylist(String playlistID) throws IOException, InterruptedException {
        // this returns the html5 player
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        if (StringUtils.isNotEmpty(playlistID)) {
            int page = 1;
            int counter = 1;
            while (true) {
                if (this.isAbort()) { throw new InterruptedException(); }

                br.getPage(getBase() + "/playlist?list=" + playlistID + "&page=" + page);
                checkErrors(br);
                String[] videos = br.getRegex("href=\"(/watch\\?v=[A-Za-z0-9\\-_]+)\\&amp;list=[A-Z0-9]+").getColumn(0);
                if (videos != null) {
                    for (String relativeUrl : videos) {
                        String id = getVideoIDByUrl(relativeUrl);
                        if (dupeCheckSet.add(id)) {
                            ret.add(new YoutubeClipData(id, counter++));
                        }
                    }
                }
                // Several Pages: http://www.youtube.com/playlist?list=FL9_5aq5ZbPm9X1QH0K6vOLQ
                String nextPage = br.getRegex("<a href=\"/playlist\\?list=" + playlistID + "\\&amp;page=(\\d+)\"[^\r\n]+>Next").getMatch(0);
                if (nextPage != null) {
                    page = Integer.parseInt(nextPage);
                    // anti ddos
                    Thread.sleep(500);
                } else {
                    break;
                }
            }

        }
        return ret;
    }

    public ArrayList<YoutubeClipData> parseChannelgrid(String channelID) throws IOException, InterruptedException {
        // http://www.youtube.com/user/Gronkh/videos
        // channel: http://www.youtube.com/channel/UCYJ61XIK64sp6ZFFS8sctxw
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        int counter = 1;
        if (StringUtils.isNotEmpty(channelID)) {
            String pageUrl = null;
            while (true) {
                if (this.isAbort()) { throw new InterruptedException(); }
                String content = null;
                if (pageUrl == null) {
                    // this returns the html5 player
                    br.getPage(getBase() + "/channel/" + channelID + "/videos?view=0");

                    checkErrors(br);
                    content = br.toString();
                } else {
                    br.getPage(pageUrl);
                    checkErrors(br);
                    content = jd.plugins.hoster.Youtube.unescape(br.toString());
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
                    Thread.sleep(1000);
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
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        int counter = 1;
        if (StringUtils.isNotEmpty(userID)) {
            String pageUrl = null;
            while (true) {
                if (this.isAbort()) { throw new InterruptedException(); }
                String content = null;
                if (pageUrl == null) {
                    // this returns the html5 player
                    br.getPage(getBase() + "/user/" + userID + "/videos?view=0");

                    checkErrors(br);
                    content = br.toString();
                } else {
                    br.getPage(pageUrl);
                    checkErrors(br);
                    content = jd.plugins.hoster.Youtube.unescape(br.toString());
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
                    Thread.sleep(1000);
                } else {
                    break;
                }
            }

        }
        return ret;
    }

    private void checkErrors(Browser br) throws InterruptedException {
        if (br.containsHTML(">404 Not Found<")) { throw new InterruptedException("404 Not Found"); }
        if (br.containsHTML("iframe style=\"display:block;border:0;\" src=\"/error")) { throw new InterruptedException("Unknown Error"); }

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