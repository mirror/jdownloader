//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datpiff.com" }, urls = { "https?://(?:www\\.)?datpiff\\.com/([^<>\"% ]*?\\-download(\\-track)?\\.php\\?id=[a-z0-9]+|mixtapes\\-detail\\.php\\?id=\\d+|.*?\\-mixtape\\.\\d+\\.html)" })
public class DatPiffCom extends PluginForHost {
    private static final String PATTERN_PREMIUMONLY               = "(?i)>\\s*you must be logged in to download mixtapes<";
    private static final String ONLYREGISTEREDUSERTEXT            = "Only downloadable for registered users";
    private static final String PATTERN_CURRENTLYUNAVAILABLE      = "(?i)>\\s*This is most likely because the uploader is currently making changes";
    private static final String CURRENTLYUNAVAILABLETEXT          = "Currently unavailable";
    public static final String  PROPERTY_ARTIST                   = "artist";
    public static final String  PROPERTY_SONG_MIXTAPE_DOWNLOAD_ID = "mixtape_download_id";
    public static final String  PROPERTY_SONG_MIXTAPE_STREAM_ID   = "mixtape_stream_id";
    public static final String  PROPERTY_SONG_TRACK_POSITION      = "track_position";
    public static final String  PROPERTY_SONG_TRACK_DOWNLOAD_ID   = "track_download_id";
    private static final String TYPE_MIXTAPE                      = "https?://[^/]+/[A-Za-z0-9\\-_]+\\-mixtape\\.(\\d+)\\.html";
    private static final String TYPE_SONG                         = "https?://[^/]+/pop-download-track\\.php\\?id=(\\d+)";
    private static final String TYPE_ALBUM                        = "https?://[^/]+/pop-mixtape-download\\.php\\?id=(mb\\d+)";

    public DatPiffCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.datpiff.com/register");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML(ONLYREGISTEREDUSERTEXT)) {
            link.getLinkStatus().setStatusText(ONLYREGISTEREDUSERTEXT);
            return AvailableStatus.TRUE;
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)(>\\s*A zip file has not yet been generated\\s*<|>\\s*Mixtape Not Found\\s*<|has been removed<)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getPluginPatternMatcher().matches(TYPE_SONG)) {
            final String title = br.getRegex("<span>Download Track<em>([^<>\"]+)</em>").getMatch(0);
            if (title != null) {
                link.setName(Encoding.htmlDecode(title).trim() + ".mp3");
            }
        } else {
            /* TYPE_MIXTAPE */
            String filename = null;
            if (isOfficialDownloadUnavailable(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(PATTERN_CURRENTLYUNAVAILABLE)) {
                filename = br.getRegex("<title>([^<>\"]*?) \\- Mixtapes @ DatPiff\\.com</title>").getMatch(0);
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setName(Encoding.htmlDecode(filename.trim()));
                link.getLinkStatus().setStatusText(CURRENTLYUNAVAILABLETEXT);
                return AvailableStatus.TRUE;
            }
            filename = br.getRegex("<title>Download Mixtape \\&quot;(.*?)\\&quot;</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<span>Download Mixtape<em>(.*?)</em></span>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<span>Download Track<em>([^<>\"]*?)</em>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (filename == null) {
                final String downloadButton = br.getRegex("(?-s)iframe src=\"(https?://.*?embed/.*?/\\?downloadbutton=\\d+)").getMatch(0);
                if (downloadButton != null) {
                    filename = br.getRegex("property=\"og:url\" content=\".*?\\.com/(.*?)\\.\\d+\\.html?").getMatch(0);
                    if (filename != null) {
                        filename = filename + ".zip";
                    }
                    link.setName(Encoding.htmlDecode(filename.trim()));
                    return AvailableStatus.TRUE;
                }
            }
            if (filename == null) {
                /* Fallback */
                filename = new Regex(br.getURL(), ".*/([^/]+)\\.html$").getMatch(0);
            }
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename.trim()) + ".zip");
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isOfficialDownloadUnavailable(final Browser br) {
        return br.containsHTML("(?i)>\\s*The uploader has disabled downloads") || br.containsHTML("(?i)>\\s*Download Unavailable\\s*<");
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        // Untested
        // final String timeToRelease =
        // br.getRegex("\\'dateTarget\\': (\\d+),").getMatch(0);
        // if (timeToRelease != null) throw new
        // PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
        // "Not yet released", Long.parseLong(timeToRelease) -
        // System.currentTimeMillis());
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            requestFileInformation(link);
            if (link.getPluginPatternMatcher().matches(TYPE_SONG)) {
                /* Download single song */
                if (isOfficialDownloadUnavailable(br)) {
                    logger.info("Official download is disabled --> Attempting stream download");
                    final String trackDownloadID = link.getStringProperty(PROPERTY_SONG_TRACK_DOWNLOAD_ID);
                    // final String mixtapeStreamID = link.getStringProperty(PROPERTY_SONG_MIXTAPE_STREAM_ID);
                    final String mixtapeDownloadID = link.getStringProperty(PROPERTY_SONG_MIXTAPE_DOWNLOAD_ID);
                    final String trackPosition = link.getStringProperty(PROPERTY_SONG_TRACK_POSITION);
                    if (trackDownloadID == null || mixtapeDownloadID == null || trackPosition == null) {
                        /*
                         * Maybe user has added single URL without crawler --> Required properties are not given --> No way to attempt
                         * stream download!
                         */
                        throw new PluginException(LinkStatus.ERROR_FATAL, "The uploader has disabled downloads");
                    }
                    // br.getPage("/player/" + mixtapeStreamID + "?tid=" + trackPosition);
                    br.getPage("https://embeds.datpiff.com/mixtape/" + mixtapeDownloadID + "?trackid=" + trackPosition + "&platform=desktop");
                    final String trackPrefix = br.getRegex("var trackPrefix\\s*=\\s*'(https?://[^\\']+)'").getMatch(0);
                    final String trackServerFilename = br.getRegex("id\":" + trackDownloadID + ", \"title\":\"[^\"]+\", \"artist\":playerData\\.artist,\"mfile\":trackPrefix\\.concat\\( '([^\\']+)'").getMatch(0);
                    if (trackPrefix == null || trackServerFilename == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = trackPrefix + trackServerFilename;
                } else if (br.containsHTML("(?i)>\\s*This track contains commercially-available content which we can not legally offer for download")) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This track is not downloadable");
                }
                if (dllink == null) {
                    final Form dlform = br.getFormbyProperty("id", "loginform");
                    if (dlform == null) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Broken track");
                    }
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(false);
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    brc.submitForm(dlform);
                    dllink = brc.getRedirectLocation();
                }
            } else {
                /* Download complete mixtape as .zip file. */
                if (br.containsHTML(PATTERN_PREMIUMONLY)) {
                    throw new AccountRequiredException();
                } else if (br.containsHTML(PATTERN_CURRENTLYUNAVAILABLE)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, CURRENTLYUNAVAILABLETEXT, 3 * 60 * 60 * 1000l);
                }
                if (br.getURL().matches(TYPE_ALBUM)) {
                    logger.info("We've already accessed the target URL: " + br.getURL());
                } else {
                    final String downloadID = br.getRegex("openDownload\\(\\s*'([^<>\"\\']+)").getMatch(0);
                    if (downloadID == null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not (yet) downloadable");
                    }
                    br.getPage("/pop-mixtape-download.php?id=" + downloadID);
                }
                final Form form = br.getForm(0);
                if (form != null) {
                    if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(form)) {
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    br.setFollowRedirects(false);
                    br.submitForm(form);
                    br.setFollowRedirects(true);
                    dllink = br.getRedirectLocation();
                }
                if (dllink == null) {
                    // whole mixtape
                    String action = br.getRegex("id=\"loginform\" action=\"(/[^<>\"]*?)\"").getMatch(0);
                    if (action == null) {
                        action = br.getRegex("<form action=\"(/[^<>\"]*?)\"").getMatch(0);
                    }
                    if (action == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    for (int i = 1; i <= 3; i++) {
                        String id = br.getRegex("name=\"id\" value=\"([^<>\"]*?)\"").getMatch(0);
                        if (id == null) {
                            id = new Regex(link.getDownloadURL(), "\\.php\\?id=(.+)").getMatch(0);
                        }
                        if (id == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        String postData = "id=" + id + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100);
                        if (br.containsHTML("solvemedia\\.com/papi/")) {
                            final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                            File cf = null;
                            try {
                                cf = sm.downloadCaptcha(getLocalCaptchaFile());
                            } catch (final Exception e) {
                                if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                    throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support", e);
                                } else {
                                    throw e;
                                }
                            }
                            final String code = getCaptchaCode("solvemedia", cf, link);
                            final String chid = sm.getChallenge(code);
                            postData += "&cmd=downloadsolve&adcopy_response=" + Encoding.urlEncode(code) + "&adcopy_challenge=" + Encoding.urlEncode(chid);
                        }
                        br.postPage(action, postData);
                        dllink = br.getRedirectLocation();
                        if (dllink == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (dllink.matches("https?://(www\\.)?datpiff\\.com/pop\\-mixtape\\-download\\.php\\?id=[A-Za-z0-9]+")) {
                            br.getPage(dllink);
                            continue;
                        }
                        break;
                    }
                }
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (dllink.matches("https?://[^/]+/pop\\-mixtape\\-download\\.php\\?id=[A-Za-z0-9]+")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String headerFilename = getFileNameFromHeader(dl.getConnection());
        if (headerFilename != null) {
            link.setFinalFileName(headerFilename);
        }
        link.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openGetConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    try {
                        br2.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private void login(final Account account) throws Exception {
        if (true) {
            /* 2021-09-29: This is broken */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        setBrowserExclusive();
        br.postPage("https://www." + account.getHoster() + "/login", "cmd=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getRedirectLocation() == null) { // Should be /account if OK, or /login if failed.
            for (int i = 0; i < 3; i++) { // Sometimes retry is needed, login page is displayed without redirect.
                Thread.sleep(3 * 1000);
                br.postPage("https://www." + account.getHoster() + "/login", "cmd=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getRedirectLocation() != null) {
                    break;
                }
            }
        }
        if (br.getCookie(br.getHost(), "mcim", Cookies.NOTDELETEDPATTERN) == null || br.getCookie(br.getHost(), "lastuser", Cookies.NOTDELETEDPATTERN) == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account);
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://www.datpiff.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account);
        handleDownload(link, account);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* Captchas may happen in all modes */
        return true;
    }
}