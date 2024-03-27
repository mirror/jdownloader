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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MooncloudSite extends PluginForHost {
    public MooncloudSite(PluginWrapper wrapper) {
        super(wrapper);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.enablePremium("https://" + getHost() + "/pricing");
        }
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mooncloud.site" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFolderID(link);
        if (fid != null) {
            return "mooncloud_site://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFolderID(final DownloadLink link) {
        try {
            final UrlQuery query = UrlQuery.parse(link.getPluginPatternMatcher());
            final String folderID = query.get("id");
            return folderID;
        } catch (final MalformedURLException mue) {
            return null;
        }
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

    public int getMaxChunks(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    private final String PROPERTY_ACCOUNT_TOKEN = "accesstoken";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String fid = this.getFolderID(link);
        final UrlQuery query = UrlQuery.parse(link.getPluginPatternMatcher());
        final String folderID = query.get("id");
        if (folderID != null) {
            final String extDefault = ".zip";
            link.setFinalFileName(fid + extDefault);
            query.add("userId", fid);
            br.getPage("https://" + getHost() + "/download?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            try {
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final long filesize = ((Number) entries.get("size")).longValue();
                link.setVerifiedFileSize(filesize);
            } catch (final Throwable e) {
                // Non-json response
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } else {
            /* Direct-URL -> No need to check this here */
            directurlFindAndSetFilehash(link, link.getPluginPatternMatcher());
            return AvailableStatus.UNCHECKABLE;
        }
    }

    public static void directurlFindAndSetFilehash(final DownloadLink link, final String directurl) {
        final String md5hash = new Regex(directurl, "/([A-Fa-f0-9]{32})[^/]*").getMatch(0);
        if (md5hash != null) {
            link.setMD5Hash(md5hash);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        throw new AccountRequiredException();
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String directlinkproperty = "directurl_" + (account != null ? account.getType().getLabel() : null);
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        final UrlQuery query = UrlQuery.parse(link.getPluginPatternMatcher());
        final String folderID = query.get("id");
        String dllink;
        boolean storeDirecturl = true;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            if (folderID != null) {
                // TODO
                storeDirecturl = true;
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                storeDirecturl = false;
                // TODO: Add refresh for expired URLs
                dllink = link.getPluginPatternMatcher();
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        if (storedDirecturl == null && storeDirecturl) {
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final String storedToken = account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
            if (storedToken != null) {
                logger.info("Attempting token login");
                br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + storedToken);
                if (!force) {
                    /* Don't validate cookies */
                    return null;
                }
                br.getPage("https://api." + getHost() + "/me");
                try {
                    final Map<String, Object> me = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    final String email = me.get("email").toString();
                    if (email.equalsIgnoreCase(account.getUser())) {
                        logger.info("Token login successful");
                        return me;
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
                logger.info("Token login failed");
                account.removeProperty(storedToken);
            }
            logger.info("Performing full login");
            // br.getPage("https://" + this.getHost() + "/login.php");
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("email", account.getUser());
            postdata.put("password", account.getPass());
            br.postPageRaw("https://api." + getHost() + "/login", JSonStorage.serializeToJson(postdata));
            if (!br.getRequest().getHtmlCode().startsWith("{")) {
                /* No json response -> Invalid login credentials */
                throw new AccountInvalidException();
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String accessToken = (String) entries.get("accessToken");
            final String errorStr = (String) entries.get("error");
            if (errorStr != null) {
                throw new AccountInvalidException("Login failed - API error: " + errorStr);
            } else if (StringUtils.isEmpty(accessToken)) {
                /* Login failed for unknown reasons */
                throw new AccountInvalidException();
            }
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + accessToken);
            account.setProperty(PROPERTY_ACCOUNT_TOKEN, accessToken);
            br.getPage("/me");
            final Map<String, Object> me = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            return me;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> me = login(account, true);
        if (Boolean.TRUE.equals(me.get("active"))) {
            /* We know that this is an active premium account. We do not know anything else about it. */
            account.setType(AccountType.PREMIUM);
        } else {
            ai.setExpired(true);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}