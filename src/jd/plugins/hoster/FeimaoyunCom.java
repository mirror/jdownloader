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
import java.util.LinkedHashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "feimaoyun.com" }, urls = { "https?://(?:www\\.)?feemoo\\.com/(file\\-\\d+\\.html|#/s/\\d+)" })
public class FeimaoyunCom extends PluginForHost {
    public FeimaoyunCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/upgrade.html");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/terms.html";
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

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || host.equalsIgnoreCase("feemoo.com")) {
            return this.getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), "(\\d+)(?:\\.html)?$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        br.postPage("https://www." + this.getHost() + "/index.php/down_detaila", "code=" + this.getFID(link));
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final long status = JavaScriptEngineFactory.toLong(entries.get("status"), 0);
        if (status == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/file");
        String filename = (String) entries.get("file_name");
        final String filesize = (String) entries.get("file_size");
        if (!StringUtils.isEmpty(filename)) {
            /* Set final filename here because server filenames are bad. */
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        } else if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        final long isDeleted = JavaScriptEngineFactory.toLong(entries.get("is_del"), 0);
        if (isDeleted == 1) {
            /* 2020-12-03: Filename- and size can be given for offline items too! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String fid = getFID(link);
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (true) {
                /* 2020-12-03: Failed to find any way to download as free user */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
                final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), link);
                ajax.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
                if (ajax.toString().equals("false")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (down2_url != null) {
                    this.br.getHeaders().put("Referer", down2_url);
                }
                /* If we don't wait for some seconds here, the continue_url will redirect us to the main url!! */
                this.sleep(5 * 1001l, link);
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
            this.sleep(wait * 1001l, link);
        }
        link.setProperty(directlinkproperty, dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
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

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    /**
     * @param validateCookies
     *            true = check cookies for validity, perform full login if necessary </br>
     *            false = Just set cookies and return false if cookies are younger than 300000l
     *
     * @return true = Cookies are validated </br>
     *         false = Cookies are not validated (only set on current Browser instance)
     */
    @SuppressWarnings("deprecation")
    private boolean login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean validatedCookies = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust cookies without checking as they're still fresh");
                        return false;
                    }
                    br.getPage("https://www." + account.getHoster() + "/home.php");
                    if (isLoggedIn()) {
                        validatedCookies = true;
                    } else {
                        this.br = new Browser();
                    }
                }
                if (!validatedCookies) {
                    br.getPage("https://www." + account.getHoster() + "/home.php");
                    br.postPage("/home.php", "action=login&task=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=1");
                    // if (this.br.containsHTML("yzm\\.php")) {
                    // final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "http://" +
                    // account.getHoster(),
                    // true);
                    // final String code = getCaptchaCode("/yzm.php", dummyLink);
                    // postData += "&yzm=" + Encoding.urlEncode(code);
                    // }
                    final String status = PluginJSonUtils.getJson(br, "status");
                    if (!"true".equalsIgnoreCase(status) && !"1".equalsIgnoreCase(status)) {
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
        return br.containsHTML("logoutbtn\\(\\)");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
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
        br.setFollowRedirects(false);
        if (account.getType() == AccountType.FREE) {
            br.getPage(link.getDownloadURL());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                /* 2019-06-27: TODO: Add premium support */
                br.setFollowRedirects(true);
                br.postPage("/yythems_ajax.php", "action=load_down_addr_svip&file_id=" + this.getFID(link));
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
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account != null && account.getType() == AccountType.PREMIUM) {
            return true;
        } else {
            return false;
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