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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixcloud.com" }, urls = { "https?://stream\\d+\\.mixcloud\\.com/.+|https://thumbnailer\\.mixcloud\\.com/unsafe/.+" })
public class MixCloudCom extends antiDDoSForHost {
    public MixCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.mixcloud.com/select/");
    }
    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".m4a";
    /* Connection stuff */
    private static final boolean resume            = true;
    private static final int     maxchunks         = 0;
    private static final int     maxdownloads      = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://www.mixcloud.com/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        br.setFollowRedirects(true);
        final String url_filename = new Regex(link.getDownloadURL(), "mixcloud\\.com/(.+)").getMatch(0);
        String filename = link.getFinalFileName();
        if (filename == null) {
            filename = url_filename;
        }
        dllink = link.getDownloadURL();
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ext;
        if (dllink.contains("thumbnailer")) {
            ext = ".jpeg";
        } else {
            if (!StringUtils.isEmpty(dllink)) {
                ext = getFileNameExtensionFromString(dllink, default_extension);
            } else {
                ext = default_extension;
            }
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        dllink = Encoding.htmlDecode(dllink);
        link.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setProperty("directlink", dllink);
            } else {
                server_issues = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxdownloads;
    }

    /**
     * https://www.mixcloud.com/select/ --> Users can pay to get to listen to extra stuff from creators which normal users cannot or they
     * can only listen to previews.
     */
    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                String csrftoken = null;
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    getPage("https://" + this.getHost() + "/");
                    csrftoken = br.getCookie(br.getHost(), "csrftoken", Cookies.NOTDELETEDPATTERN);
                    br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                    br.getHeaders().put("x-csrftoken", csrftoken);
                    br.getHeaders().put("accept", "application/json");
                    br.getHeaders().put("content-type", "application/json");
                    this.postPageRaw("https://www.mixcloud.com/graphql", "{\"id\":\"q33\",\"query\":\"query DashboardStatsCardQuery {viewer {id,...F1}} fragment F0 on Stats {comments {totalCount},favorites {totalCount},reposts {totalCount},plays {totalCount},minutes {totalCount},__typename} fragment F1 on Viewer {me {username,hasProFeatures,isUploader,stats {...F0},id},id}\",\"variables\":{}}");
                    final String username = PluginJSonUtils.getJson(br, "username");
                    if (!StringUtils.isEmpty(username)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                getPage("https://" + this.getHost() + "/");
                csrftoken = br.getCookie(br.getHost(), "csrftoken", Cookies.NOTDELETEDPATTERN);
                if (csrftoken == null) {
                    logger.warning("Failed to find csrftoken");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.getHeaders().put("x-csrftoken", csrftoken);
                postPage("/authentication/email-login/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                /*
                 * E.g. {"data": {"password": "test123456", "$valid": false, "email": "test123456", "$errors": {"email":
                 * ["Username does not exist"]}}, "success": false}
                 */
                final String isValid = PluginJSonUtils.getJson(br, "success");
                if (!StringUtils.equalsIgnoreCase(isValid, "true")) {
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        final String hasProFeatures = PluginJSonUtils.getJson(br, "hasProFeatures");
        if ("true".equalsIgnoreCase(hasProFeatures)) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium user");
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) user");
        }
        ai.setUnlimitedTraffic();
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        /* We got direct-URLs so login is usually not even needed - only in case these direct-URLs expire. */
        this.handleFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxdownloads;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
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
