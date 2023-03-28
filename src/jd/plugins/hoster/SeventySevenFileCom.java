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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SeventySevenFileCom extends PluginForHost {
    public SeventySevenFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://77file.com/vip.php");
    }

    @Override
    public String getAGBLink() {
        return "https://www.77file.com/terms.php";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "77file.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:s|down)/[A-Za-z0-9]+|(?:file|down)/[^/]+\\.html)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                  = true;
    private final int            FREE_MAXCHUNKS               = 1;
    private final int            FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    /* 2022-05-20: Premium limits untested */
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("/down/", "/s/"));
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), ".+/([^/]+)(\\.html)?$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        /* 2023-03-24: Added check for redirect */
        final String redirect = br.getRegex(">window\\.location\\.href='(/[^<>\"\\']+)'").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("<span id=\"file_size\"></span>")) {
            /* Empty / Missing filesize */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*此文件已被用户删除，暂时无法访问")) {
            /* 2021-12-07 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String extFileID = new Regex(br.getURL(), "/s/(.+)").getMatch(0);
        if (extFileID != null) {
            link.setLinkID(this.getHost() + "://" + extFileID);
        }
        String filename = br.getRegex("align='absbottom' border='0'[^/>]*/>([^<>\"]+)<").getMatch(0);
        String filesize = br.getRegex("<span id=\"file_size\">([^<>\"]+)</span>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            filesize += "b";
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleFreeDownload(link, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleFreeDownload(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        // final String fid = getFID(link);
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resumable, maxchunks)) {
            if (account != null) {
                this.login(account, false);
            }
            requestFileInformation(link);
            final String fileID2 = this.br.getRegex("file_id=(\\d+)").getMatch(0);
            if (fileID2 == null) {
                logger.warning("Failed to find fileID2");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser brAjax = this.br.cloneBrowser();
            brAjax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brAjax.getPage("/ajax_new.php??a=1&ctime=" + System.currentTimeMillis());
            /* 2020-04-09: Waittime is skippable */
            final String longWaittimeStr = PluginJSonUtils.getJson(brAjax, "wtime");
            if (longWaittimeStr != null && longWaittimeStr.matches("\\d+")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(longWaittimeStr) * 1001l);
            }
            /* 2020-04-09: Waittime is skippable */
            // final String waittimeStr = PluginJSonUtils.getJson(brAjax, "waittime");
            // if (waittimeStr == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // final int wait = Integer.parseInt(waittimeStr);
            // /* Too high waittime --> Reconnect required */
            // if (wait > 75) {
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
            // }
            // this.sleep(wait * 1001l, link);
            /* 2020-04-09: '/down' page is skippable */
            // br.getPage(br.getURL().replace("/file/", "/down/"));
            /* 2020-04-09: Captcha is skippable */
            // final String code = getCaptchaCode("/imagecode.php", link);
            // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // br.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
            // if (br.toString().equals("false")) {
            // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // }
            br.postPage("/ajax.php", "action=load_down_addr1&file_id=" + fileID2);
            String dllink = br.getRegex("true\\|<a href=\"([^<>\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://down\\.[^<>\"]+)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            /* 2023-03-24: They will e.g. send "Content-Type: text/plain" for text-files and won't provide a content-disposition header. */
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dllink);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(directlinkproperty);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                /*
                 * 2021-11-09: Warning: They only allow one active session per account so user logging in via browser may end JDs session!
                 */
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not verify cookies */
                        return false;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/account.php?action=login");
                final Form loginform = br.getFormbyProperty("name", "user_form");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                br.getPage("/mydisk.php?item=profile&menu=cp");
                if (!isLoggedin(br)) {
                    throw new AccountInvalidException();
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

    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("action=logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!br.getURL().contains("/mydisk.php?item=profile&menu=cp")) {
            br.getPage("/mydisk.php?item=profile&menu=cp");
        }
        ai.setUnlimitedTraffic();
        final String premiumExpire = br.getRegex("(?i)VIP结束时间\\s*：</td>\\s*<td>\\s*(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        if (premiumExpire == null) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(premiumExpire, "yyyy-MM-dd", Locale.ENGLISH), br);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.PREMIUM) {
            // this.handleFreeDownload(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "premium_directlink");
            final String premiumDirectlinkproperty = "premium_directlink";
            // final String fid = getFID(link);
            if (!attemptStoredDownloadurlDownload(link, premiumDirectlinkproperty, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS)) {
                this.login(account, false);
                requestFileInformation(link);
                /* Here comes unfinished code because we do not support their used captcha called "TencentCaptcha" yet. */
                if (br.containsHTML("getverify\\.php")) {
                    /* 2022-05-31: Unsupported captcha in premium mode */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String dllink = br.getRegex("true\\|<a href=\"([^<>\"]+)").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(https?://down\\.[^<>\"]+)\"").getMatch(0);
                }
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperty(premiumDirectlinkproperty, dllink);
            }
            dl.startDownload();
        } else {
            this.handleFreeDownload(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        }
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        if (!urlConnection.getContentType().contains("html")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc != null && acc.getType() == AccountType.PREMIUM) {
            /* Premium download = captcha (really strange but that's how it is lol) */
            return true;
        } else {
            /* Anonymous download and free account download = no captcha */
            return false;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PhpDisk;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}