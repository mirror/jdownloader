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
import java.util.Locale;

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
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wushare.com" }, urls = { "https?://(?:www\\.)?wushare\\.com/file/([A-Za-z0-9]+)" })
public class WuShareCom extends PluginForHost {
    public WuShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://wushare.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://wushare.com/tos";
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
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*Page not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fInfo = br.getRegex("<span class=\"fn\">([^<>\"]*?)</span> \\(<span class=\"cb\">(\\d+)</span>\\)");
        final String filename = fInfo.getMatch(0);
        final String filesize = fInfo.getMatch(1);
        if (fInfo.patternFind()) {
            link.setName(Encoding.htmlDecode(filename).trim());
            link.setDownloadSize(Long.parseLong(filesize));
        } else {
            logger.warning("Failed to find file information");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.postPage(br.getURL(), "action=free_download");
            if ("oversize".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                throw new AccountRequiredException();
            }
            final String code = getCaptchaCode("/captcha?id=" + System.currentTimeMillis(), link);
            br.postPage(br.getURL(), "action=get_download_link&captcha_response_field=" + code);
            if (br.containsHTML("\"error_captcha\"")) {
                invalidateLastChallengeResponse();
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            validateLastChallengeResponse();
            final String waitSeconds = br.getRegex("\"status\"\\s*:\\s*\"waiting\"\\s*,\\s*\"time\"\\s*:\\s*(\\d+)").getMatch(0);
            if (waitSeconds != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitSeconds) * 1001l);
            }
            dllink = br.getRegex("\"link\"\\s*:\\s*\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.toString().equals("free members do not allows parallel downloads!")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your IP has been reported as still downloading", 10 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty("directlink", dllink);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("expire:\\s*</span><span class=\"info\">([^<>\"]*?)</span>").getMatch(0);
        if (expire == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMM yyyy", Locale.ENGLISH), br);
        final String usedSpaceBytesStr = br.getRegex(">\\s*Used space:\\s*</span><span class=\"info cb\">(\\d+)</span>").getMatch(0);
        if (usedSpaceBytesStr != null) {
            ai.setUsedSpace(Long.parseLong(usedSpaceBytesStr));
        }
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    /** 2023-10-12: https is not supported. */
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            boolean isLoggedin = false;
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(this.getHost(), cookies);
                br.getPage("http://" + this.getHost() + "/account");
                if (this.isLoggedIN(br)) {
                    logger.info("Cookie login successful");
                    isLoggedin = true;
                } else {
                    logger.info("Cookie login failed --> Full login required");
                }
            }
            if (!isLoggedin) {
                logger.info("Performing full login");
                br.postPage("http://" + this.getHost() + "/login", "stay_login=1&commit=Login&referrer=http%3A%2F%2Fwushare.com%2F&username_or_email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
            }
            if (!br.containsHTML(">\\s*Account type:\\s*</span><span class=\"info\">\\s*Premium\\s*</span>")) {
                throw new AccountInvalidException("Unsupported account type (free account)");
            }
            account.saveCookies(br.getCookies(br.getURL()), "");
        }
    }

    private boolean isLoggedIN(final Browser br) {
        return br.containsHTML("class=\"logout\"");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getPluginPatternMatcher());
        if (!this.isLoggedIN(br)) {
            /* Login failure --> Re-check account soon then cookies should get refreshed if needed! */
            throw new AccountUnavailableException("Session expired?", 1 * 60 * 1000);
        }
        final String dllink = br.getRegex("class=\"dl_link\">(https?[^<>\"]*?)</p>").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
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

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc != null && acc.getType() == AccountType.PREMIUM) {
            /* Premium account -> No captchas */
            return false;
        } else {
            return true;
        }
    }
}