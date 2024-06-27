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

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imgs.aventertainments.com", "aventertainments.com" }, urls = { "https?://imgs\\d+\\.aventertainments\\.com/.+", "https?://www\\.aventertainments\\.com/newdlsample\\.aspx.+\\.mp4|https?://ppvclips\\d+\\.aventertainments\\.com/.+\\.m3u9|https?://(?:www\\.)?aventertainments\\.com/ppv/new_detail\\.aspx\\?ProID=\\d+.*|https?://(?:www\\.)?aventertainments\\.com/ppv/Download\\.aspx\\?.+" })
public class AventertainmentsCom extends PluginForHost {
    public AventertainmentsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.aventertainments.com/register.aspx?languageID=1&VODTypeID=1&Site=PPV");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        prepBR(br);
        return br;
    }

    public static Browser prepBR(final Browser br) {
        br.setCookie("aventertainments.com", "IPCountry", "EN");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    /* TODO: 2020-10-21: Check if this linktype still exists */
    private final String        TYPE_IMAGE        = "(?i)https?://imgs\\.aventertainments\\.com/.+";
    /* TODO: 2020-10-21: Check if this linktype still exists */
    private final String        TYPE_VIDEO_HTTP   = "(?i)https?://(?:www\\.)?aventertainments\\.com/newdlsample\\.aspx.*?\\.mp4";
    /* TODO: 2020-10-21: Check if this linktype still exists */
    private final String        TYPE_VIDEO_HLS    = "(?i)https?://ppvclips\\d+\\.aventertainments\\.com/.+\\.m3u8";
    /* 2022-01-04: New: User paid content */
    private static final String TYPE_VIDEO_DIRECT = "(?i)https?://[^/]+/ppv/Download\\.aspx\\?.+";
    /*
     * 2021-07-16: Important: Allow more parameters after "ProID" as URLs can also contain the user preferred language via parameter
     * "languageID".
     */
    private final String        TYPE_NEW_2020     = "(?i)https?://(?:www\\.)?aventertainments\\.com/ppv/new_detail\\.aspx\\?ProID=\\d+.*?";
    public static final String  html_loggedin     = "aventertainments.com/logout\\.aspx";
    /* Connection stuff */
    private final boolean       free_resume       = true;
    private final int           free_maxchunks    = 0;
    private final int           free_maxdownloads = -1;
    private String              dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.aventertainments.com/aveterms.htm";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String newurl = link.getPluginPatternMatcher().replace(".m3u9", ".m3u8");
        link.setPluginPatternMatcher(newurl);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String mainlink = link.getStringProperty("mainlink");
        dllink = null;
        this.setBrowserExclusive();
        if (account != null) {
            login(account, false);
        }
        br.setFollowRedirects(true);
        String urlTitle = new Regex(link.getPluginPatternMatcher(), "/([^/]+)$").getMatch(0);
        String finalFilename = link.getFinalFileName();
        if (link.getPluginPatternMatcher().matches(TYPE_NEW_2020)) {
            /*
             * 2021-07-21: Not used anymore as these are now also processed by our crawler which will add found videos as TYPE_VIDEO_HTTP
             * and/or TYPE_VIDEO_HLS.
             */
            urlTitle = UrlQuery.parse(link.getPluginPatternMatcher().toLowerCase()).get("proid");
            /* Set fallback-filename */
            if (!link.isNameSet()) {
                link.setName(urlTitle + ".mp4");
            }
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(urlTitle)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            finalFilename = br.getRegex("data-cast-title=\"([^\"]+)\"").getMatch(0);
            this.dllink = br.getRegex("source src=\"(https?://[^\"]+)\" type=\"application/x-mpegurl\" />").getMatch(0);
            if (finalFilename != null) {
                link.setFinalFileName(Encoding.htmlDecode(finalFilename).trim() + ".mp4");
            }
        } else if (link.getPluginPatternMatcher().matches(TYPE_VIDEO_HLS)) {
            dllink = link.getPluginPatternMatcher();
        } else {
            if (link.getPluginPatternMatcher().matches(TYPE_VIDEO_HTTP)) {
                br.setFollowRedirects(false);
                if (mainlink != null) {
                    /* Important!! */
                    this.br.getHeaders().put("Referer", mainlink);
                }
                this.br.getPage(link.getPluginPatternMatcher());
                dllink = this.br.getRedirectLocation();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                br.setFollowRedirects(true);
            } else if (link.getPluginPatternMatcher().matches(TYPE_IMAGE)) {
                /* Picture directURL */
                dllink = link.getPluginPatternMatcher();
                if (finalFilename != null) {
                    finalFilename = Encoding.htmlDecode(finalFilename);
                    finalFilename = finalFilename.trim();
                    final String ext = getFileNameExtensionFromString(dllink, ".jpg");
                    finalFilename = applyFilenameExtension(finalFilename, ext);
                    link.setFinalFileName(finalFilename);
                }
            } else {
                /* Video directurl */
                dllink = link.getPluginPatternMatcher();
            }
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    /* Assume directurl is offline */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String serverFilename = Plugin.getFileNameFromDispositionHeader(con);
                if (serverFilename != null) {
                    if (finalFilename == null && serverFilename != null) {
                        link.setFinalFileName(finalFilename);
                    }
                } else {
                    /* 2022-01-04: Filename is not (always) given via header... */
                    final String videoFallbackFilename = new Regex(con.getURL().toExternalForm(), "(?i)/([^/]+\\.mp4)").getMatch(0);
                    if (finalFilename == null && link.getPluginPatternMatcher().matches(TYPE_VIDEO_DIRECT) && videoFallbackFilename != null) {
                        link.setFinalFileName(videoFallbackFilename);
                    } else if (link.getName() != null) {
                        link.setFinalFileName(this.correctOrApplyFileNameExtension(link.getName(), con));
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.matches(TYPE_VIDEO_HLS)) {
            br.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    if (!force) {
                        /* Trust login cookies without check */
                        return;
                    } else {
                        br.getPage("https://www.aventertainments.com/main.aspx?languageID=1");
                        if (br.containsHTML(html_loggedin)) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(account.getHoster()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearAll();
                        }
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www.aventertainments.com/login.aspx?languageID=1&VODTypeID=1&Site=PPV");
                final Form loginform = br.getFormbyKey("__EVENTTARGET");
                loginform.put("ctl00$ContentPlaceHolder1$uid", Encoding.urlEncode(account.getUser()));
                loginform.put("ctl00$ContentPlaceHolder1$passwd", Encoding.urlEncode(account.getPass()));
                loginform.put("ctl00$ContentPlaceHolder1$SavedLoginBox", "on");
                loginform.put("__EVENTTARGET", Encoding.urlEncode("ctl00$ContentPlaceHolder1$SubmitBtn"));
                loginform.put("__EVENTARGUMENT", "");
                final String VIEWSTATEGENERATOR = br.getRegex("VIEWSTATEGENERATOR\"\\s*value\\s*=\\s*\"(.*?)\"").getMatch(0);
                final String EVENTVALIDATION = br.getRegex("EVENTVALIDATION\"\\s*value\\s*=\\s*\"(.*?)\"").getMatch(0);
                if (VIEWSTATEGENERATOR != null) {
                    loginform.put("__VIEWSTATEGENERATOR", Encoding.urlEncode(VIEWSTATEGENERATOR));
                }
                if (EVENTVALIDATION != null) {
                    loginform.put("__EVENTVALIDATION", Encoding.urlEncode(EVENTVALIDATION));
                }
                br.submitForm(loginform);
                if (!br.containsHTML(html_loggedin)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        /* 2022-01-04: There are no different account-types (?) Users can buy single video-items and then stream or download those. */
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(free_maxdownloads);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
