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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sankakucomplex.com" }, urls = { "https?://(?:www\\.)?(?:chan|idol)\\.sankakucomplex\\.com/(?:[a-z]{2}/)?post/show/(\\d+)" })
public class SankakucomplexCom extends antiDDoSForHost {
    public SankakucomplexCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://chan.sankakucomplex.com/user/signup");
    }

    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension = ".jpg";
    private String              dllink            = null;
    private boolean             accountRequired   = false;

    @Override
    public String getAGBLink() {
        return "https://www.sankakucomplex.com/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @Override
    public void init() {
        try {
            Browser.setBurstRequestIntervalLimitGlobal(this.getHost(), 3000, 20, 60000);
        } catch (Exception e) {
        }
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        br.setFollowRedirects(true);
        final String host = new URL(link.getPluginPatternMatcher()).getHost();
        br.setCookie("https://" + host, "locale", "en");
        br.setCookie("https://" + host, "hide-news-ticker", "1");
        br.setCookie("https://" + host, "auto_page", "1");
        br.setCookie("https://" + host, "hide_resized_notice", "1");
        br.setCookie("https://" + host, "blacklisted_tags", "");
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>404: Page Not Found<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*You lack the access rights required to view this content")) {
            this.accountRequired = true;
            return AvailableStatus.TRUE;
        }
        String filename = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        dllink = checkDirectLink(link, "directlink");
        if (dllink != null) {
            /* This means we must have checked this one before so filesize/name has already been set -> Done! */
            return AvailableStatus.TRUE;
        }
        if (dllink == null) {
            dllink = br.getRegex("<li>Original: <a href=\"(//[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<a href=\"(//[^<>\"]*?)\">Save this file").getMatch(0);
            }
            if (dllink == null) {
                /* 2021-02-23 */
                dllink = br.getRegex("<meta content=\"(//[^<>\"]+)\" property=og:image>").getMatch(0);
            }
            if (dllink != null) {
                dllink = "https:" + dllink;
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = null;
        if (dllink != null) {
            ext = new Regex(dllink, "[a-z0-9]+(\\.[a-z]+)(\\?|$)").getMatch(0);
        }
        if (ext == null) {
            ext = getFileNameExtensionFromString(dllink, default_Extension);
        }
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (Encoding.isHtmlEntityCoded(this.dllink)) {
            dllink = Encoding.htmlDecode(dllink);
        }
        final String size = br.getRegex("<li>Original:\\s*<a href.*?title=\"([0-9\\,]+) bytes").getMatch(0);
        if (size != null) {
            /* Size is given --> We don't have to check for it! */
            link.setDownloadSize(Long.parseLong(size.replace(",", "")));
            return AvailableStatus.TRUE;
        }
        if (dllink != null && !isDownload) {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    link.setProperty("directlink", dllink);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        requestFileInformation(link, true);
        if (this.accountRequired) {
            throw new AccountRequiredException();
        } else if (this.dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Disable chunks as we only download small files */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("text")) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                try {
                    if (this.looksLikeDownloadableContent(con) && !con.getURL().toString().contains("expired.png")) {
                        return dllink;
                    } else {
                        return null;
                    }
                } finally {
                    con.disconnect();
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
            }
        }
        return null;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    br.getPage("https://chan." + this.getHost() + "/user/home");
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://chan." + this.getHost() + "/user/login");
                final Form loginform = br.getFormbyActionRegex(".*user/authenticate");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("user%5Bname%5D", Encoding.urlEncode(account.getUser()));
                loginform.put("user%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.getCookie(this.getHost(), "pass_hash", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        if (br.containsHTML(">\\s*Subscription Level\\s*:\\s*<a href=\"[^\"]+\">Plus<")) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link, true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
