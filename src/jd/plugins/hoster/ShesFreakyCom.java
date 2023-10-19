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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shesfreaky.com" }, urls = { "https?://(?:www\\.)?shesfreaky\\.com/(?:video/[A-Za-z0-9\\-_]+-\\d+\\.html|embed/\\d+)" })
public class ShesFreakyCom extends PluginForHost {
    public ShesFreakyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/signup");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        br.setFollowRedirects(true);
        return br;
    }

    private String              dllink      = null;
    private static final String TYPE_NORMAL = "(?i)https?://[^/]+/video/([A-Za-z0-9\\-_]+)-(\\d+)\\.html";
    private static final String TYPE_EMBED  = "(?i)https?://[^/]+/embed/(\\d+)";

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/terms/";
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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1);
        }
    }

    private String getWeakFileTitle(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            final Regex patternNormal = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL);
            if (patternNormal.patternFind()) {
                return patternNormal.getMatch(0).replace("-", " ").trim();
            } else {
                return this.getFID(link);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(getWeakFileTitle(link) + extDefault);
        }
        this.setBrowserExclusive();
        final String contenturl;
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            /* Do this so we can find meaningful filenames even for embed URLs. */
            contenturl = "https://www." + this.getHost() + "/video/-" + this.getFID(link) + ".html";
        } else {
            contenturl = link.getPluginPatternMatcher();
        }
        if (account != null) {
            this.login(account, contenturl, true);
        } else {
            br.getPage(link.getPluginPatternMatcher());
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"error\"|>404: File Not Found<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("(?i)<title>([^<]+)</title>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            title = title.replaceFirst("\\s*- ShesFreaky$", "");
            link.setFinalFileName(title + extDefault);
        }
        dllink = br.getRegex("url: escape\\(\\'(https?://[^<>\"]*?)\\'\\)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"(.*?)\"").getMatch(0);
        }
        if (dllink != null && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (this.br.containsHTML("class=\"private\\-video\"")) {
            /* E.g. 'You must be friends with blabla to video this content' */
            throw new AccountRequiredException();
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (this.br.containsHTML("class=\"private\\-video\"")) {
            /* E.g. 'You must be friends with blabla to video this content' */
            throw new AccountRequiredException();
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    private boolean login(final Account account, final String checkurl, final boolean force) throws Exception {
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
                if (checkurl != null) {
                    br.getPage(checkurl);
                } else {
                    br.getPage("https://www." + this.getHost() + "/");
                }
                if (this.isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(this.br.getCookies(this.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://www." + this.getHost() + "/login");
            final Form loginform = br.getFormbyProperty("id", "formLogin");
            if (loginform == null) {
                logger.warning("Failed to find loginform");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("ahd_username", Encoding.urlEncode(account.getUser()));
            loginform.put("ahd_password", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginform);
            if (!isLoggedin(br)) {
                throw new AccountInvalidException();
            }
            if (checkurl != null) {
                br.getPage(checkurl);
            }
            account.saveCookies(this.br.getCookies(this.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, null, true);
        ai.setUnlimitedTraffic();
        account.setConcurrentUsePossible(true);
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
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
