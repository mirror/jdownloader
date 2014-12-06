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
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.logging2.LogSource;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "twitch.tv" }, urls = { "http://((www\\.|[a-z]{2}\\.)?(twitchtv\\.com|twitch\\.tv)/(?!directory)[^<>/\"]+/((b|c)/\\d+|videos(\\?page=\\d+)?)|(www\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+)" }, flags = { 0 })
public class JustinTvDecrypt extends PluginForDecrypt {

    public JustinTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser ajax = null;

    private void ajaxGetPage(final String string) throws IOException {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/vnd.twitchtv.v3+json");
        ajax.getHeaders().put("Referer", "http://api.twitch.tv/crossdomain/receiver.html?v=2");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getPage(string);
    }

    private final String FASTLINKCHECK = "FASTLINKCHECK";
    private final String SINGLEVIDEO   = "http://((www\\.|[a-z]{2}\\.)?(twitchtv\\.com|twitch\\.tv)/[^<>/\"]+/((b|c)/\\d+)|(www\\.)?twitch\\.tv/archive/archive_popout\\?id=\\d+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = this.getPluginConfig();
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
            if (!br.getURL().matches(SINGLEVIDEO)) {
                try {
                    decryptedLinks.add(createOfflinelink(parameter, null));
                } catch (final Throwable t) {
                    logger.info("OfflineLink :" + parameter);
                }
                return decryptedLinks;
            }
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
                links = ajax.getRegex("\"url\":\"(https?://[^<>\"]*?" + qual_regex + "_\\d+\\.flv)\"").getColumn(0);
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
        }
        return decryptedLinks;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("twitch.tv");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
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