//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tropicshare.com" }, urls = { "https?://(?:www\\.)?tropicshare\\.com/files/(\\d+)" })
public class TropicShareCom extends PluginForHost {
    public TropicShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://tropicshare.com/users/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://tropicshare.com/pages/6-Terms-of-service.html";
    }

    private static AtomicReference<String> userAgent = new AtomicReference<String>(null);

    private void prepBR(final Browser br) {
        if (userAgent.get() == null) {
            userAgent.set(UserAgents.stringUserAgent(BrowserName.Chrome));
        }
        br.getHeaders().put("User-Agent", userAgent.get());
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBR(br);
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("(?i)>\\s*File size: </span>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fInfo = br.getRegex("(?i)<h2>([^<>\"]*?)<span style=\"float: right;font\\-size: 12px;\">File size: ([^<>\"]*?)</span>");
        final String filename = fInfo.getMatch(0);
        final String filesize = fInfo.getMatch(1);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String fid = br.getRegex("fileid=\"(\\d+)\"").getMatch(0);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int wait = 60;
        final String regexedwaitStr = br.getRegex("<count>(\\d+)</count").getMatch(0);
        if (regexedwaitStr != null) {
            wait = Integer.parseInt(regexedwaitStr);
        } else {
            logger.warning("Failed to find pre-download-wait-seconds in HTML code");
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("/files/time/", "id=" + fid);
        if (br.containsHTML("(?i)\"status\":\"Please wait, while downloading\"")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        final String uid = PluginJSonUtils.getJsonValue(br, "uid");
        if (uid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        sleep(wait * 1001l, link);
        final String dllink = "/files/download/?uid=" + uid;
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("(?i)Error, you not have premium acount")) {
                throw new AccountRequiredException();
            } else if (br.containsHTML("(?i)Waiting time: ")) {
                final String minutes = br.getRegex("<span id=\"min\">(\\d+)</span>").getMatch(0);
                final String seconds = br.getRegex("<span id=\"sec\">(\\d+)</span>").getMatch(0);
                if (minutes != null && seconds != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(minutes) * 60 * 1001 + Integer.parseInt(seconds) * 1001l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                }
            } else if (br.containsHTML("(?i)For parallel downloads please select one of Premium packages")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error: Got html code instead of file");
            }
        }
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            prepBR(br);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Do not validate cookies. */
                    return;
                } else {
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        account.clearCookies(null);
                    }
                }
            }
            br.setFollowRedirects(false);
            br.postPage("http://" + this.getHost() + "/users/login", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.getCookie(br.getHost(), "login_sid", Cookies.NOTDELETEDPATTERN) == null) {
                throw new AccountInvalidException();
            }
            account.saveCookies(this.br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("class=\"settings-panel\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /* Make sure we are on the main page. */
        br.getPage("/");
        final Regex expireInfo = br.getRegex("(?i)Remain:\\s*<span>\\s*(\\d+)\\s*</span>\\s*Months?\\s*<span>\\s*(\\d+)\\s*</span>\\s*Days?");
        if (!br.containsHTML("<span>\\s*Premium") || expireInfo.getMatches().length != 1) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new AccountInvalidException("\r\nNicht unterstützter Accounttyp!\r\nJDownloader unterstützt nur premium Accounts dieses Anbieters!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.");
            } else {
                throw new AccountInvalidException("\r\nUnsupported account type!\r\nJDownloader is only supporting premium accounts of this provider!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.");
            }
        }
        ai.setUnlimitedTraffic();
        final String months = expireInfo.getMatch(0);
        final String days = expireInfo.getMatch(1);
        final long monthsSeconds = Integer.parseInt(months) * 31 * 24 * 60 * 60;
        final long daysSeconds = Integer.parseInt(days) * 24 * 60 * 60;
        final long expireMilliseconds = (monthsSeconds + daysSeconds) * 1001;
        ai.setValidUntil(System.currentTimeMillis() + expireMilliseconds, br);
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* 2023-01-25: Check login before every download as their cookies sometimes randomly expire(?) */
        login(account, true);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, -4);
        if (this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.info("User has direct downloads ENABLED");
        } else {
            logger.info("User has direct downloads DISABLED (or something else went wrong)");
            br.followConnection(true);
            final String fid = br.getRegex("fileid=\"(\\d+)\"").getMatch(0);
            if (fid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String dllink = "/files/download/premium/" + fid;
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -4);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error: Got html code instead of file");
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}