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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

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
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xun-niu.com" }, urls = { "https?://(?:www\\.)?xun-niu\\.com/(?:file|down|down2)\\-([a-z0-9]+)\\.html" })
public class XunNiuCom extends PluginForHost {
    public XunNiuCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www." + this.getHost() + "/vip.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www." + this.getHost() + "/about.php?action=help";
    }

    /* Connection stuff */
    /* 2019-09-12: Failed to test any free download as it seems like all files they host are PREMIUMONLY! */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    /* 2019-09-12: Successfully tested 2 chunks but this may lead to disconnects! */
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 1;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = this.getFID(link);
        br.getPage("http://www." + this.getHost() + "/file-" + fid + ".html");
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">文件资源若被删除，可能的原因有|内容涉及不良信息。")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<div class=\"span7\">\\s*<h1>([^<>\"]+)</h1>").getMatch(0);
        String filesize = br.getRegex(">文件大小：([^<>\"]+)<").getMatch(0);
        if (filename != null) {
            /* Set final filename here because server filenames are bad. */
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        } else {
            link.setName(this.getFID(link));
        }
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
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
            final boolean skipWaittime = true;
            final boolean skipCaptcha = true;
            if (!skipWaittime) {
                /* 2019-09-12: Defaultvalue = 50 */
                int wait = 50;
                final String waittime = br.getRegex("var\\s*secs\\s*=\\s*(\\d+);").getMatch(0);
                if (waittime != null) {
                    wait = Integer.parseInt(waittime);
                }
                if (wait > 180) {
                    /* High waittime --> Reconnect is faster than waiting :) */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                }
                this.sleep(wait * 1001l, downloadLink);
            }
            /*
             * 2019-09-12: General procedure: /down2 --> /down --> Captcha --> Download(?) --> Failed to start a single free download via
             * browser until now!
             */
            if (br.containsHTML("/down2-" + fid)) {
                br.getPage("/down2-" + fid + ".html");
            }
            if (br.containsHTML("/down-" + fid)) {
                br.getPage("/down-" + fid + ".html");
            }
            String action = br.getRegex("url\\s*:\\s*'([^\\']+)'").getMatch(0);
            if (action == null) {
                action = "ajax.php";
            }
            if (!action.startsWith("/")) {
                action = "/" + action;
            }
            final Browser ajax = this.br.cloneBrowser();
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            boolean failed = true;
            int counter = 0;
            if ((br.containsHTML("imagecode\\.php") || true) && !skipCaptcha) {
                do {
                    try {
                        final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), downloadLink);
                        ajax.postPage(action, "action=check_code&code=" + Encoding.urlEncode(code));
                        if (ajax.toString().equals("false")) {
                            continue;
                        }
                        failed = false;
                        break;
                    } finally {
                        counter++;
                    }
                } while (failed && counter <= 10);
                if (failed) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                // if (down2_url != null) {
                // this.br.getHeaders().put("Referer", down2_url);
                // }
                // /* If we don't wait for some seconds here, the continue_url will redirect us to the main url!! */
                // this.sleep(5 * 1001l, downloadLink);
            }
            ajax.postPage(action, "action=load_down_addr1&file_id=" + fid);
            // final String dlarg = br.getRegex("url\\s*:\\s*\\'[^\\']*\\',\\s*data\\s*:\\s*\\'action=(pc_\\d+)").getMatch(0);
            // if (dlarg != null) {
            // ajax.postPage(action, "action=" + dlarg + "&file_id=" + fid + "&ms=" + System.currentTimeMillis() + "&sc=640*480");
            // }
            /* TODO: Improve errorhandling */
            dllink = ajax.getRegex("true\\|<a href=\"([^<>\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = ajax.getRegex("true\\|(http[^<>\"]+)").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.startsWith("vip.php")) {
                /* 2019-09-12: They might even display 4-5 mirrors here but none of them is for freeusers! */
                throw new AccountRequiredException();
            }
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
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    /**
     * @param validateCookies
     *            true = check cookies for validity, perform full login if necessary </br>
     *            false = Just set cookies and return false if cookies are younger than 300000l
     *
     * @return true = Cookies are validated </br>
     *         false = Cookies are not validated (only set on current Browser instance)
     */
    private boolean login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean validatedCookies = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust cookies without checking as they're still fresh");
                        return false;
                    }
                    br.getPage("http://www." + account.getHoster() + "/mydisk.php");
                    if (isLoggedIn()) {
                        validatedCookies = true;
                    } else {
                        this.br = new Browser();
                    }
                }
                /*
                 * 2019-09-12: Every full login will invalidate al older sessions (user will have to re-login via browser)! If users
                 * complain about too many login captchas, tell them to only login via browser OR JDownloader to avoid this!
                 */
                if (!validatedCookies) {
                    br.getPage("http://www." + account.getHoster() + "/account.php?action=login");
                    final Form loginform = br.getFormbyProperty("name", "login_form");
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("username", account.getUser());
                    loginform.put("password", account.getPass());
                    final String captchaFieldKey = "verycode";
                    if (loginform.hasInputFieldByName(captchaFieldKey)) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(), true);
                        final String code = getCaptchaCode("/includes/imgcode.inc.php?verycode_type=2&t=0." + System.currentTimeMillis(), dummyLink);
                        loginform.put(captchaFieldKey, code);
                    }
                    loginform.put("remember", "1");
                    br.submitForm(loginform);
                    if (!isLoggedIn()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    validatedCookies = true;
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return validatedCookies;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIn() {
        return br.getCookie(br.getHost(), "phpdisk_zcore_v2_info", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            throw e;
        }
        if (br.getURL() == null || !br.getURL().contains("/mydisk.php")) {
            br.getPage("/mydisk.php");
        }
        long expire = 0;
        String expireStr = br.getRegex(">VIP结束时间</b>：<span class=\"txt_r\">(\\d{4}-\\d{2}-\\d{2})</span>").getMatch(0);
        if (expireStr != null) {
            expire = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd", Locale.CHINA);
        }
        ai.setUnlimitedTraffic();
        if (expire < System.currentTimeMillis()) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            ai.setValidUntil(expire);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, true);
        if (account.getType() == AccountType.FREE) {
            br.getPage(link.getPluginPatternMatcher());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                /* 2019-06-27: TODO: Add premium support */
                br.setFollowRedirects(true);
                br.getPage(link.getPluginPatternMatcher());
                br.postPage("/ajax.php", "action=get_vip_fl&file_id=" + this.getFID(link));
                final String urls_text = br.getRegex("true\\|(http.+)").getMatch(0);
                if (urls_text != null) {
                    final String[] mirrors = urls_text.split("\\|");
                    if (mirrors.length > 0) {
                        /* Choose first mirror hardcoded */
                        dllink = mirrors[0];
                    }
                }
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                /*
                 * 2019-09-12 E.g. error "<p>请登录原地址重新获取： <a href="http://www.xun-niu.com/viewfile.php?file_id=" target="
                 * _blank">http://www.xun-niu.com/viewfile.php?file_id=<a></p><p style="color:#ff0000">温馨提示：此文件链接已失效，请勿非法盗链, err2。</p>"
                 */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
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