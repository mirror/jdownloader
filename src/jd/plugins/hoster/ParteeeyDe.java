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

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "parteeey.de" }, urls = { "https?://(?:www\\.)?parteeey\\.de/(?:.*#mulFile\\-|galerie/datei\\?p=)(\\d+)" })
public class ParteeeyDe extends PluginForHost {
    public ParteeeyDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.parteeey.de/registrierung");
    }

    @Override
    public String getAGBLink() {
        return "https://www.parteeey.de/nutzungsbedingungen";
    }

    public static final String default_extension          = ".jpg";
    private final String       PROPERTY_DIRECTURL         = "directurl";
    public static final String PROPERTY_DECRYPTERFILENAME = "decrypterfilename";
    public static final String PROPERTY_THUMBURL          = "thumburl";
    public static final String PROPERTY_GALLERYID         = "galleryid";

    private String getContentURL(final DownloadLink link) {
        return "https://www." + this.getHost() + "/galerie/datei?p=" + getFID(link);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fid = getFID(link);
        this.setBrowserExclusive();
        prepBR(this.br);
        if (account == null) {
            link.getLinkStatus().setStatusText("Account needed for linkcheck- and download");
            return AvailableStatus.UNCHECKABLE;
        }
        String urlThumb = link.getStringProperty(ParteeeyDe.PROPERTY_THUMBURL);
        final String filename_from_crawler = link.getStringProperty(ParteeeyDe.PROPERTY_DECRYPTERFILENAME);
        final String galleryid = link.getStringProperty(ParteeeyDe.PROPERTY_GALLERYID);
        /**
         * 2017-10-06: Official downloads are disabled (that time this required special account permissions.) </br>
         * 2023-04-25: Left this disabled: Official downloads are slower and they do not provide the content in higher quality so there is
         * no point in using them.
         */
        final boolean tryOfficialDownload = false;
        String dllink = null;
        if (tryOfficialDownload) {
            /*
             * 2016-08-21: Prefer download via official download function as this gives us the highest quality possible - usually a bit(some
             * KBs) better than the image displayed in the gallery via browser.
             */
            /* Login with validating cookies */
            login(account, getMainpage(), true);
            dllink = getMainpage() + "/galerie/datei/herunterladen/" + fid;
        } else {
            final String original_filename_in_thumbnail = getFilenameFromThumbnailDirecturl(urlThumb);
            if (original_filename_in_thumbnail != null && galleryid != null) {
                /* 2019-08-21: Best way to generate downloadlinks - and we do not even have to login to download them! */
                logger.info("Using thumbnail to original download (fast way)");
                dllink = String.format(getMainpage() + "/files/mul/galleries/%s/%s", galleryid, original_filename_in_thumbnail);
            } else {
                logger.info("Using official download (slower way)");
                final boolean grabAjax = false;
                if (grabAjax) {
                    /* 2016-08-21: Set width & height to higher values so that we get the max quality possible. */
                    /* Login with validating cookies */
                    login(account, getMainpage(), true);
                    this.br.postPage(getMainpage() + "/Ajax/mulFileInfo", "filId=" + fid + "&width=5000&height=5000&filIdPrevious=&filIdNext=");
                    try {
                        final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                        dllink = (String) entries.get("path");
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.warning("Exception happened during ajax handling");
                    }
                } else {
                    /*
                     * 2019-08-21: These images seem to have a slightly better quality than via ajax request (also the downloadlink
                     * extracted via ajax request will contain the String '/tumbnail/'.)
                     */
                    /* Login WITHOUT validating cookies - this speeds up the downloading process! */
                    final String viewImageURL = getMainpage() + "/galerie/datei?p=" + fid;
                    login(account, viewImageURL, true);
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        /* Offline - very very rare case! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = br.getRegex("<meta property=\"og:image\" content=\"(https?://[^\"]+)\">").getMatch(0);
                }
                if (dllink != null && !dllink.startsWith("http") && !dllink.startsWith("/")) {
                    /* Create full URL out of relative URL. */
                    dllink = getMainpage() + "/" + dllink;
                }
            }
        }
        link.setProperty(PROPERTY_DIRECTURL, dllink);
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            final String url_filename = getFilenameFromThumbnailDirecturl(dllink);
            String filename;
            if (url_filename != null) {
                filename = url_filename;
            } else if (filename_from_crawler != null) {
                filename = filename_from_crawler;
                filename = this.applyFilenameExtension(filename, default_extension);
            } else {
                filename = fid + default_extension;
            }
            link.setFinalFileName(filename);
            URLConnectionAdapter con = null;
            try {
                con = this.br.openHeadConnection(dllink);
                this.downloadErrorhandling(con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else if (filename_from_crawler != null) {
            link.setFinalFileName(filename_from_crawler);
        }
        return AvailableStatus.TRUE;
    }

    public String getMainpage() {
        return "https://www." + this.getHost();
    }

    private void downloadErrorhandling(final URLConnectionAdapter con) throws IOException, PluginException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        /* Account required to download. */
        throw new AccountRequiredException();
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Connection stuff - disable resume & chunks to keep serverload low. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        this.downloadErrorhandling(dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void login(final Account account, final String checkURL, final boolean validateCookies) throws Exception {
        synchronized (account) {
            prepBR(br);
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(account.getHoster(), cookies);
                if (!validateCookies) {
                    /* We trust these cookies as they're not that old --> Do not check them */
                    logger.info("Trust cookies without validation");
                    return;
                } else {
                    logger.info("Checking cookies...");
                    br.getPage(checkURL);
                    if (isLoggedin(this.br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                        account.clearCookies("");
                    }
                }
            }
            /* Perform full login */
            logger.info("Performing full login");
            br.getPage(getMainpage() + "/login");
            br.postPage(br.getURL(), "loginData%5BauthsysAuthProvider%5D%5BrememberLogin%5D=on&sent=true&url=%2F&usedProvider=authsysAuthProvider&loginData%5BauthsysAuthProvider%5D%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&loginData%5BauthsysAuthProvider%5D%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()));
            if (!isLoggedin(br)) {
                throw new AccountInvalidException();
            }
            final String pathOfCheckURL = new URL(checkURL).getPath();
            if (!br._getURL().getPath().equals(pathOfCheckURL)) {
                logger.info("Accessing checkURL again after successful full login: " + checkURL);
                br.getPage(pathOfCheckURL);
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("/logout\"") && br.getCookie("parteeey.de", "login-token", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, this.getMainpage(), true);
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