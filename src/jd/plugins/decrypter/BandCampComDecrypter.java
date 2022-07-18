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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.BandCampCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { BandCampCom.class })
public class BandCampComDecrypter extends PluginForDecrypt {
    public BandCampComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
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
            ret.add("https?://(([a-z0-9\\-]+\\.)?" + domainspart + "/(?:album|track)/[a-z0-9\\-_]+|(?!www\\.)?[a-z0-9\\-]+\\." + domainspart + "/?$)|https?://(?:www\\.)?" + domainspart + "/EmbeddedPlayer(?:\\.html)?[^\\?#]*/(?:album|track)=\\d+");
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

    private final String                   TYPE_EMBED         = "https?://(?:www\\.)?bandcamp\\.com/EmbeddedPlayer(?:\\.html)?.*?/(?:album|track)=\\d+.*";
    private static AtomicReference<String> videoSupportBroken = new AtomicReference<String>();

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_EMBED)) {
            br.getPage(param.getCryptedUrl());
            final String originalURL = br.getRegex("\\&quot;linkback\\&quot;:\\&quot;(https?://[^/]+/(?:album|track)/[a-z0-9\\-_]+)").getMatch(0);
            if (originalURL == null) {
                /* Assume that this content is offline or url is invalid */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            param.setCryptedUrl(originalURL);
            br.clearCookies(br.getHost());
        }
        br.getPage(param.getCryptedUrl());
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
        if (json == null) {
            if (!this.canHandle(br.getURL())) {
                logger.info("Invalid URL or URL doesn't contain any downloadable content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* Let's see if there is anything else other than music that we may be able to download. */
                if (artistList.length > 0) {
                    for (String artistURL : artistList) {
                        artistURL += "/";
                        ret.add(this.createDownloadlink(artistURL));
                    }
                    return ret;
                } else if (albumList.length > 0) {
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
        } else if (Encoding.isHtmlEntityCoded(json)) {
            json = Encoding.htmlDecode(json);
        }
        final String json_album = br.getRegex("<script type=\"application/(?:json\\+ld|ld\\+json)\">\\s*(.*?)\\s*</script>").getMatch(0);
        if (json_album == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> albumInfo = JSonStorage.restoreFromString(json_album, TypeRef.HASHMAP);
        String artist = (String) JavaScriptEngineFactory.walkJson(albumInfo, "byArtist/name");
        if (artist == null) {
            artist = br.getRegex("name\\s*=\\s*\"title\"\\s*content\\s*=\\s*\"[^\"]+,\\s*by\\s*([^<>\"]+)\\s*\"").getMatch(0);
        }
        String album = (String) JavaScriptEngineFactory.walkJson(albumInfo, "inAlbum/name");
        if (album == null) {
            album = br.getRegex("<title>\\s*(.*?)\\s*\\|.*?</title>").getMatch(0);
            if (album == null) {
                album = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
            }
        }
        String date = (String) JavaScriptEngineFactory.walkJson(albumInfo, "datePublished");
        if (date == null) {
            date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
        }
        final List<Map<String, Object>> audios = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final int padLength = StringUtils.getPadLength(audios.size());
        artist = Encoding.htmlDecode(artist).trim();
        album = Encoding.htmlDecode(album).trim();
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        int trackPosition = 1;
        for (final Map<String, Object> audio : audios) {
            String contentUrl = (String) audio.get("title_link");
            final String title = (String) audio.get("title");
            final long duration = JavaScriptEngineFactory.toLong(audio.get("duration"), 0);
            if (StringUtils.isEmpty(contentUrl) || StringUtils.isEmpty(title)) {
                /* Skip invalid objects */
                continue;
            }
            if (contentUrl.startsWith("/")) {
                contentUrl = br.getURL(contentUrl).toString();
            }
            final DownloadLink dl = createDownloadlink(contentUrl);
            dl.setProperty("fromdecrypter", true);
            dl.setProperty("directdate", date);
            dl.setProperty("directartist", artist);
            dl.setProperty("directalbum", album);
            dl.setProperty("directname", title);
            dl.setProperty("type", "mp3");
            dl.setProperty("directtracknumber", StringUtils.formatByPadLength(padLength, trackPosition));
            final String formattedFilename = BandCampCom.getFormattedFilename(this, dl);
            dl.setName(formattedFilename);
            if (cfg.getBooleanProperty(BandCampCom.FASTLINKCHECK, BandCampCom.defaultFASTLINKCHECK)) {
                dl.setAvailable(true);
            }
            if (duration > 0) {
                final long length = 128 * 1024l / 8 * duration;
                dl.setDownloadSize(length);
                dl.setAvailable(true);
            }
            ret.add(dl);
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
                                for (final String format : new String[] { "1080p", "hd"/* 720 */, "medium", "mobile_high" }) {
                                    final String videoURL = StringUtils.valueOfOrNull(photo.get("video_" + format + "_download"));
                                    if (StringUtils.isNotEmpty(videoURL)) {
                                        final DownloadLink videoEntry = createDownloadlink(brc.getURL(videoURL).toString());
                                        final long fileSize = JavaScriptEngineFactory.toLong(photo.get("video_" + format + "_size"), -1l);
                                        if (fileSize > 0) {
                                            dl.setDownloadSize(fileSize);
                                        }
                                        if (cfg.getBooleanProperty(BandCampCom.FASTLINKCHECK, BandCampCom.defaultFASTLINKCHECK)) {
                                            dl.setAvailable(true);
                                            ret.add(videoEntry);
                                        } else {
                                            final Browser br2 = brc.cloneBrowser();
                                            br2.setFollowRedirects(true);
                                            URLConnectionAdapter con = null;
                                            try {
                                                con = br2.openHeadConnection(videoEntry.getPluginPatternMatcher());
                                                if (looksLikeDownloadableContent(con)) {
                                                    dl.setAvailable(true);
                                                    dl.setVerifiedFileSize(con.getCompleteContentLength());
                                                    ret.add(videoEntry);
                                                }
                                            } catch (IOException e) {
                                                logger.log(e);
                                            } finally {
                                                if (con != null) {
                                                    con.disconnect();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.log(e);
                            videoSupportBroken.set(getPluginVersionHash());
                        }
                    }
                }
            }
            trackPosition++;
        }
        final boolean decryptThumb = cfg.getBooleanProperty(BandCampCom.GRABTHUMB, BandCampCom.defaultGRABTHUMB);
        if (decryptThumb) {
            String thumbnail = br.getRegex("<a class=\"popupImage\" href=\"(https?://[^<>\"]*?\\.jpg)\"").getMatch(0);
            if (thumbnail != null) {
                thumbnail = thumbnail.replaceFirst("(_\\d+)(\\.\\w+)$", "_0$2");
                final DownloadLink thumb = createDownloadlink(thumbnail);
                thumb.setProperties(ret.get(0).getProperties());
                thumb.setProperty("type", "jpg");
                final String formattedFilename = BandCampCom.getFormattedFilename(this, thumb);
                thumb.setFinalFileName(formattedFilename);
                ret.add(thumb);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        final String formattedpackagename = getFormattedPackagename(this, cfg, artist, album, date);
        if (!cfg.getBooleanProperty(BandCampCom.CLEANPACKAGENAME, BandCampCom.defaultCLEANPACKAGENAME)) {
            fp.setCleanupPackageName(false);
        } else {
            fp.setCleanupPackageName(true);
        }
        fp.setName(formattedpackagename);
        fp.addLinks(ret);
        if (ret.size() == 0) {
            logger.info("Failed to find any downloadable content: Empty album?");
        }
        return ret;
    }

    public static String getFormattedPackagename(final PluginForDecrypt plugin, final SubConfiguration cfg, final String artist, final String album, final String dateString) throws ParseException {
        String formattedpackagename = cfg.getStringProperty(BandCampCom.CUSTOM_PACKAGENAME, BandCampCom.defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) {
            formattedpackagename = BandCampCom.defaultCustomPackagename;
        }
        if (!formattedpackagename.contains("*artist*") && !formattedpackagename.contains("*album*")) {
            formattedpackagename = BandCampCom.defaultCustomPackagename;
        }
        if (dateString != null && formattedpackagename.contains("*date*")) {
            Date date = TimeFormatter.parseDateString(dateString);
            if (date == null) {
                try {
                    final SimpleDateFormat oldFormat = new SimpleDateFormat("yyyyMMdd");
                    date = oldFormat.parse(dateString);
                } catch (Exception e) {
                    plugin.getLogger().log(e);
                }
            }
            if (date != null) {
                final String userDefinedDateFormat = cfg.getStringProperty(BandCampCom.CUSTOM_DATE, BandCampCom.defaultCUSTOM_DATE);
                String formattedDate = null;
                for (final String format : new String[] { userDefinedDateFormat, "yyyyMMdd" }) {
                    if (format != null) {
                        try {
                            final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                            formattedDate = formatter.format(date);
                            if (formattedDate != null) {
                                break;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(e);
                        }
                    }
                }
                if (formattedDate != null) {
                    formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
                } else {
                    formattedpackagename = formattedpackagename.replace("*date*", "");
                }
            }
        }
        if (formattedpackagename.contains("*artist*")) {
            formattedpackagename = formattedpackagename.replace("*artist*", artist);
        }
        // Insert albumname at the end to prevent errors with tags
        formattedpackagename = formattedpackagename.replace("*album*", album);
        if (cfg.getBooleanProperty(BandCampCom.PACKAGENAMELOWERCASE, BandCampCom.defaultPACKAGENAMELOWERCASE)) {
            formattedpackagename = formattedpackagename.toLowerCase(Locale.ENGLISH);
        }
        if (cfg.getBooleanProperty(BandCampCom.PACKAGENAMESPACE, BandCampCom.defaultPACKAGENAMESPACE)) {
            formattedpackagename = formattedpackagename.replaceAll("\\s+", "_");
        }
        return formattedpackagename;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}