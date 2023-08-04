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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class XunniupanCom extends PluginForHost {
    public XunniupanCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www." + this.getHost() + "/vip.php");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xunniu-pan.com", "xunniufile.com", "xunniupan.co", "xunniupan.com", "xun-niu.com" });
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

    @Override
    public String rewriteHost(final String host) {
        /* This filehost is frequently changing its domain which is why we need this. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:file|down|down2)\\-([a-z0-9]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "http://www." + this.getHost() + "/about.php?action=help";
    }

    /* Connection stuff */
    /* 2019-09-12: Failed to test any free download as it seems like all files they host are PREMIUMONLY! */
    private final int    FREE_MAXDOWNLOADS            = 1;
    private final int    ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private final int    ACCOUNT_PREMIUM_MAXDOWNLOADS = 5;
    private final String PROPERTY_DIRECTURL_PREMIUM   = "premlink";

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    protected String getDownloadModeDirectlinkProperty(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return "freelink2";
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return PROPERTY_DIRECTURL_PREMIUM;
        } else {
            /* Free(anonymous) and unknown account type */
            return "freelink";
        }
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

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = this.getFID(link);
        if (account != null) {
            this.login(account, false);
        }
        br.getPage("http://www." + this.getHost() + "/file-" + fid + ".html");
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">文件资源若被删除，可能的原因有|内容涉及不良信息。")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<div class=\"span\\d+\">\\s*<h1>([^<>\"]+)</h1>").getMatch(0);
        String filesize = br.getRegex("(?i)>\\s*文件大小\\s*：([^<>\"]+)<").getMatch(0);
        if (filename != null) {
            /* Set final filename here because server filenames are bad. */
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            filesize += "b";
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String fid = getFID(link);
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
            /*
             * Important! Correct Referer header needs to be present otherwise previously generated directurls will just redirect to
             * main-page.
             */
            br.getHeaders().put("Referer", link.getPluginPatternMatcher());
        } else {
            requestFileInformation(link, account);
            if (br.containsHTML("action=get_vip_fl")) {
                /* Premium account */
                br.postPage("/ajax.php", "action=get_vip_fl&file_id=" + this.getFID(link));
                final String urls_text = br.getRegex("true\\|(http.+)").getMatch(0);
                if (urls_text != null) {
                    final String[] mirrors = urls_text.split("\\|");
                    if (mirrors.length > 0) {
                        /* 2020-07-16: Chose random mirror */
                        final int mirrorNumber = new Random().nextInt(mirrors.length - 1);
                        dllink = mirrors[mirrorNumber];
                        logger.info("Selecting random mirror number: " + mirrorNumber + " | Link: " + dllink);
                    }
                }
            } else {
                /* Free account / No account */
                final boolean skipWaittime = false;
                final boolean skipCaptcha = false;
                if (!skipWaittime) {
                    /* 2019-09-12: Defaultvalue = 50 */
                    int wait = 0;
                    final String waittime = br.getRegex("var\\s*secs\\s*=\\s*(\\d+);").getMatch(0);
                    if (waittime != null) {
                        wait = Integer.parseInt(waittime);
                    } else {
                        logger.warning("Failed to find any pre download waittime value");
                    }
                    if (wait > 180) {
                        /* High waittime --> Reconnect is faster than waiting :) */
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                    }
                    if (wait > 0) {
                        this.sleep(wait * 1001l, link);
                    } else {
                        logger.info("No pre download waittime needed");
                    }
                }
                /*
                 * 2019-09-12: General procedure: /down2 --> /down --> Captcha --> Download(?) --> Failed to start a single free download
                 * via browser until now!
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
                int counter = -1;
                if ((br.containsHTML("imagecode\\.php") || true) && !skipCaptcha) {
                    do {
                        counter++;
                        final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), link);
                        ajax.postPage(action, "action=check_code&code=" + Encoding.urlEncode(code));
                        if (ajax.toString().equals("false")) {
                            continue;
                        }
                        failed = false;
                        break;
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
                {
                    /* 2021-04-08 */
                    dllink = ajax.getRegex("\"([^\"]*dl2\\.php[^\"]+)\"").getMatch(0);
                }
                if (dllink == null) {
                    dllink = ajax.getRegex("true\\|<a href=\"([^<>\"]+)").getMatch(0);
                }
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
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dllink);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, this.getMaxChunks(account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 503) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired");
            } else {
                throw e;
            }
        }
        dl.startDownload();
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
    private boolean login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust cookies without checking as they're still fresh");
                        return false;
                    }
                    logger.info("Validating cookies...");
                    br.getPage("http://www." + account.getHoster() + "/mydisk.php");
                    if (isLoggedIn()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                        this.br.clearCookies(br.getHost());
                    }
                }
                /*
                 * 2019-09-12: Every full login will invalidate al older sessions (user will have to re-login via browser)! If users
                 * complain about too many login captchas, tell them to only login via browser OR JDownloader to avoid this!
                 */
                logger.info("Performing full login");
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

    private boolean isLoggedIn() {
        return br.getCookie(br.getHost(), "phpdisk_zcore_v2_info", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (br.getURL() == null || !br.getURL().contains("/mydisk.php")) {
            br.getPage("/mydisk.php");
        }
        long expire = 0;
        String expireStr = br.getRegex("(?i)>\\s*VIP结束时间</b>\\s*：\\s*<span class=\"txt_r\">(\\d{4}-\\d{2}-\\d{2})</span>").getMatch(0);
        if (expireStr != null) {
            expire = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd", Locale.CHINA);
        }
        ai.setUnlimitedTraffic();
        if (expire < System.currentTimeMillis()) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        } else {
            ai.setValidUntil(expire, this.br);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PhpDisk;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /*
         * 2021-02-22: Remove directurl so next attempt, another random mirror will be selected. This host provides multiple mirrors and
         * speeds may vary!
         */
        link.removeProperty(PROPERTY_DIRECTURL_PREMIUM);
    }
}