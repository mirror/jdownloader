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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dufile.com" }, urls = { "https?://(?:www\\.)?dufile\\.com/(?:file|down)/([a-z0-9]+)\\.html" })
public class DuFileCom extends PluginForHost {
    public DuFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.sufile.com/upgrade.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.sufile.com/terms.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    /* Further tests were impossible due to a lack of example links. */
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 10;
    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("/(vip|down)/", "/file/"));
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), "([a-z0-9]+)\\.html$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)404 Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("class=\"title\">([^<>\"]*?)</h2>").getMatch(0);
        final String filesize = br.getRegex("文件大小：<b>([^<>\"]*?)</b>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Set final filename here because server filenames are bad. */
        link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            requestFileInformation(link);
            /* Check for "limit reached" message. */
            final String waitMinutes = br.getRegex("(?i)id=\"down_interval_tag\"[^>]*>(\\d+)</span>\\s*分钟<").getMatch(0);
            if (waitMinutes != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waitMinutes) * 60 * 1001l);
            }
            final boolean skipWaittime = false;
            if (!skipWaittime) {
                int wait = 30;
                final String waittime = br.getRegex("id=\"wait_input\" style=\"font-weight:bold;font-size:22px; color: green;\">(\\d+)</span>").getMatch(0);
                if (waittime != null) {
                    wait = Integer.parseInt(waittime);
                }
                this.sleep(wait * 1001l, link);
            }
            final String fid = getFID(link);
            br.getPage("/down/" + fid + ".html");
            final String code = getCaptchaCode("/down_code.php", link);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/down_code.php", "action=yz&id=" + fid + "&code=" + Encoding.urlEncode(code));
            if (br.toString().equals("0")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            br.getPage("/dd.php?file_key=" + fid + "&p=1");
            dllink = br.getRegex("<a id=\"downs\" href=\"(http[^\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("href=\"(http[^\"]+\\w+\\.sufile\\.net(?::\\d+)?/down/[^\"]+)").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.getURL().endsWith("/two.html")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultaneous downloads", 1 * 60 * 1000l);
            } else if (br.containsHTML(">\\s*普通用户只允许同时下载一个文件")) {
                /* 2022-02-25 */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultaneous downloads", 1 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dllink);
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
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
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

    @SuppressWarnings("deprecation")
    private void login(final Account account) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Verifying login cookies");
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("http://" + this.getHost() + "/member/");
                    if (this.br.containsHTML("/logout.php")) {
                        /* Save cookie timestamp. */
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("http://www." + this.getHost() + "/login.html");
                String postData = "type=login&nick=" + Encoding.urlEncode(account.getUser()) + "&pwd=" + Encoding.urlEncode(account.getPass());
                if (this.br.containsHTML("yzm\\.php")) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "sufile.com", "http://" + this.getHost(), true);
                    final String code = getCaptchaCode("/yzm.php", dummyLink);
                    postData += "&yzm=" + Encoding.urlEncode(code);
                }
                br.postPage("/post.php", postData);
                if (!br.toString().equals("1")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder login Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account);
        br.getPage("/member/userinfo.php");
        final String expire = br.getRegex(">到期时间：</span><span class=\\\\mr15 w300 dib\">([^<>\"]*?)</span>").getMatch(0);
        ai.setUnlimitedTraffic();
        if (br.containsHTML("href=\"upvip\\.php\" title=\"升级为VIP会员\"") || expire == null) {
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.CHINA));
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            handleDownload(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                requestFileInformation(link);
                login(account);
                br.setFollowRedirects(false);
                br.setFollowRedirects(true);
                br.getPage(link.getPluginPatternMatcher());
                /* Example of a finallink: \"(https?://vip\\d+[a-z0-9\\-\\.]+\\.sufile.net:3660/down/filename?key=xxx */
                dllink = br.getRegex("\"(https?://vip\\d+[a-z0-9\\-\\.]+\\.sufile[^<>\"]*?)\"").getMatch(0);
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
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
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