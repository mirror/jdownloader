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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UploadhavenCom extends PluginForHost {
    public UploadhavenCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/account/register");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USERNAME_IS_EMAIL };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "uploadhaven.com" });
        return ret;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/download/([a-f0-9]{32})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/account/register";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        if (account != null) {
            this.login(account, false);
        }
        /**
         * 2021-01-19: Some URLs are restricted to one particular referer-website (mainpage does the job)
         */
        final String referer;
        if (!StringUtils.isEmpty(link.getDownloadPassword())) {
            /* Prefer "Download password" --> User defined Referer value */
            referer = link.getDownloadPassword();
        } else if (link.getReferrerUrl() != null) {
            referer = link.getReferrerUrl();
        } else if (link.getContainerUrl() != null && !this.canHandle(link.getContainerUrl())) {
            referer = link.getContainerUrl();
        } else {
            referer = null;
        }
        if (!StringUtils.isEmpty(referer)) {
            br.getHeaders().put("Referer", referer);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (this.isRefererProtected(br)) {
            /* We can't obtain more file information in this state! */
            if (StringUtils.isEmpty(link.getComment())) {
                link.setComment("This link is referer-protected. Enter the correct referer into the 'Download password' field to be able to download this item.");
            }
            return AvailableStatus.TRUE;
        } else if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(this.getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("(?i)File\\s*:\\s*([^<>\"]+)<br>").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("(?i)>\\s*Download file \\- ([^<>\"]+)\\s*?<").getMatch(0);
        }
        String filesize = br.getRegex("(?i)Size\\s*:\\s+(.*?) +\\s+").getMatch(0);
        if (!StringUtils.isEmpty(filename)) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String directlinkproperty;
        if (account != null) {
            directlinkproperty = "directlink_account_" + account.getType();
        } else {
            directlinkproperty = "directlink_free";
        }
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            requestFileInformation(link, account);
            if (this.isRefererProtected(br)) {
                /**
                 * 2021-01-19: WTF seems like using different referers multiple times in a row, they will always allow the 3rd string as
                 * valid Referer no matter what is used as a string?! E.g. "dfrghe".
                 */
                boolean success = false;
                String userReferer = null;
                for (int i = 0; i <= 2; i++) {
                    try {
                        userReferer = getUserInput("Enter referer?", link);
                    } catch (final PluginException abortException) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Enter referer as password to be able to download this item");
                    }
                    try {
                        new URL(userReferer);
                    } catch (final MalformedURLException e) {
                        logger.info("Entered string is not a valid URL!");
                        continue;
                    }
                    link.setDownloadPassword(userReferer);
                    requestFileInformation(link, account);
                    if (!this.isRefererProtected(br)) {
                        success = true;
                        break;
                    } else {
                        /* Try again */
                        br.clearCookies(null);
                    }
                }
                if (!success) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong referer entered");
                }
                logger.info("Valid referer == " + userReferer);
                /* Is already set in the above loop! */
                // link.setDownloadPassword(userReferer);
                if (StringUtils.isEmpty(link.getComment())) {
                    link.setComment("DownloadPassword == Referer");
                }
            }
            Form dlform = br.getFormbyProperty("id", "form-join");
            if (dlform == null) {
                /* 2024-07-22: Same for free- and premium users */
                dlform = br.getFormbyProperty("id", "form-download");
            }
            if (dlform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final boolean isPremium = account != null && AccountType.PREMIUM.equals(account.getType());
            if (!isPremium) {
                /* Non-premium -> Wait before download. */
                int wait = 5;
                String waitStr = br.getRegex("var\\s*?seconds\\s*?=\\s*?(\\d+)\\s*;").getMatch(0);
                if (waitStr == null) {
                    waitStr = br.getRegex("class\\s*=\\s*\"download-timer-seconds.*?\"\\s*>\\s*(\\d+)").getMatch(0);
                }
                if (waitStr != null) {
                    wait = Integer.parseInt(waitStr);
                }
                this.sleep((wait + 3) * 1001l, link);
            }
            br.submitForm(dlform);
            dllink = br.getRegex("downloadFile\"\\)\\.attr\\(\"src\",\\s*?\"(https?[^\">]+)").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("(https?://[A-Za-z0-9\\-]+\\.uploadhaven\\.com/[^\"]+key=[^\"]+)").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    /**
     * @return: true: Special Referer is required to download the file behind this URL </br>
     *          2024-07-22: Premium users will not run into this error.
     */
    private boolean isRefererProtected(final Browser br) {
        if (br.containsHTML(">\\s*Hotlink protection active")) {
            return true;
        } else {
            return false;
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Don't validate cookies */
                    return false;
                }
                br.getPage("https://" + this.getHost() + "/");
                if (this.isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/account/login");
            final Form loginform = br.getFormbyActionRegex(".*/account/login");
            if (loginform == null) {
                logger.warning("Failed to find loginform");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("email", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginform);
            if (!isLoggedin(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final String registerDate = br.getRegex("Registered\\s*:\\s*(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (registerDate != null) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(registerDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        } else {
            logger.warning("Failed to find register date");
        }
        /* Find premium status */
        br.getPage("/account/subscription");
        final String premiumValidUntil = br.getRegex("Ends on (\\d{2}/\\d{2}/\\d{4})").getMatch(0);
        if (premiumValidUntil != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(premiumValidUntil, "MM/dd/yyyy", Locale.ENGLISH), br);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultanFreeDownloadNum());
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}