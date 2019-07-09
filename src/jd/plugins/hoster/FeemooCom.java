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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "feemoo.com" }, urls = { "https?://(?:www\\.)?feemoo\\.com/file\\-([a-z0-9]+)\\.html" })
public class FeemooCom extends PluginForHost {
    public FeemooCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.feemoo.com/upgrade.html");
    }

    @Override
    public String getAGBLink() {
        return "https://www.feemoo.com/terms.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = false;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 1;
    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("文件不存在或已删除") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"sfilename\">([^<>\"]+)<").getMatch(0);
        String filesize = br.getRegex("<span>文件大小：</span><font>([^<>\"]+)<").getMatch(0);
        if (filename != null) {
            /* Set final filename here because server filenames are bad. */
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        } else {
            link.setName(this.getLinkID(link));
        }
        if (filesize != null) {
            filesize += "b";
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String fid = getFID(downloadLink);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            String down2_url = null;
            if (br.containsHTML("/down2-" + fid)) {
                br.getPage("/down2-" + fid + ".html");
                down2_url = this.br.getURL();
            }
            final Browser ajax = this.br.cloneBrowser();
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String continue_url;
            if (br.containsHTML("yythems_ajax_file")) {
                ajax.postPage("/yythems_ajax_file.php", "action=load_down_addr2&file_id=" + fid);
                continue_url = ajax.getRegex("(fmdown\\.php[^<>\"\\']+)").getMatch(0);
            } else {
                continue_url = br.getRegex("(fmdown\\.php[^<>\"\\']+)").getMatch(0);
            }
            if (continue_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (br.containsHTML("imagecode\\.php")) {
                /* 2019-06-27: TODO: Improve this captcha-check! */
                final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), downloadLink);
                ajax.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
                if (ajax.toString().equals("false")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (down2_url != null) {
                    this.br.getHeaders().put("Referer", down2_url);
                }
                /* If we don't wait for some seconds here, the continue_url will redirect us to the main url!! */
                this.sleep(5 * 1001l, downloadLink);
            }
            br.getPage(continue_url);
            // final String dlarg = br.getRegex("url : \\'ajax\\.php\\',\\s*?data\\s*?:\\s*?\\'action=(pc_\\d+)").getMatch(0);
            // if (dlarg != null) {
            // ajax.postPage("/ajax.php", "action=" + dlarg + "&file_id=" + fid + "&ms=" + System.currentTimeMillis() + "&sc=640*480");
            // }
            /* After the fmdown.php */
            if (br.containsHTML(">该文件暂无普通下载点，请使用SVIP")) {
                throw new AccountRequiredException();
            } else if (this.br.containsHTML(">非VIP用户每次下载间隔为")) {
                /* Usually 10 minute wait --> Let's reconnect! */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            dllink = br.getRegex("var\\s*?file_url\\s*?=\\s*?\\'(http[^<>\"\\']+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https?://[^/]+/dl\\.php[^<>\"\\']+)").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            int wait = 10;
            final String waittime = br.getRegex("var\\s*?t\\s*?=\\s*?(\\d+);").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            this.sleep(wait * 1001l, downloadLink);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)\\.html$").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("deprecation")
    private void login(final Account account) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://www." + account.getHoster() + "/home.php");
                    if (isLoggedIn(this.br)) {
                        /* Save cookie timestamp. */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    }
                    this.br = new Browser();
                }
                br.setFollowRedirects(false);
                br.getPage("https://www." + account.getHoster() + "/home.php");
                br.postPage("/home.php", "action=login&task=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=1");
                // if (this.br.containsHTML("yzm\\.php")) {
                // final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(),
                // true);
                // final String code = getCaptchaCode("/yzm.php", dummyLink);
                // postData += "&yzm=" + Encoding.urlEncode(code);
                // }
                final String status = PluginJSonUtils.getJson(br, "status");
                if (!"true".equalsIgnoreCase(status) && !"1".equalsIgnoreCase(status)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIn(final Browser br) {
        return br.containsHTML("logoutbtn\\(\\)");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/home.php?action=up_svip&m=");
        /* 2019-06-27: TODO: Check/fix this */
        long expire = 0;
        String expireStr = br.getRegex(">到期时间：</span><span class=\\\\mr15 w300 dib\">([^<>\"]*?)</span>").getMatch(0);
        if (expireStr == null) {
            expireStr = br.getRegex("<span>VIP到期时间：(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})</span>").getMatch(0);
        }
        if (expireStr != null) {
            if (expireStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                /* 2019-06-27: New */
                expire = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            } else {
                expire = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd", Locale.CHINA);
            }
        }
        ai.setUnlimitedTraffic();
        if (br.containsHTML("href=\"upvip\\.php\" title=\"升级为VIP会员\"") || expire < System.currentTimeMillis()) {
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            ai.setValidUntil(expire);
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        if (account.getType() == AccountType.FREE) {
            br.getPage(link.getDownloadURL());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                /* 2019-06-27: TODO: Add premium support */
                br.setFollowRedirects(true);
                br.postPage("/yythems_ajax.php", "action=load_down_addr_svip&file_id=" + this.getLinkID(link));
                final String status = PluginJSonUtils.getJson(br, "status");
                final String errormessage = PluginJSonUtils.getJson(br, "str");
                if (!"true".equalsIgnoreCase(status)) {
                    if (!StringUtils.isEmpty(errormessage)) {
                        if (errormessage.equalsIgnoreCase("请升级SVIP会员后再使用SVIP极速下载通道！")) {
                            /* Account is a free account, file is only downloadable for premium users! */
                            throw new AccountRequiredException();
                        }
                    }
                }
                br.getPage(link.getDownloadURL());
                dllink = br.getRegex("TODO_FIXME").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_ChineseFileHosting;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}