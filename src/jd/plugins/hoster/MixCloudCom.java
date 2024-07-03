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

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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
import jd.plugins.decrypter.MixCloudComCrawler;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixcloud.com" }, urls = { "https?://stream\\d+\\.mixcloud\\.com/.+|https://thumbnailer\\.mixcloud\\.com/unsafe/.+" })
public class MixCloudCom extends PluginForHost {
    public MixCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.mixcloud.com/select/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String default_extension = ".m4a";
    /* Connection stuff */
    private static final int    maxchunks         = 0;
    private String              dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://www.mixcloud.com/";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
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
            ext = ".jpg";
        } else {
            if (!StringUtils.isEmpty(dllink)) {
                ext = getFileNameExtensionFromString(dllink, default_extension);
            } else {
                ext = default_extension;
            }
        }
        filename = correctOrApplyFileNameExtension(filename, ext, null);
        dllink = Encoding.htmlOnlyDecode(dllink);
        link.setFinalFileName(filename);
        if (!isDownload) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, filename, ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    /**
     * https://www.mixcloud.com/select/ --> Users can pay to get to listen to extra stuff from creators which normal users cannot or they
     * can only listen to previews.
     */
    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            String csrftoken = null;
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Do not validate cookies */
                    return false;
                }
                br.getPage("https://" + this.getHost() + "/");
                csrftoken = MixCloudComCrawler.findCsrftoken(br);
                PostRequest request = br.createJSonPostRequest("https://app.mixcloud.com/graphql", "{\"id\":\"q33\",\"query\":\"query DashboardStatsCardQuery {viewer {id,...F1}} fragment F0 on Stats {comments {totalCount},favorites {totalCount},reposts {totalCount},plays {totalCount},minutes {totalCount},__typename} fragment F1 on Viewer {me {username,hasProFeatures,isUploader,stats {...F0},id},id}\",\"variables\":{}}");
                br.getPage(request);
                final String username = PluginJSonUtils.getJson(br, "username");
                if (!StringUtils.isEmpty(username)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(this.br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/");
            csrftoken = MixCloudComCrawler.findCsrftoken(br);
            if (csrftoken == null) {
                logger.warning("Failed to find csrftoken");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final PostRequest request = br.createPostRequest("https://app.mixcloud.com/authentication/email-login/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            request.getHeaders().put(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
            request.getHeaders().put("Accept", "*/*");
            request.getHeaders().put("Origin", "https://www.mixcloud.com");
            //
            request.getHeaders().put("X-Mixcloud-Platform", "www");
            request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage(request);
            /*
             * E.g. {"data": {"password": "test123456", "$valid": false, "email": "test123456", "$errors": {"email":
             * ["Username does not exist"]}}, "success": false}
             */
            final String isValid = PluginJSonUtils.getJson(br, "success");
            if (!StringUtils.equalsIgnoreCase(isValid, "true")) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final String hasProFeatures = PluginJSonUtils.getJson(br, "hasProFeatures");
        if ("true".equalsIgnoreCase(hasProFeatures)) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
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
        return Integer.MAX_VALUE;
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
