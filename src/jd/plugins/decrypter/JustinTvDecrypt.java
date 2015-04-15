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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.logging2.LogSource;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "twitch.tv" }, urls = { "http://((www\\.|[a-z]{2}\\.)?(twitchtv\\.com|twitch\\.tv)/(?!directory)[^<>/\"]+/((b|c|v)/\\d+|videos(\\?page=\\d+)?)|(www\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+)" }, flags = { 0 })
public class JustinTvDecrypt extends PluginForDecrypt {

    public JustinTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser ajax         = null;
    private String  userApiToken = null;
    private String  userId       = null;

    private void ajaxGetPage(final String string) throws IOException {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/vnd.twitchtv.v3+json");
        ajax.getHeaders().put("Referer", "http://api.twitch.tv/crossdomain/receiver.html?v=2");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (userApiToken != null) {
            ajax.getHeaders().put("Twitch-Api-Token", userApiToken);
        }
        ajax.getPage(string);
    }

    private void ajaxGetPagePlayer(final String string) throws IOException {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("X-Requested-With", "ShockwaveFlash/17.0.0.134");
        ajax.getPage(string);
    }

    private final String FASTLINKCHECK  = "FASTLINKCHECK";
    private final String videoSingleWeb = "https?://(?:(?:www\\.|[a-z]{2}\\.)?(?:twitchtv\\.com|twitch\\.tv)/[^<>/\"]+/((b|c)/\\d+)|(?:www\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+)";
    private final String videoSingleHLS = "https?://(?:(?:www\\.|[a-z]{2}\\.)?(?:twitchtv\\.com|twitch\\.tv)/[^<>/\"]+/v/\\d+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> desiredLinks = new ArrayList<DownloadLink>();

        final SubConfiguration cfg = this.getPluginConfig();
        br = new Browser();
        br.setCookie("http://twitch.tv", "language", "en-au");
        // redirects occur to de.domain when browser accept language set to German!
        br.getHeaders().put("Accept-Language", "en-gb");
        // currently redirect to www.
        String parameter = param.toString().replaceAll("://([a-z]{2}\\.)?(twitchtv\\.com|twitch\\.tv)", "://www.twitch.tv");
        br.setFollowRedirects(true);

        /* Log in if possible to be able to download "for subscribers only" videos */
        String token = null;
        String additionalparameters = "";
        if (getUserLogin(false)) {
            logger.info("Logged in via decrypter");
            br.getPage("https://api.twitch.tv/api/viewer/token.json?as3=t");
            token = br.getRegex("\"token\":\"([a-z0-9]+)\"").getMatch(0);
            if (token != null) {
                additionalparameters = "?as3=t&oauth_token=" + token;
            }
        } else {
            logger.info("NOT logged in via decrypter");
        }

        br.getPage(parameter);
        if (parameter.matches("http://(www\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+")) {
            parameter = "http://www.twitch.tv/" + System.currentTimeMillis() + "/b/" + new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        final String vid = new Regex(parameter, "(\\d+)$").getMatch(0);
        if (br.containsHTML(">Sorry, we couldn\\'t find that stream\\.|<h1>This channel is closed</h1>|>I\\'m sorry, that page is in another castle") || br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(createOfflinelink(parameter, null));
            } catch (final Throwable t) {
                logger.info("OfflineLink :" + parameter);
            }
            return decryptedLinks;
        }
        pluginsLoaded();
        if (parameter.contains("/videos")) {
            final String username = new Regex(parameter, "/([^<>\"/]*?)/videos").getMatch(0);
            String[] decryptAgainLinks = null;
            if (br.getURL().contains("/profile")) {
                final int step = 100;
                int maxVideos = 0;
                int offset = 0;
                do {
                    ajaxGetPage("http://api.twitch.tv/kraken/channels/" + username + "/videos?limit=100&offset=" + offset + "&on_site=1");
                    if (offset == 0) {
                        maxVideos = Integer.parseInt(ajax.getRegex("\"_total\":(\\d+)").getMatch(0));
                    }
                    decryptAgainLinks = ajax.getRegex("(/" + username + "/(b|c)/\\d+)\"").getColumn(0);
                    if (decryptAgainLinks == null || decryptAgainLinks.length == 0) {
                        logger.warning("Decrypter broken: " + parameter);
                        return null;
                    }
                    for (final String dl : decryptAgainLinks) {
                        decryptedLinks.add(createDownloadlink("http://twitch.tv" + dl));
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
                    decryptedLinks.add(createDownloadlink("http://twitch.tv" + dl));
                }
            }
        } else {
            if (br.getURL().matches(videoSingleWeb)) {
                // no longer get videoname from html, it requires api call.
                ajaxGetPage("http://api.twitch.tv/kraken/videos/" + (new Regex(parameter, "/b/\\d+$").matches() ? "a" : "c") + vid + "?on_site=1&");
                String filename = getJson(ajax, "title");
                final String channelName = getJson(ajax, "display_name");
                final String date = getJson(ajax, "recorded_at");
                final String vdne = "Video does not exist";
                if (ajax != null && vdne.equals(getJson(ajax, "message"))) {
                    try {
                        decryptedLinks.add(createOfflinelink(parameter, vid + " - " + vdne, vdne));
                    } catch (final Throwable t) {
                        logger.info("OfflineLink :" + parameter);
                    }
                    return decryptedLinks;
                }
                String failreason = "Unknown server error";
                boolean failed = true;
                for (int i = 1; i <= 10; i++) {
                    try {
                        ajaxGetPage("https://api.twitch.tv/api/videos/" + (new Regex(parameter, "/b/\\d+$").matches() ? "a" : "c") + vid + additionalparameters);
                        if (ajax.containsHTML("\"restrictions\":\\{\"live\":\"chansub\"")) {
                            failreason = "Only downloadable for subscribers";
                        } else {
                            failed = false;
                        }
                        break;
                    } catch (final BrowserException e) {
                        this.sleep(5000l, param);
                    }
                }
                if (failed) {
                    try {
                        decryptedLinks.add(createOfflinelink(parameter, vid + " - " + failreason, failreason));
                    } catch (final Throwable t) {
                        logger.info("OfflineLink :" + parameter);
                    }
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
                    if (date != null) {
                        dlink.setProperty("originaldate", date);
                    }
                    if (channelName != null) {
                        dlink.setProperty("channel", Encoding.htmlDecode(channelName.trim()));
                    }
                    dlink.setProperty("LINKDUPEID", "twitch" + vid + "_" + counter);
                    final String formattedFilename = jd.plugins.hoster.JustinTv.getFormattedFilename(dlink);
                    dlink.setName(formattedFilename);
                    if (cfg.getBooleanProperty(FASTLINKCHECK, false)) {
                        dlink.setAvailable(true);
                    }
                    try {
                        dlink.setContentUrl(parameter);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                    }
                    decryptedLinks.add(dlink);
                    counter++;
                }

                String fpName = "";
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
                        LogSource.exception(logger, e);
                    }
                }
                fpName += filename;
                fpName += " - [" + links.length + "]" + " - " + used_quality;
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName);
                fp.addLinks(decryptedLinks);
            } else if (br.getURL().matches(videoSingleHLS)) {
                if (System.getProperty("jd.revision.jdownloaderrevision") == null) {
                    logger.warning("HLS is only supported in JDownloader 2.");
                    return decryptedLinks;
                }
                // they have multiple qualities, this would be defendant on uploaders original quality.
                // we need sig for next request
                // https://api.twitch.tv/api/vods/3707868/access_token?as3=t
                ajaxGetPage("http://api.twitch.tv/kraken/videos/v" + vid + "?on_site=1");
                if (ajax.getHttpConnection().getResponseCode() == 404) {
                    // offline
                    final String message = getJson(ajax, "message");
                    try {
                        decryptedLinks.add(createOfflinelink(parameter, vid + " - " + message, message));
                    } catch (final Throwable t) {
                        logger.info("OfflineLink :" + parameter);
                    }
                    return decryptedLinks;
                }
                String filename = getJson(ajax, "title");
                final String channelName = getJson(ajax, "display_name");
                final String date = getJson(ajax, "recorded_at");
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename = Encoding.htmlDecode(filename.trim());
                filename = filename.replaceAll("[\r\n#]+", "");
                this.ajaxGetPagePlayer("http://api.twitch.tv/api/vods/" + vid + "/access_token?as3=t" + (token != null ? "&oauth_token=" + token : ""));
                // {"token":"{\"user_id\":null,\"vod_id\":3707868,\"expires\":1421924057,\"chansub\":{\"restricted_bitrates\":[]},\"privileged\":false}","sig":"a73d0354f84e8122d78b14f47552e0f83217a89e"}
                final String auth = getJson(ajax, "sig");
                final String expire = getJson(ajax.toString().replaceAll("\\\\\"", "\""), "expires");
                final String privileged = getJson(ajax.toString().replaceAll("\\\\\"", "\""), "privileged");
                // auth required
                // http://usher.twitch.tv/vod/3707868?nauth=%7B%22user_id%22%3Anull%2C%22vod_id%22%3A3707868%2C%22expires%22%3A1421885482%2C%22chansub%22%3A%7B%22restricted_bitrates%22%3A%5B%5D%7D%2C%22privileged%22%3Afalse%7D&nauthsig=d4ecb4772b28b224accbbc4711dff1c786725ce9
                final String a = Encoding.urlEncode("{\"user_id\":" + (userId != null ? userId : "null") + ",\"vod_id\":" + vid + ",\"expires\":" + expire + ",\"chansub\":{\"restricted_bitrates\":[]},\"privileged\":" + privileged + "}") + "&nauthsig=" + auth;
                this.ajaxGetPagePlayer("http://usher.twitch.tv/vod/" + vid + "?nauth=" + a);
                // #EXTM3U
                // #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="chunked",NAME="Source",AUTOSELECT=YES,DEFAULT=YES
                // #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=3428253,CODECS="avc1.4D4029,mp4a.40.2",VIDEO="chunked"
                // http://vod.ak.hls.ttvnw.net/v1/AUTH_system/vods_edbf/adren_tv_12744116464_192799820/chunked/index-dvr.m3u8
                // #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="high",NAME="High",AUTOSELECT=YES,DEFAULT=YES
                // #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1590189,CODECS="avc1.42C01F,mp4a.40.2",VIDEO="high"
                // http://vod.ak.hls.ttvnw.net/v1/AUTH_system/vods_edbf/adren_tv_12744116464_192799820/high/index-dvr.m3u8
                // #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="medium",NAME="Medium",AUTOSELECT=YES,DEFAULT=YES
                // #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=880744,CODECS="avc1.42C01E,mp4a.40.2",VIDEO="medium"
                // http://vod.ak.hls.ttvnw.net/v1/AUTH_system/vods_edbf/adren_tv_12744116464_192799820/medium/index-dvr.m3u8
                // #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="low",NAME="Low",AUTOSELECT=YES,DEFAULT=YES
                // #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=617200,CODECS="avc1.42C01E,mp4a.40.2",VIDEO="low"
                // http://vod.ak.hls.ttvnw.net/v1/AUTH_system/vods_edbf/adren_tv_12744116464_192799820/low/index-dvr.m3u8
                // #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="mobile",NAME="Mobile",AUTOSELECT=YES,DEFAULT=YES
                // #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=271909,CODECS="avc1.42C00D,mp4a.40.2",VIDEO="mobile"
                // http://vod.ak.hls.ttvnw.net/v1/AUTH_system/vods_edbf/adren_tv_12744116464_192799820/mobile/index-dvr.m3u8
                if (ajax.getHttpConnection().getResponseCode() == 403) {
                    // error handling for invalid token and or subscription based video/channel?
                    try {
                        final String failreason = ("true".equalsIgnoreCase(privileged) ? "Subscription required" : "Login required");
                        decryptedLinks.add(createOfflinelink(parameter, vid + " - " + failreason, failreason));
                    } catch (final Throwable t) {
                        logger.info("OfflineLink :" + parameter);
                    }
                    return decryptedLinks;
                }
                final String[] medias = ajax.getRegex("#EXT-X-MEDIA([^\r\n]+[\r\n]+){3}").getColumn(-1);
                if (medias == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final String media : medias) {
                    // name = quality
                    // final String quality = new Regex(media, "NAME=\"(.*?)\"").getMatch(0);
                    final String bw = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
                    final String m3u8 = new Regex(media, "https?://[^\r\n]+").getMatch(-1);

                    final DownloadLink dlink = createDownloadlink("http://twitchdecrypted.tv/" + System.currentTimeMillis() + new Random().nextInt(100000000));
                    dlink.setProperty("directlink", "true");
                    dlink.setProperty("m3u", m3u8);
                    dlink.setProperty("plainfilename", filename);
                    // dlink.setProperty("quality", quality);
                    if (date != null) {
                        dlink.setProperty("originaldate", date);
                    }
                    if (channelName != null) {
                        dlink.setProperty("channel", Encoding.htmlDecode(channelName.trim()));
                    }
                    final String linkID = "twitch:" + vid + ":HLS:" + bw;
                    try {
                        dlink.setLinkID(linkID);
                    } catch (final Throwable t) {
                        dlink.setProperty("LINKDUPEID", linkID);
                    }
                    // let linkchecking routine do all this!
                    // final String formattedFilename = jd.plugins.hoster.JustinTv.getFormattedFilename(dlink);
                    // dlink.setName(formattedFilename);
                    dlink.setName("linkcheck-failed-recheck-online-status-manually.mp4");
                    try {
                        dlink.setContentUrl(parameter);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                    }
                    try {
                        ((jd.plugins.hoster.JustinTv) plugin).setBrowser(br.cloneBrowser());
                        dlink.setAvailableStatus(((jd.plugins.hoster.JustinTv) plugin).requestFileInformation(dlink));
                    } catch (Exception e) {
                        dlink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                    }
                    decryptedLinks.add(dlink);
                }
                // because its too akward to know bitrate to p rating we online check, then confirm by ffprobe results
                if (true) {
                    // highest to lowest determines best. this is how there api returns 'source[chunked], high, medium, low, mobile'
                    // I assume that is... 1080, 720, 480, 360, 240, but it might not be!
                    boolean q1080 = this.getPluginConfig().getBooleanProperty("q1080p", true);
                    boolean q720 = this.getPluginConfig().getBooleanProperty("q720p", true);
                    boolean q480 = this.getPluginConfig().getBooleanProperty("q480p", true);
                    boolean q360 = this.getPluginConfig().getBooleanProperty("q360p", true);
                    boolean q240 = this.getPluginConfig().getBooleanProperty("q240p", true);
                    // covers when users are idiots and disables all qualities.
                    if (!q1080 && !q720 && !q480 && !q360 && !q240) {
                        q1080 = true;
                        q720 = true;
                        q480 = true;
                        q360 = true;
                        q240 = true;
                    }
                    final boolean useBest = this.getPluginConfig().getBooleanProperty("useBest", true);

                    for (final DownloadLink downloadLink : decryptedLinks) {
                        // chunked and be 1080 outside of norm parmaeters and can have standard size entry also.. so a second 1080. We will
                        // assume chunked is best (first one). entry one should be the best in this situation, but we must match against
                        // user setting.
                        final int vidQual = downloadLink.getIntegerProperty("videoQuality", -1);
                        if (desiredLinks.isEmpty() || !useBest) {
                            // videos fall within ranges, not always right on the quality specified above.
                            if (vidQual >= 1080) {
                                if (q1080) {
                                    desiredLinks.add(downloadLink);
                                }
                            } else if (vidQual <= 1080 && vidQual >= 720) {
                                if (q720) {
                                    desiredLinks.add(downloadLink);
                                }
                            } else if (vidQual <= 720 && vidQual >= 480) {
                                if (q480) {
                                    desiredLinks.add(downloadLink);
                                }
                            } else if (vidQual <= 480 && vidQual >= 360) {
                                if (q360) {
                                    desiredLinks.add(downloadLink);
                                }
                            } else if (vidQual <= 360 && vidQual >= 240) {
                                if (q240) {
                                    desiredLinks.add(downloadLink);
                                }
                            }
                        }
                    }
                }
                String fpName = "";
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
                        LogSource.exception(logger, e);
                    }
                }
                fpName += filename;
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName);
                fp.addLinks(decryptedLinks);
            } else {
                // unsupported feature
                try {
                    decryptedLinks.add(createOfflinelink(parameter, null));
                } catch (final Throwable t) {
                    logger.info("OfflineLink :" + parameter);
                }
            }
        }
        return !desiredLinks.isEmpty() ? desiredLinks : decryptedLinks;
    }

    PluginForHost plugin = null;

    private boolean getUserLogin(final boolean force) throws Exception {
        plugin = JDUtilities.getPluginForHost("twitch.tv");
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            jd.plugins.hoster.JustinTv.login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        // set from cookie value after login.
        // Set-Cookie:api_token=[a-f0-9]{32}; domain=.twitch.tv; path=/; expires=Wed, 29-Apr-2015 01:00:05 GMT
        userApiToken = br.getCookie(this.getHost(), "api_token");
        // userId is present in another cookie?
        userId = br.getCookie(this.getHost(), "persistent");
        if (userId != null) {
            userId = new Regex(userId, "^(\\d+)").getMatch(0);
        }
        return true;
    }

    private final static AtomicBoolean pL = new AtomicBoolean(false);

    private void pluginsLoaded() {
        if (!pL.get()) {
            /* make sure the plugin is loaded! */
            JDUtilities.getPluginForHost("twitch.tv");
            pL.set(true);
        }
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}