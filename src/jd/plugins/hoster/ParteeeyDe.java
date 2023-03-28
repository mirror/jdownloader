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
package jd.plugins.hoster;

import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "parteeey.de" }, urls = { "https?://(?:www\\.)?parteeey\\.de/(?:#mulFile\\-|galerie/datei\\?p=)\\d+" })
public class ParteeeyDe extends PluginForHost {
    public ParteeeyDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.parteeey.de/registrierung");
    }

    @Override
    public String getAGBLink() {
        return "https://www.parteeey.de/nutzungsbedingungen";
    }

    /* Connection stuff - disable resume & chunks to keep serverload low. */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;
    public static final String   default_extension = ".jpg";
    private String               dllink            = null;
    private boolean              server_issues     = false;

    public void correctDownloadLink(final DownloadLink link) {
        link.setContentUrl(String.format("https://www.parteeey.de/galerie/datei?p=%s", getFID(link)));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        server_issues = false;
        final String fid = getFID(link);
        link.setLinkID(this.getHost() + "://" + fid);
        this.setBrowserExclusive();
        prepBR(this.br);
        if (account == null) {
            link.getLinkStatus().setStatusText("Account needed for linkcheck- and download");
            return AvailableStatus.UNCHECKABLE;
        }
        String urlThumb = link.getStringProperty("thumburl", null);
        String filename = null;
        final String filename_decrypter = link.getStringProperty("decrypterfilename", null);
        final String galleryid = link.getStringProperty("galleryid", null);
        /* 2017-10-06: Official downloads are disabled (seems like that requires special permissions on users' accounts from now on) */
        final boolean downloadAllowed = false;
        if (downloadAllowed) {
            /*
             * 2016-08-21: Prefer download via official download function as this gives us the highest quality possible - usually a bit(some
             * KBs) better than the image displayed in the gallery via browser.
             */
            /* Login with validating cookies */
            login(account, true);
            dllink = "https://www." + this.getHost() + "/galerie/datei/herunterladen/" + fid;
        } else {
            final String original_filename_in_thumbnail = getFilenameFromThumbnailDirecturl(urlThumb);
            if (original_filename_in_thumbnail != null && galleryid != null) {
                /* 2019-08-21: Best way to generate downloadlinks - and we do not even have to login to download them! */
                logger.info("Using thumbnail to original download (fast way)");
                dllink = String.format("https://www." + this.getHost() + "/files/mul/galleries/%s/%s", galleryid, original_filename_in_thumbnail);
            } else {
                logger.info("Using official download (slower way)");
                final boolean grabAjax = false;
                if (grabAjax) {
                    /* 2016-08-21: Set width & height to higher values so that we get the max quality possible. */
                    /* Login with validating cookies */
                    login(account, true);
                    this.br.postPage("https://www." + this.getHost() + "/Ajax/mulFileInfo", "filId=" + fid + "&width=5000&height=5000&filIdPrevious=&filIdNext=");
                    try {
                        final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                        dllink = (String) entries.get("path");
                    } catch (final Throwable e) {
                    }
                } else {
                    /*
                     * 2019-08-21: These images seem to have a slightly better quality than via ajax request (also the downloadlink
                     * extracted via ajax request will contain the String '/tumbnail/'.)
                     */
                    /* Login WITHOUT validating cookies - this speeds up the downloading process! */
                    final boolean verifiedLogin = login(account, false);
                    br.getPage("https://www." + this.getHost() + "/galerie/datei?p=" + fid);
                    if (!verifiedLogin && !isLoggedin(br)) {
                        logger.info("Login cookies did not work - trying again!");
                        login(account, true);
                        br.getPage("/galerie/datei?p=" + fid);
                    }
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        /* Offline - very very rare case! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = br.getRegex("<meta property=\"og:image\" content=\"(https?://[^\"]+)\">").getMatch(0);
                }
                if (dllink != null && !dllink.startsWith("http") && !dllink.startsWith("/")) {
                    dllink = "https://www." + this.getHost() + "/" + dllink;
                }
            }
        }
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            final String url_filename = getFilenameFromThumbnailDirecturl(dllink);
            if (url_filename != null) {
                filename = url_filename;
            } else if (filename_decrypter != null) {
                filename = filename_decrypter;
                if (!filename.endsWith(default_extension)) {
                    filename += default_extension;
                }
            } else {
                filename = fid + default_extension;
            }
            link.setFinalFileName(filename);
            URLConnectionAdapter con = null;
            try {
                con = this.br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    server_issues = true;
                }
                link.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        /* Account required to download. */
        throw new AccountRequiredException();
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public boolean login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                prepBR(br);
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    if (!validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust cookies without validation");
                        return false;
                    } else {
                        logger.info("Checking cookies...");
                        br.getPage("https://www." + this.getHost());
                        if (isLoggedin(this.br)) {
                            logger.info("Cookie login successful");
                            return true;
                        } else {
                            logger.info("Cookie login failed");
                        }
                    }
                }
                /* Perform full login */
                logger.info("Performing full login");
                br.postPage("https://www." + this.getHost() + "/login", "loginData%5BauthsysAuthProvider%5D%5BrememberLogin%5D=on&sent=true&url=%2F&usedProvider=authsysAuthProvider&loginData%5BauthsysAuthProvider%5D%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&loginData%5BauthsysAuthProvider%5D%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("Ihre Login\\-Daten sind ungültig")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
        return true;
    }

    public static boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout\"") && br.getCookie("parteeey.de", "login-token", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        handleDownload(link);
    }

    public static String getFilenameFromThumbnailDirecturl(final String directurl) {
        if (directurl == null) {
            return null;
        }
        final Regex newStyle = new Regex(directurl, "(?i).*/thumb\\.php\\?f=assets%3A%2F%2Fmul%2Fgalleries%2F\\d+%2F(\\d+_[^\\&]+).*");
        if (newStyle.matches()) {
            /* 2021-06-14: New */
            return newStyle.getMatch(0);
        } else {
            return new Regex(directurl, "(?i)/[a-z]+_\\d+_\\d+_\\d+_(.+)$").getMatch(0);
        }
    }

    public static Browser prepBR(final Browser br) {
        /* Last updated: 2021-06-14 */
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}