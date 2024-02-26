//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.BandCampCom;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { BandCampCom.class })
public class BandCampComDecrypter extends PluginForDecrypt {
    public BandCampComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        prepBR(br);
        return br;
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        /* Prefer English language */
        br.getHeaders().put("Accept-Language", "en-US, en;q=0.9");
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return BandCampCom.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            final String domainspart = buildHostsPatternPart(domains);
            ret.add("https?://(([a-z0-9\\-]+\\.)?" + domainspart + "/(?:(?:album|track)/[a-z0-9\\-_]+|\\?show=\\d+)|(?!www\\.)?[a-z0-9\\-]+\\." + domainspart + "/?$)|https?://(?:www\\.)?" + domainspart + "/EmbeddedPlayer(?:\\.html)?[^\\?#]*/(?:album|track)=\\d+");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-07-30: Too many requests in a short time --> 503 server response */
        return 1;
    }

    private final Pattern                  TYPE_EMBED         = Pattern.compile("(?i)https?://[^/]+/EmbeddedPlayer(?:\\.html)?.*?/(?:album|track)=\\d+.*");
    private final Pattern                  TYPE_SHOW          = Pattern.compile("(?i)https?://[^/]+/\\?show=(\\d+)");
    private static AtomicReference<String> videoSupportBroken = new AtomicReference<String>();

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Regex show = new Regex(param.getCryptedUrl(), TYPE_SHOW);
        if (show.patternFind()) {
            return crawlShow(show.getMatch(0));
        } else {
            return crawlAlbumOrTrack(param.getCryptedUrl());
        }
    }

    public ArrayList<DownloadLink> crawlShow(final String showID) throws Exception {
        br.getPage("https://" + this.getHost() + "/api/bcweekly/2/get?id=" + showID + "&lo=1&lo_action_url=https://" + this.getHost());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Map<String, Object> show = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object error = show.get("error");
        if (error != null) {
            /* E.g. {"error":true} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int showDurationSeconds = ((Number) show.get("audio_duration")).intValue();
        final String published_date = show.get("published_date").toString();
        final long dateTimestamp = dateToTimestamp(published_date);
        final String showTitle = show.get("audio_title").toString();
        final Map<String, Object> audio_stream = (Map<String, Object>) show.get("audio_stream");
        final String directurl = audio_stream.get("mp3-128").toString();
        final DownloadLink showcomplete = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(Encoding.htmlOnlyDecode(directurl)));
        showcomplete.setProperty(BandCampCom.PROPERTY_FILE_TYPE, "mp3");
        showcomplete.setProperty(BandCampCom.PROPERTY_TITLE, showTitle);
        showcomplete.setProperty(BandCampCom.PROPERTY_TRACK_DATE_TIMESTAMP, dateTimestamp);
        showcomplete.setProperty(BandCampCom.PROPERTY_ALBUM_TRACK_POSITION, 0);
        showcomplete.setDownloadSize(128 * 1024l / 8 * showDurationSeconds);
        ret.add(showcomplete);
        /* Crawl individual tracks which were played in this show. */
        final List<Map<String, Object>> audiotracks = (List<Map<String, Object>>) show.get("tracks");
        for (int index = 0; index < audiotracks.size(); index++) {
            /* Some tracks can be offline but we will set all items as online anyways because most of them can expected to be online. */
            final Map<String, Object> trackmap = audiotracks.get(index);
            final DownloadLink track = this.createDownloadlink(trackmap.get("track_url").toString());
            track.setProperty(BandCampCom.PROPERTY_CONTENT_ID, trackmap.get("track_id"));
            track.setProperty(BandCampCom.PROPERTY_ALBUM_ID, trackmap.get("album_id"));
            track.setProperty(BandCampCom.PROPERTY_ARTIST, trackmap.get("artist"));
            track.setProperty(BandCampCom.PROPERTY_ALBUM_TITLE, trackmap.get("album_title"));
            track.setProperty(BandCampCom.PROPERTY_TITLE, trackmap.get("title"));
            track.setProperty(BandCampCom.PROPERTY_FILE_TYPE, "mp3");
            track.setProperty(BandCampCom.PROPERTY_SHOW_TRACK_POSITION, index + 1);
            /* Calculate track-duration */
            final int timecode = ((Number) trackmap.get("timecode")).intValue();
            final boolean isLastItem = index == audiotracks.size() - 1;
            final int trackDurationSeconds;
            if (isLastItem) {
                trackDurationSeconds = showDurationSeconds - timecode;
            } else {
                /* Get timecode from next item to calculate duration of this item. */
                final Map<String, Object> nextItem = audiotracks.get(index + 1);
                trackDurationSeconds = ((Number) nextItem.get("timecode")).intValue() - timecode;
            }
            track.setDownloadSize(128 * 1024l / 8 * trackDurationSeconds);
            ret.add(track);
        }
        /* Add cover art if user wants it. */
        final String imageID = show.get("show_v2_image_id").toString();
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean grabCoverArt = cfg.getBooleanProperty(BandCampCom.SETTING_GRAB_COVER_ART, BandCampCom.defaultGRAB_COVER_ART);
        if (grabCoverArt) {
            final String coverArtURL = "https://f4.bcbits.com/img/" + imageID + "_0";
            final DownloadLink thumb = createDownloadlink(DirectHTTP.createURLForThisPlugin(coverArtURL));
            thumb.setProperty(BandCampCom.PROPERTY_TITLE, showTitle);
            thumb.setProperty(BandCampCom.PROPERTY_TRACK_DATE_TIMESTAMP, dateTimestamp);
            thumb.setProperty(BandCampCom.PROPERTY_SHOW_NUMBEROF_TRACKS, 0);
            thumb.setProperty(BandCampCom.PROPERTY_FILE_TYPE, "jpg");
            ret.add(thumb);
        }
        /* Add additional properties and set filename. */
        for (final DownloadLink result : ret) {
            result.setProperty(BandCampCom.PROPERTY_SHOW_NUMBEROF_TRACKS, audiotracks.size());
            final String formattedFilename = BandCampCom.getFormattedFilename(result);
            result.setFinalFileName(formattedFilename);
            result.setAvailable(true);
        }
        /* Set FilePackage with package name. */
        final FilePackage fp = FilePackage.getInstance();
        final String userDefinedPackagename = getFormattedPackagename(ret.get(0), cfg);
        if (!StringUtils.isEmpty(userDefinedPackagename)) {
            fp.setName(userDefinedPackagename);
        } else {
            fp.setName(showTitle);
        }
        final String description = (String) show.get("desc");
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        fp.setPackageKey("bandcamp://show/" + showID);
        fp.addLinks(ret);
        return ret;
    }

    public ArrayList<DownloadLink> crawlAlbumOrTrack(final String url) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String contentURL = url;
        if (new Regex(contentURL, TYPE_EMBED).patternFind()) {
            br.getPage(contentURL);
            final String originalURL = br.getRegex("\\&quot;linkback\\&quot;:\\&quot;(https?://[^/]+/(?:album|track)/[a-z0-9\\-_]+)").getMatch(0);
            if (originalURL == null) {
                /* Assume that this content is offline or url is invalid */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            contentURL = originalURL;
            br.clearCookies(br.getHost());
        }
        br.getPage(contentURL);
        if (br.containsHTML(">\\s*Sorry\\s*,\\s*that something isn('|’)t here|trackinfo\\s*:\\s*\\[\\],") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String json = br.getRegex("trackinfo(?:\\&quot;|\")\\s*:\\s*(\\[.*?\\])(\\s*,\\s*\"|\\s*,\\s*\\&quot)").getMatch(0);
        if (!br.getURL().contains(this.getHost()) && json == null) {
            /* 2020-03-16: Redirect to external website */
            ret.add(this.createDownloadlink(br.getURL()));
            return ret;
        }
        /*
         * 2020-11-30: URLs to Labels look the same as artist URLs but they won't lead to any music directly but simply list all artists
         * under that label.
         */
        final String[] artistList = br.getRegex("(https?://[^/]+\\.bandcamp\\.com)\\?label=\\d+").getColumn(0);
        final String[] albumList = br.getRegex("(/album/[^<>\"\\']+)").getColumn(0);
        final String albumID = br.getRegex("<\\!-- album id (\\d+) -->").getMatch(0);
        if (albumID == null) {
            logger.warning("Failed to find albumID");
        }
        if (json == null) {
            /* Not a single track or album but a list of albums or users. */
            if (!this.canHandle(br.getURL())) {
                logger.info("Invalid URL or URL doesn't contain any downloadable content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Let's see if there is anything else other than music that we may be able to download. */
            if (artistList != null && artistList.length > 0) {
                for (String artistURL : artistList) {
                    artistURL += "/";
                    ret.add(this.createDownloadlink(artistURL));
                }
                return ret;
            } else if (albumList != null && albumList.length > 0) {
                for (String albumURL : albumList) {
                    albumURL = br.getURL(albumURL).toString();
                    ret.add(this.createDownloadlink(albumURL));
                }
                return ret;
            } else {
                /* E.g. "https://daily.bandcamp.com/" */
                logger.info("Failed to find any downloadable content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (Encoding.isHtmlEntityCoded(json)) {
            json = Encoding.htmlDecode(json);
        }
        final String json_album = br.getRegex("<script type=\"application/(?:json\\+ld|ld\\+json)\">\\s*(.*?)\\s*</script>").getMatch(0);
        final String errorAlbumUnavailable = br.getRegex("<h4 class=\"notable\">([^<]+)</h4>").getMatch(0);
        if (json_album == null) {
            if (errorAlbumUnavailable != null) {
                /* E.g. "Sold out": https://baritalia.bandcamp.com/album/cdr */
                logger.info("Item unavailable because: " + errorAlbumUnavailable);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final Map<String, Object> albuminfo = restoreFromString(json_album, TypeRef.MAP);
        Number numTracks = (Number) albuminfo.get("numTracks");
        String albumArtist = (String) JavaScriptEngineFactory.walkJson(albuminfo, "byArtist/name");
        String albumDescription = null;
        String albumTitle = null;
        final String dateStr = (String) JavaScriptEngineFactory.walkJson(albuminfo, "datePublished");
        Long dateTimestamp = null;
        if (dateStr != null) {
            dateTimestamp = dateToTimestamp(dateStr);
        }
        /**
         * Not all albums have playable audio tracks so for some, all we can crawl is the album cover art. </br>
         * Example-album without any streamable tracks: https://midsummerex.bandcamp.com/album/intl
         */
        boolean isSingleTrack = new Regex(br.getURL(), BandCampCom.PATTERN_SINGLE_TRACK).patternFind();
        final List<Map<String, Object>> albumtracks = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(json);
        if (albumtracks != null && albumtracks.size() > 0) {
            final String type = albuminfo.get("@type").toString();
            if (type.equalsIgnoreCase("MusicRecording") && albumtracks.size() == 1) {
                isSingleTrack = true;
            } else {
                isSingleTrack = false;
            }
            if (!isSingleTrack) {
                /* Can be different in context of album or single track! */
                final String jsonAlbumEmbed = br.getRegex("data-embed=\"([^\"]+)").getMatch(0);
                if (jsonAlbumEmbed != null) {
                    final Map<String, Object> albumEmbedInfo = JSonStorage.restoreFromString(Encoding.htmlOnlyDecode(jsonAlbumEmbed), TypeRef.MAP);
                    albumArtist = (String) albumEmbedInfo.get("artist");
                    if (!StringUtils.isEmpty(albumArtist)) {
                        albumArtist = Encoding.htmlDecode(albumArtist).trim();
                    }
                }
                albumTitle = (String) albuminfo.get("name");
                albumDescription = (String) albuminfo.get("description");
                numTracks = (Number) albuminfo.get("numTracks");
            }
            int index = 0;
            int numberOfUnPlayableTracks = 0;
            for (final Map<String, Object> audio : albumtracks) {
                String contentUrl = audio.get("title_link").toString();
                if (StringUtils.isEmpty(contentUrl)) {
                    /* Skip invalid objects */
                    continue;
                }
                if (contentUrl.startsWith("/")) {
                    contentUrl = br.getURL(contentUrl).toExternalForm();
                }
                final DownloadLink audiotrack = createDownloadlink(contentUrl);
                BandCampCom.parseAndSetSingleTrackInfo(audiotrack, br, index);
                if (!audiotrack.hasProperty(BandCampCom.PROPERTY_DATE_DIRECTURL)) {
                    numberOfUnPlayableTracks++;
                }
                ret.add(audiotrack);
                final String videoSourceID = (String) audio.get("video_source_id");
                if (StringUtils.isNotEmpty(videoSourceID)) {
                    synchronized (videoSupportBroken) {
                        if (videoSupportBroken.get() == null || !getPluginVersionHash().equals(videoSupportBroken.get())) {
                            try {
                                String token = new Regex(StringUtils.valueOfOrNull(audio.get("video_poster_url")), "\\d+/\\d+/([^/]+)").getMatch(0);
                                if (token == null) {
                                    token = new Regex(StringUtils.valueOfOrNull(audio.get("video_mobile_url")), "\\d+/\\d+/([^/]+)").getMatch(0);
                                }
                                if (token != null) {
                                    final String playerID = "9891472";
                                    final Browser brc = br.cloneBrowser();
                                    brc.setCookie("bandcamp.23video.com", "uuid", UUID.randomUUID().toString());
                                    brc.setCookie("bandcamp.23video.com", "_visual_swf_referer", "https%3A//bandcamp.com/");
                                    brc.setCurrentURL("https://bandcamp.23video.com/" + playerID + ".ihtml/player.html?token=" + token + "&source=embed&photo_id=" + videoSourceID);
                                    brc.getPage("https://bandcamp.23video.com/api/concatenate?callback=visualplatformconcat_0&format=json&playersettings_0=%2Fapi%2Fplayer%2Fsettings%3Fplayer_id%3D" + playerID + "%26parameters%3Dtoken%253D" + token + "%2526source%253Dembed%2526photo_id%253D" + videoSourceID + "&livelist_1=%2Fapi%2Flive%2Flist%3Ftoken%3D" + token + "%26source%3Dembed%26photo_id%3D" + videoSourceID + "%26upcoming_p%3D1%26ordering%3Dstreaming%26player_id%3D" + playerID + "&photolist_2=%2Fapi%2Fphoto%2Flist%3Fsize%3D10%26include_actions_p%3D1%26token%3D" + token + "%26source%3Dembed%26photo_id%3D" + videoSourceID + "%26upcoming_p%3D1%26ordering%3Dstreaming%26player_id%3D" + playerID);
                                    final String visualplatformconcat_0 = brc.getRegex("visualplatformconcat_0\\(\\s*(\\{.*?\\})\\s*\\)\\s*;\\s*$").getMatch(0);
                                    final Map<String, Object> visualplatformconcat = restoreFromString(visualplatformconcat_0, TypeRef.MAP);
                                    final Map<String, Object> photo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(visualplatformconcat, "photolist_2/photo");
                                    for (final String format : new String[] { "4k", "1080p", "hd"/* 720 */, "medium", "mobile_high", "webm_360p", "webm_720p" }) {
                                        final String videoURL = StringUtils.valueOfOrNull(photo.get("video_" + format + "_download"));
                                        if (StringUtils.isNotEmpty(videoURL)) {
                                            final DownloadLink videoEntry = createDownloadlink(brc.getURL(videoURL).toString());
                                            /* Inherent all properties of corresponding audiotrack, then add video specific properties. */
                                            videoEntry.setProperties(audiotrack.getProperties());
                                            videoEntry.setProperty(BandCampCom.PROPERTY_VIDEO_FORMAT, format);
                                            final Object video_width = photo.get("video_" + format + "_width");
                                            final Object video_height = photo.get("video_" + format + "_height");
                                            if (video_width != null) {
                                                videoEntry.setProperty(BandCampCom.PROPERTY_VIDEO_WIDTH, video_width.toString());
                                            }
                                            if (video_height != null) {
                                                videoEntry.setProperty(BandCampCom.PROPERTY_VIDEO_HEIGHT, video_height.toString());
                                            }
                                            final long fileSize = JavaScriptEngineFactory.toLong(photo.get("video_" + format + "_size"), -1l);
                                            if (fileSize > 0) {
                                                videoEntry.setDownloadSize(fileSize);
                                            }
                                            String fileExtension = Plugin.getFileNameExtensionFromURL(videoURL);
                                            if (fileExtension == null) {
                                                fileExtension = "mp4";
                                            }
                                            videoEntry.setProperty(BandCampCom.PROPERTY_FILE_TYPE, fileExtension);
                                            ret.add(videoEntry);
                                        }
                                    }
                                }
                            } catch (final Exception e) {
                                logger.log(e);
                                videoSupportBroken.set(getPluginVersionHash());
                            }
                        }
                    }
                }
                index++;
            }
            logger.info("Number of un-playable tracks in this album: " + numberOfUnPlayableTracks);
        }
        if (ret.isEmpty()) {
            logger.info("This album doesn't contain any playable tracks");
        }
        // final String json_band = br.getRegex("data-band=\"(\\{[^\"]+)").getMatch(0);
        /* Single song or album cover art. Crawl it if user wants it or if no audio/video items were found. */
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        if (cfg.getBooleanProperty(BandCampCom.SETTING_GRAB_COVER_ART, BandCampCom.defaultGRAB_COVER_ART) || ret.isEmpty()) {
            /* TODO: Check filenames */
            String coverArtURL = br.getRegex("(?i)<a class=\"popupImage\" href=\"(https?://[^<>\"]*?\\.jpg)\"").getMatch(0);
            if (coverArtURL != null) {
                coverArtURL = coverArtURL.replaceFirst("(_\\d+)(\\.\\w+)$", "_0$2");
                final DownloadLink thumb = createDownloadlink(coverArtURL);
                if (ret.size() > 0) {
                    /* Inherit properties from first crawler audio track. */
                    thumb.setProperties(ret.get(0).getProperties());
                }
                // thumb.setProperty(BandCampCom.PROPERTY_TITLE, "coverart");
                thumb.setProperty(BandCampCom.PROPERTY_FILE_TYPE, "jpg");
                thumb.setProperty(BandCampCom.PROPERTY_ALBUM_TRACK_POSITION, 0);
                ret.add(thumb);
            }
        }
        /* Set additional properties, filename and online-status. */
        for (final DownloadLink result : ret) {
            result.setProperty(BandCampCom.PROPERTY_ALBUM_NUMBEROF_TRACKS, albumtracks.size());
            if (albumID != null) {
                result.setProperty(BandCampCom.PROPERTY_ALBUM_ID, albumID);
            }
            if (albumArtist != null) {
                result.setProperty(BandCampCom.PROPERTY_ARTIST_ALBUM, Encoding.htmlDecode(albumArtist).trim());
            }
            if (dateTimestamp != null) {
                result.setProperty(BandCampCom.PROPERTY_ALBUM_DATE_TIMESTAMP, dateTimestamp);
            }
            if (albumTitle != null) {
                result.setProperty(BandCampCom.PROPERTY_ALBUM_TITLE, albumTitle);
            }
            final String formattedFilename = BandCampCom.getFormattedFilename(result);
            result.setFinalFileName(formattedFilename);
            if (isSingleTrack || cfg.getBooleanProperty(BandCampCom.SETTING_ENABLE_FAST_LINKCHECK_ALBUM, BandCampCom.defaultEnableFastLinkcheckAlbum)) {
                result.setAvailable(true);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        final String formattedpackagename = getFormattedPackagename(ret.get(0), cfg);
        if (!cfg.getBooleanProperty(BandCampCom.CLEANPACKAGENAME, BandCampCom.defaultCLEANPACKAGENAME)) {
            fp.setCleanupPackageName(false);
        } else {
            fp.setCleanupPackageName(true);
        }
        if (!isSingleTrack) {
            /* Allow all crawled results of an album to be auto merged into one package. */
            fp.setAllowInheritance(true);
        }
        fp.setName(formattedpackagename);
        if (albumID != null) {
            fp.setPackageKey("bandcamp://album/" + albumID);
        } else {
            final String trackID = ret.get(0).getStringProperty(BandCampCom.PROPERTY_CONTENT_ID);
            if (trackID != null) {
                fp.setPackageKey("bandcamp://track/" + trackID);
            }
        }
        if (albumDescription != null) {
            fp.setComment(albumDescription);
        }
        fp.addLinks(ret);
        if (ret.isEmpty()) {
            if (errorAlbumUnavailable != null) {
                /* E.g. "Sold out": https://baritalia.bandcamp.com/album/cdr */
                logger.info("Item unavailable because: " + errorAlbumUnavailable);
            } else {
                logger.info("Failed to find any downloadable content: Empty album?");
            }
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        return ret;
    }

    public static final String regexUsernameFromURL(final String url) {
        return new Regex(url, "(?i)https?://([^/]*?)\\.[^/]+/").getMatch(0);
    }

    public static final long dateToTimestamp(final String dateStr) {
        return TimeFormatter.getMilliSeconds(dateStr, "dd MMM yyyy HH:mm:ss ZZZ", Locale.ENGLISH);
    }

    public static String getFormattedPackagename(final DownloadLink link, final SubConfiguration cfg) throws ParseException {
        String formatString = cfg.getStringProperty(BandCampCom.CUSTOM_PACKAGENAME, BandCampCom.defaultCustomPackagename);
        if (StringUtils.isEmpty(formatString)) {
            formatString = BandCampCom.defaultCustomPackagename;
        }
        /* Insert album title at the end to prevent errors with tags */
        String formattedpackagename = BandCampCom.getFormattedBaseString(link, formatString);
        if (cfg.getBooleanProperty(BandCampCom.PACKAGENAMELOWERCASE, BandCampCom.defaultPACKAGENAMELOWERCASE)) {
            formattedpackagename = formattedpackagename.toLowerCase(Locale.ENGLISH);
        }
        if (cfg.getBooleanProperty(BandCampCom.PACKAGENAMESPACE, BandCampCom.defaultPACKAGENAMESPACE)) {
            formattedpackagename = formattedpackagename.replaceAll("\\s+", "_");
        }
        formattedpackagename = formattedpackagename.replaceFirst("([-\\s]+$)", "");
        return formattedpackagename;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}