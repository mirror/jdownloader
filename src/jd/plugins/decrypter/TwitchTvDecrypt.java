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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "twitch.tv" }, urls = { "https?://((www\\.|[a-z]{2}\\.|secure\\.)?(twitchtv\\.com|twitch\\.tv)/(?!directory)(?:[^<>/\"]+/(?:(b|c|v)/\\d+|videos(\\?page=\\d+)?|video/\\d+)|videos/\\d+)|(www\\.|secure\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+)" })
public class TwitchTvDecrypt extends PluginForDecrypt {
    public TwitchTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String userApiToken = null;

    private Browser ajaxGetPage(final String string) throws Exception {
        final Browser ajax = br.cloneBrowser();
        // https://dev.twitch.tv/docs/v5/
        // For client IDs created on or after May 31, 2019, the only available version of the Kraken API is v5. For client IDs created prior
        // to May 31, 2019, use the application/vnd.twitchtv.v5+json header on your requests to access v5 of the Kraken API.
        ajax.getHeaders().put("Accept", "application/vnd.twitchtv.v5+json");
        ajax.getHeaders().put("Referer", "https://www.twitch.tv");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getHeaders().put("Origin", "https://www.twitch.tv");
        ajax.getHeaders().put("Client-ID", jd.plugins.hoster.TwitchTv.getClientID(br, this));
        if (userApiToken != null) {
            ajax.getHeaders().put("Twitch-Api-Token", userApiToken);
        }
        ajax.getPage(string);
        return ajax;
    }

    private Browser ajaxGetPagePlayer(final String string) throws Exception {
        final Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("Client-ID", jd.plugins.hoster.TwitchTv.getClientID(br, this));
        ajax.getHeaders().put("Accept", "application/vnd.twitchtv.v5+json");
        ajax.getHeaders().put("Origin", "https://www.twitch.tv");
        ajax.getPage(string);
        return ajax;
    }

    private final String FASTLINKCHECK  = "FASTLINKCHECK";
    private final String videoSingleWeb = "https?://(?:(?:www\\.|[a-z]{2}\\.|secure\\.)?(?:twitchtv\\.com|twitch\\.tv)/[^<>/\"]+/((b|c)/\\d+)|(?:www\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+)";
    private final String videoSingleHLS = "https?://(?:(?:www\\.|[a-z]{2}\\.|secure\\.)?(?:twitchtv\\.com|twitch\\.tv)/(?:[^<>/\"]+/v/\\d+|videos/\\d+))";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> desiredLinks = new ArrayList<DownloadLink>();
        // currently they redirect https to http
        String parameter = param.toString().replaceFirst("^http://", "https://").replaceAll("://([a-z]{2}\\.|secure\\.)?(twitchtv\\.com|twitch\\.tv)", "://www.twitch.tv");
        final String vid = new Regex(parameter, "(\\d+)$").getMatch(0);
        if (parameter.matches(".*?/[^<>/\"]+/video/\\d+.*")) {
            // /username/video/videoID -> redirect to /videos/videoID
            decryptedLinks.add(createDownloadlink("https://twitch.tv/videos/" + vid));
            return decryptedLinks;
        }
        final SubConfiguration cfg = this.getPluginConfig();
        br = new Browser();
        br.setCookie("http://twitch.tv", "language", "en-au");
        // redirects occur to de.domain when browser accept language set to German!
        br.getHeaders().put("Accept-Language", "en-gb");
        // currently redirect to www.
        br.setFollowRedirects(true);
        /* Log in if possible to be able to download "for subscribers only" videos */
        String token = null;
        String additionalparameters = "";
        if (getUserLogin(false)) {
            logger.info("Logged in via decrypter");
            final Browser brc = ajaxGetPage("https://api.twitch.tv/api/viewer/token.json?as3=t");
            token = brc.getRegex("\"token\":\"([a-z0-9]+)\"").getMatch(0);
            if (token != null) {
                additionalparameters = "?as3=t&oauth_token=" + token;
            }
        } else {
            logger.info("NOT logged in via decrypter");
        }
        br.getPage(parameter);
        if (parameter.matches("https?://(www\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+")) {
            parameter = new Regex(parameter, "https?://[^/]+/").getMatch(-1) + System.currentTimeMillis() + "/b/" + new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        if (br.containsHTML(">Sorry, we couldn\\'t find that stream\\.|<h1>This channel is closed</h1>|>I\\'m sorry, that page is in another castle") || br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(createOfflinelink(parameter, null));
            } catch (final Throwable t) {
                logger.log(t);
                logger.info("OfflineLink :" + parameter);
            }
            return decryptedLinks;
        }
        if (parameter.contains("/videos") && !new Regex(parameter, videoSingleHLS).matches()) {
            final String username = new Regex(parameter, "/([^<>\"/]*?)/videos").getMatch(0);
            String[] decryptAgainLinks = null;
            if (br.getURL().contains("/profile")) {
                final int step = 100;
                int maxVideos = 0;
                int offset = 0;
                do {
                    final Browser ajax = ajaxGetPage("https://api.twitch.tv/kraken/channels/" + username + "/videos?limit=100&offset=" + offset + "&on_site=1");
                    if (offset == 0) {
                        maxVideos = Integer.parseInt(ajax.getRegex("\"_total\":(\\d+)").getMatch(0));
                    }
                    decryptAgainLinks = ajax.getRegex("(/" + username + "/(b|c)/\\d+)\"").getColumn(0);
                    if (decryptAgainLinks == null || decryptAgainLinks.length == 0) {
                        logger.warning("Decrypter broken: " + parameter);
                        return null;
                    }
                    for (final String dl : decryptAgainLinks) {
                        decryptedLinks.add(createDownloadlink("https://twitch.tv" + dl));
                    }
                    offset += step;
                } while (decryptedLinks.size() < maxVideos);
            } else {
                if (br.containsHTML("<strong id=\"videos_count\">0")) {
                    logger.info("Nothing to decrypt here: " + parameter);
                    return decryptedLinks;
                }
                decryptAgainLinks = br.getRegex("(\\'|\")(/" + username + "/(b|c)/\\d+)\\1").getColumn(1);
                if (decryptAgainLinks == null || decryptAgainLinks.length == 0) {
                    logger.warning("Decrypter broken: " + parameter);
                    return null;
                }
                for (final String dl : decryptAgainLinks) {
                    decryptedLinks.add(createDownloadlink("https://twitch.tv" + dl));
                }
            }
        } else {
            String filename = null;
            String channelName = null;
            String date = null;
            String fpName = null;
            final FilePackage fp = FilePackage.getInstance();
            if (br.getURL().matches(videoSingleWeb)) {
                // no longer get videoname from html, it requires api call.
                Browser ajax = ajaxGetPage("https://api.twitch.tv/kraken/videos/" + (new Regex(parameter, "/b/\\d+$").matches() ? "a" : "c") + vid + "?on_site=1&");
                final Map<String, Object> ajaxMap = JSonStorage.restoreFromString(ajax.toString(), TypeRef.HASHMAP);
                filename = (String) ajaxMap.get("title");
                channelName = (String) JavaScriptEngineFactory.walkJson(ajaxMap, "channel/display_name");
                date = (String) ajaxMap.get("recorded_at");
                final String vdne = "Video does not exist";
                if (ajax != null && vdne.equals(PluginJSonUtils.getJsonValue(ajax, "message"))) {
                    decryptedLinks.add(createOfflinelink(parameter, vid + " - " + vdne, vdne));
                    return decryptedLinks;
                }
                String failreason = "Unknown server error";
                boolean failed = true;
                for (int i = 1; i <= 10; i++) {
                    try {
                        ajax = ajaxGetPage("https://api.twitch.tv/api/videos/" + (new Regex(parameter, "/b/\\d+$").matches() ? "a" : "c") + vid + additionalparameters);
                        if (ajax.containsHTML("\"restrictions\":\\{\"live\":\"chansub\"")) {
                            failreason = "Only downloadable for subscribers";
                        } else {
                            failed = false;
                        }
                        break;
                    } catch (final BrowserException e) {
                        logger.log(e);
                        this.sleep(5000l, param);
                    }
                }
                if (failed) {
                    decryptedLinks.add(createOfflinelink(parameter, vid + " - " + failreason, failreason));
                    return decryptedLinks;
                }
                if (filename == null) {
                    filename = vid;
                }
                /** Prefer highest quality */
                String used_quality = null;
                final String[][] qualities = { { "live_user_[A-Za-z0-9]+", "high" }, { "format_720p", "720p" }, { "format_480p", "480p" }, { "format_360p", "360p" }, { "format_240p", "240p" } };
                String[] links = null;
                for (final String current_quality[] : qualities) {
                    final String qual_regex = current_quality[0];
                    final String qual_name = current_quality[1];
                    links = ajax.getRegex("\"url\":\"(https?://[^<>\"]*?" + qual_regex + "[^\"]*_\\d+\\.flv)\"").getColumn(0);
                    if (links != null && links.length > 0) {
                        used_quality = qual_name;
                        break;
                    }
                }
                if (links == null || links.length == 0) {
                    used_quality = "standard";
                    links = ajax.getRegex("\"url\":\"(https?://[^<>\"]*?)\"").getColumn(0);
                }
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken: " + parameter);
                    return null;
                }
                filename = Encoding.htmlDecode(filename.trim());
                filename = filename.replaceAll("[\r\n#]+", "");
                int counter = 1;
                for (final String directlink : links) {
                    final DownloadLink dlink = createDownloadlink("http://twitchdecrypted.tv/" + System.currentTimeMillis() + new Random().nextInt(100000000));
                    dlink.setProperty("directlink", "true");
                    dlink.setProperty("plain_directlink", directlink);
                    dlink.setProperty("plainfilename", filename);
                    dlink.setProperty("partnumber", counter);
                    dlink.setProperty("quality", used_quality);
                    dlink.setProperty("vodid", vid);
                    if (date != null) {
                        dlink.setProperty("originaldate", date);
                    }
                    if (channelName != null) {
                        dlink.setProperty("channel", Encoding.htmlDecode(channelName.trim()));
                    }
                    dlink.setProperty("LINKDUPEID", "twitch:" + vid + ":" + counter);
                    final String formattedFilename = jd.plugins.hoster.TwitchTv.getFormattedFilename(dlink);
                    dlink.setName(formattedFilename);
                    if (cfg.getBooleanProperty(FASTLINKCHECK, false)) {
                        dlink.setAvailable(true);
                    }
                    dlink.setContentUrl(parameter);
                    decryptedLinks.add(dlink);
                    counter++;
                }
                if (channelName != null) {
                    fpName += Encoding.htmlDecode(channelName.trim()) + " - ";
                }
                if (date != null) {
                    try {
                        final String userDefinedDateFormat = cfg.getStringProperty("CUSTOM_DATE_2", "dd.MM.yyyy_HH-mm-ss");
                        final String[] dateStuff = date.split("T");
                        final String input = dateStuff[0] + ":" + dateStuff[1].replace("Z", "GMT");
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ssZ");
                        Date dateStr = formatter.parse(input);
                        String formattedDate = formatter.format(dateStr);
                        Date theDate = formatter.parse(formattedDate);
                        formatter = new SimpleDateFormat(userDefinedDateFormat);
                        formattedDate = formatter.format(theDate);
                        fpName += formattedDate + " - ";
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                fpName += filename;
                fpName += " - [" + links.length + "]" + " - " + used_quality;
                fp.setName(fpName);
                fp.addLinks(decryptedLinks);
            } else if (br.getURL().matches(videoSingleHLS)) {
                // they have multiple qualities, this would be defendant on uploaders original quality.
                // we need sig for next request
                Browser ajax = ajaxGetPage("https://api.twitch.tv/kraken/videos/v" + vid);
                if (ajax.getHttpConnection().getResponseCode() == 404) {
                    // offline
                    final String message = PluginJSonUtils.getJsonValue(ajax, "message");
                    decryptedLinks.add(createOfflinelink(parameter, vid + " - " + message, message));
                    return decryptedLinks;
                }
                Map<String, Object> ajaxMap = JSonStorage.restoreFromString(ajax.toString(), TypeRef.HASHMAP);
                filename = (String) ajaxMap.get("title");
                channelName = (String) JavaScriptEngineFactory.walkJson(ajaxMap, "channel/display_name");
                date = (String) ajaxMap.get("recorded_at");
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename = Encoding.htmlDecode(filename.trim());
                filename = filename.replaceAll("[\r\n#]+", "");
                ajax = this.ajaxGetPagePlayer("https://api.twitch.tv/api/vods/" + vid + "/access_token?api_version=5" + (token != null ? "&oauth_token=" + token : ""));
                ajaxMap = JSonStorage.restoreFromString(ajax.toString(), TypeRef.HASHMAP);
                final String auth = (String) ajaxMap.get("sig");
                // final String expire = PluginJSonUtils.getJson(ajax, "expires");
                final String privileged = (String) ajaxMap.get("privileged");
                final String tokenString = (String) ajaxMap.get("token");
                // auth required
                final String a = Encoding.urlEncode(tokenString);
                ajax = this.ajaxGetPagePlayer("https://usher.twitch.tv/vod/" + vid + ".m3u8?nauth=" + a + "&nauthsig=" + auth + "&player=twitchweb&allow_source=true");
                if (ajax.getHttpConnection().getResponseCode() == 403) {
                    // error handling for invalid token and or subscription based video/channel?
                    final String failreason = ("true".equalsIgnoreCase(privileged) ? "Subscription required" : "Login required");
                    decryptedLinks.add(createOfflinelink(parameter, vid + " - " + failreason, failreason));
                    return decryptedLinks;
                }
                final String[] medias = ajax.getRegex("#EXT-X-MEDIA([^\r\n]+[\r\n]+){2}[^\r\n]+").getColumn(-1);
                if (medias == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final ArrayList<DownloadLink> hlsStreams = new ArrayList<DownloadLink>();
                for (final String media : medias) {
                    // name = quality
                    // final String quality = new Regex(media, "NAME=\"(.*?)\"").getMatch(0);
                    final String bandwidth = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
                    final String m3u8 = new Regex(media, "https?://[^\r\n]+").getMatch(-1);
                    String fps = new Regex(media, "NAME\\s*=\\s*\"\\d+p(\\d+)").getMatch(0);
                    if (fps == null) {
                        fps = new Regex(media, "VIDEO\\s*=\\s*\"\\d+p(\\d+)").getMatch(0);
                    }
                    final DownloadLink dlink = createDownloadlink("http://twitchdecrypted.tv/" + System.currentTimeMillis() + new Random().nextInt(100000000));
                    if (fps != null) {
                        dlink.setProperty("fps", Integer.parseInt(fps));
                    }
                    dlink.setProperty("directlink", "true");
                    dlink.setProperty("m3u", m3u8);
                    dlink.setProperty("vodid", vid);
                    dlink.setProperty("plainfilename", filename);
                    // dlink.setProperty("quality", quality);
                    if (date != null) {
                        dlink.setProperty("originaldate", date);
                    }
                    if (channelName != null) {
                        dlink.setProperty("channel", Encoding.htmlDecode(channelName.trim()));
                    }
                    final String linkID = "twitch:" + vid + ":HLS:" + bandwidth + ":FPS:" + fps;
                    if (bandwidth != null) {
                        dlink.setProperty("hlsBandwidth", Integer.parseInt(bandwidth));
                    }
                    dlink.setLinkID(linkID);
                    // let linkchecking routine do all this!
                    // final String formattedFilename = jd.plugins.hoster.JustinTv.getFormattedFilename(dlink);
                    // dlink.setName(formattedFilename);
                    dlink.setName("linkcheck-failed-recheck-online-status-manually.mp4");
                    dlink.setContentUrl(parameter);
                    try {
                        ((jd.plugins.hoster.TwitchTv) plugin).setBrowser(br.cloneBrowser());
                        dlink.setAvailableStatus(((jd.plugins.hoster.TwitchTv) plugin).requestFileInformation(dlink));
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.log(e);
                        dlink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                    }
                    if (m3u8.contains("/chunked")) {
                        hlsStreams.add(dlink);
                    } else {
                        hlsStreams.add(0, dlink);
                    }
                }
                if (hlsStreams.size() == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // because its too awkward to know bitrate to p rating we online check, then confirm by ffprobe results
                if (true) {
                    // highest to lowest determines best. this is how there api returns 'source[chunked], high, medium, low, mobile'
                    // I assume that is... 1080, 720, 480, 360, 240, but it might not be!
                    boolean avoidChunked = this.getPluginConfig().getBooleanProperty("avoidChunked", true);
                    boolean q1080 = this.getPluginConfig().getBooleanProperty("q1080p", true);
                    boolean q720 = this.getPluginConfig().getBooleanProperty("q720p", true);
                    boolean q480 = this.getPluginConfig().getBooleanProperty("q480p", true);
                    boolean q360 = this.getPluginConfig().getBooleanProperty("q360p", true);
                    boolean q240 = this.getPluginConfig().getBooleanProperty("q240p", true);
                    final boolean fps60 = this.getPluginConfig().getBooleanProperty("60fps", true);
                    if (!fps60) {
                        final Iterator<DownloadLink> it = hlsStreams.iterator();
                        while (it.hasNext()) {
                            final DownloadLink next = it.next();
                            if (next.getIntegerProperty("fps", -1) == 60) {
                                it.remove();
                            }
                        }
                    }
                    // covers when users are idiots and disables all qualities.
                    if (!q1080 && !q720 && !q480 && !q360 && !q240) {
                        q1080 = true;
                        q720 = true;
                        q480 = true;
                        q360 = true;
                        q240 = true;
                    }
                    final boolean useBest = this.getPluginConfig().getBooleanProperty("useBest", true);
                    boolean chunked = false;
                    while (true) {
                        if (q1080 && (desiredLinks.isEmpty() || !useBest)) {
                            for (final DownloadLink downloadLink : hlsStreams) {
                                final int vidQual = downloadLink.getIntegerProperty("videoQuality", -1);
                                if (vidQual >= 1080 && (!avoidChunked || StringUtils.containsIgnoreCase(downloadLink.getStringProperty("m3u"), "/chunked") == chunked)) {
                                    desiredLinks.add(downloadLink);
                                }
                            }
                        }
                        if (q720 && (desiredLinks.isEmpty() || !useBest)) {
                            for (final DownloadLink downloadLink : hlsStreams) {
                                final int vidQual = downloadLink.getIntegerProperty("videoQuality", -1);
                                if (vidQual < 1080 && vidQual >= 720 && (!avoidChunked || StringUtils.containsIgnoreCase(downloadLink.getStringProperty("m3u"), "/chunked") == chunked)) {
                                    desiredLinks.add(downloadLink);
                                }
                            }
                        }
                        if (q480 && (desiredLinks.isEmpty() || !useBest)) {
                            for (final DownloadLink downloadLink : hlsStreams) {
                                final int vidQual = downloadLink.getIntegerProperty("videoQuality", -1);
                                if (vidQual < 720 && vidQual >= 480 && (!avoidChunked || StringUtils.containsIgnoreCase(downloadLink.getStringProperty("m3u"), "/chunked") == chunked)) {
                                    desiredLinks.add(downloadLink);
                                }
                            }
                        }
                        if (q360 && (desiredLinks.isEmpty() || !useBest)) {
                            for (final DownloadLink downloadLink : hlsStreams) {
                                final int vidQual = downloadLink.getIntegerProperty("videoQuality", -1);
                                if (vidQual < 480 && vidQual >= 360 && (!avoidChunked || StringUtils.containsIgnoreCase(downloadLink.getStringProperty("m3u"), "/chunked") == chunked)) {
                                    desiredLinks.add(downloadLink);
                                }
                            }
                        }
                        if (q240 && (desiredLinks.isEmpty() || !useBest)) {
                            for (final DownloadLink downloadLink : hlsStreams) {
                                final int vidQual = downloadLink.getIntegerProperty("videoQuality", -1);
                                if (vidQual < 360 && vidQual >= 240 && (!avoidChunked || StringUtils.containsIgnoreCase(downloadLink.getStringProperty("m3u"), "/chunked") == chunked)) {
                                    desiredLinks.add(downloadLink);
                                }
                            }
                        }
                        if (chunked == true) {
                            break;
                        } else {
                            chunked = true;
                        }
                    }
                }
                if (desiredLinks.size() == 0) {
                    throw new DecrypterRetryException(RetryReason.PLUGIN_SETTINGS);
                }
                if (channelName != null) {
                    fpName = (fpName == null ? "" : fpName) + Encoding.htmlDecode(channelName.trim()) + " - ";
                }
                if (date != null) {
                    try {
                        final String userDefinedDateFormat = cfg.getStringProperty("CUSTOM_DATE_2", "dd.MM.yyyy_HH-mm-ss");
                        final String[] dateStuff = date.split("T");
                        final String input = dateStuff[0] + ":" + dateStuff[1].replace("Z", "GMT");
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ssZ");
                        Date dateStr = formatter.parse(input);
                        String formattedDate = formatter.format(dateStr);
                        Date theDate = formatter.parse(formattedDate);
                        formatter = new SimpleDateFormat(userDefinedDateFormat);
                        formattedDate = formatter.format(theDate);
                        fpName += formattedDate + " - ";
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                fpName += filename;
                fp.setName(fpName);
                fp.addLinks(desiredLinks);
            } else {
                // unsupported feature
                decryptedLinks.add(createOfflinelink(parameter, null));
            }
            // chat logs?, needs to be here so it can get filename stuff from other if statements setters
            if (this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TwitchTv.grabChatHistory, jd.plugins.hoster.TwitchTv.defaultGrabChatHistory)) {
                // create download link just for this, and continue.
                final DownloadLink dlink = createDownloadlink("http://twitchdecrypted.tv/" + System.currentTimeMillis() + new Random().nextInt(100000000));
                dlink.setProperty(jd.plugins.hoster.TwitchTv.grabChatHistory, true);
                final String linkID = "twitch:" + vid + ":" + jd.plugins.hoster.TwitchTv.grabChatHistory;
                dlink.setLinkID(linkID);
                dlink.setProperty("plainfilename", filename);
                // dlink.setProperty("quality", quality);
                if (date != null) {
                    dlink.setProperty("originaldate", date);
                }
                if (channelName != null) {
                    dlink.setProperty("channel", Encoding.htmlDecode(channelName.trim()));
                }
                dlink.setProperty("extension", " - Chat History.txt");
                final String formattedFilename = jd.plugins.hoster.TwitchTv.getFormattedFilename(dlink);
                dlink.setName(formattedFilename);
                fp.add(dlink);
                if (!desiredLinks.isEmpty()) {
                    desiredLinks.add(dlink);
                } else {
                    decryptedLinks.add(dlink);
                }
            }
        }
        return !desiredLinks.isEmpty() ? desiredLinks : decryptedLinks;
    }

    @Override
    public void clean() {
        try {
            final PluginForHost plugin = this.plugin;
            this.plugin = null;
            if (plugin != null) {
                plugin.clean();
            }
        } finally {
            super.clean();
        }
    }

    PluginForHost plugin = null;

    private boolean getUserLogin(final boolean force) throws Exception {
        plugin = JDUtilities.getNewPluginForHostInstance(getHost());
        if (plugin == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Hoster plugin missing!?");
        } else {
            final Account aa = AccountController.getInstance().getValidAccount(getHost());
            if (aa == null) {
                logger.warning("There is no account available, stopping...");
                return false;
            } else {
                try {
                    plugin.setLogger(getLogger());
                    ((jd.plugins.hoster.TwitchTv) plugin).login(this.br, aa, force);
                    // set from cookie value after login.
                    // Set-Cookie:api_token=[a-f0-9]{32}; domain=.twitch.tv; path=/; expires=Wed, 29-Apr-2015 01:00:05 GMT
                    userApiToken = br.getCookie(this.getHost(), "api_token");
                    // userId is present in another cookie?
                    return true;
                } catch (final PluginException e) {
                    handleAccountException(aa, e);
                    return false;
                }
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}