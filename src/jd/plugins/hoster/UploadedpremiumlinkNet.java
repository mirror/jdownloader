//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uploadedpremiumlink.net" }, urls = { "" })
public class UploadedpremiumlinkNet extends PluginForHost {
    /** Docs: https://docs.uploadedpremiumlink.net/ */
    private final String                 API_BASE = "https://api.uploadedpremiumlink.net/wp-json/api";
    private static MultiHosterManagement mhm      = new MultiHosterManagement("uploadedpremiumlink.net");

    @SuppressWarnings("deprecation")
    public UploadedpremiumlinkNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/register");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader " + this.getVersion());
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/tos";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        final UrlQuery query = new UrlQuery();
        query.add("link", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
        if (link.getDownloadPassword() != null) {
            query.add("password", Encoding.urlEncode(link.getDownloadPassword()));
        }
        final Map<String, Object> dlresponse = (Map<String, Object>) this.accessAPI(account, link, "/generate_link_by_api", query);
        final String dllink = (String) dlresponse.get("link");
        int chunks = ((Number) dlresponse.get("maxchunks")).intValue();
        if (chunks > 1) {
            /* That means "up to X chunks" */
            chunks = -chunks;
        }
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl", 10 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            mhm.handleErrorGeneric(account, link, "Unknown download error", 10, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> user = login(account);
        final Number expire_date_timestampO = (Number) user.get("expire_date_timestamp");
        long expire_date_timestamp = 0;
        if (expire_date_timestampO != null) {
            expire_date_timestamp = expire_date_timestampO.longValue() * 1000;
        }
        if (expire_date_timestamp > System.currentTimeMillis()) {
            /* Bronze, Silver, Gold, Platinum */
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(expire_date_timestamp);
        } else {
            /* Free */
            account.setType(AccountType.FREE);
            ai.setExpired(true);
        }
        ai.setStatus(user.get("type").toString() + " | Total traffic left: " + user.get("traffic_left") + " | Daily links generated: " + user.get("daily_links_generated") + "/" + user.get("daily_links_limit"));
        ai.setTrafficLeft(((Number) user.get("daily_traffic_left")).longValue());
        ai.setTrafficMax(((Number) user.get("daily_traffic_limit")).longValue());
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final List<Map<String, Object>> hosters = (List<Map<String, Object>>) user.get("hosters");
        for (final Map<String, Object> hoster : hosters) {
            final List<String> domains = (List<String>) hoster.get("alternatives_domain");
            final String primary_domain = hoster.get("primary_domain").toString();
            if ("online".equals(hoster.get("status"))) {
                supportedHosts.addAll(domains);
            } else {
                logger.info("Skipping currently unsupported/offline host: " + primary_domain);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private Map<String, Object> login(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            logger.info("Performing full login");
            final Map<String, Object> entries = (Map<String, Object>) this.accessAPI(account, null, "/user", new UrlQuery());
            return entries;
        }
    }
    // private Object accessAPI(final Account account, final DownloadLink link, final String path) {
    // return null;
    // }

    private Object accessAPI(final Account account, final DownloadLink link, final String path, final UrlQuery postdata) throws IOException, PluginException, InterruptedException {
        postdata.add("api_key", Encoding.urlEncode(account.getPass()));
        final PostRequest req = br.createPostRequest(this.API_BASE + path, postdata);
        return accessAPI(account, link, req);
    }

    private Object accessAPI(final Account account, final DownloadLink link, final Request req) throws IOException, PluginException, InterruptedException {
        br.getPage(req);
        /* 2024-09-02: Temporary workaround for buggy response containing a warning */
        final String json = br.getRegex("/>\\s*(\\{.+)").getMatch(0);
        if (json != null) {
            br.getRequest().setHtmlCode(json);
        }
        return checkErrors(account, link);
    }

    private Object checkErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        try {
            final Object jsonO = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            if (jsonO == null || !(jsonO instanceof Map)) {
                return jsonO;
            }
            final Map<String, Object> resp = (Map<String, Object>) jsonO;
            final Map<String, Object> data = (Map<String, Object>) resp.get("data");
            if (Boolean.TRUE.equals(resp.get("success"))) {
                /* Success */
                return data;
            }
            handleErrorMap(account, link, data);
            return jsonO;
        } catch (final JSonMapperException jme) {
            if (link != null) {
                mhm.handleErrorGeneric(account, link, "Bad API answer", 50, 5 * 60 * 1000l);
            } else {
                throw Exceptions.addSuppressed(new AccountUnavailableException("Bad API answer", 1 * 60 * 1000l), jme);
            }
        }
        return null;
    }

    private void handleErrorMap(final Account account, final DownloadLink link, final Map<String, Object> entries) throws PluginException, InterruptedException {
        final HashSet<String> accountErrorsPermanent = new HashSet<String>();
        accountErrorsPermanent.add("AUTH_MISSING_APIKEY");
        accountErrorsPermanent.add("AUTH_BAD_APIKEY");
        final HashSet<String> accountErrorsTemporary = new HashSet<String>();
        accountErrorsTemporary.add("AUTH_USER_BANNED");
        accountErrorsTemporary.add("AUTH_IP_BANNED");
        accountErrorsTemporary.add("DAILY_LIMIT_EXCEEDED");
        accountErrorsTemporary.add("LINK_GENERATION_LIMIT_EXCEEDED");
        accountErrorsTemporary.add("PREMIUM_TRAFFIC_UNAVAILABLE");
        accountErrorsTemporary.add("RATE_LIMIT_EXCEEDED");
        accountErrorsTemporary.add("SERVER_TRAFFIC_LIMIT_REACHED");
        accountErrorsTemporary.add("TRAFFIC_LIMIT_EXCEEDED");
        accountErrorsTemporary.add("USER_QUOTA_EXCEEDED");
        accountErrorsTemporary.add("WEEKLY_LIMIT_EXCEEDED");
        accountErrorsTemporary.add("FREE_TRAFFIC_LIMIT_EXCEEDED");
        accountErrorsTemporary.add("FREE_USER_DAILY_LIMIT_EXCEEDED");
        accountErrorsTemporary.add("PROXY_NOT_ALLOWED");
        final HashSet<String> downloadErrorsHostUnavailable = new HashSet<String>();
        downloadErrorsHostUnavailable.add("HOSTER_MAINTENANCE");
        downloadErrorsHostUnavailable.add("HOSTER_NOT_AVAILABLE_BY_API");
        downloadErrorsHostUnavailable.add("HOSTER_TEMPORARILY_UNAVAILABLE");
        downloadErrorsHostUnavailable.add("UNSUPPORTED_HOSTER");
        final HashSet<String> downloadErrorsFileUnavailable = new HashSet<String>();
        downloadErrorsFileUnavailable.add("BAD_LINK");
        downloadErrorsFileUnavailable.add("FILESIZE_LIMIT");
        downloadErrorsFileUnavailable.add("FILESIZE_LIMIT_FREE");
        downloadErrorsFileUnavailable.add("FILESIZE_LIMIT_GUEST");
        downloadErrorsFileUnavailable.add("LINK_ERROR");
        downloadErrorsFileUnavailable.add("LINK_GENERATION_FAILED");
        downloadErrorsFileUnavailable.add("LINK_TEMPORARILY_UNAVAILABLE");
        downloadErrorsFileUnavailable.add("LINK_TYPE_UNSUPPORTED");
        downloadErrorsFileUnavailable.add("MUST_BE_PREMIUM");
        downloadErrorsFileUnavailable.add("RESOURCE_RETRIEVAL_FAILURE");
        /** TODO: LINK_PASS_PROTECTED, LINK_THIRD_PARTY_PR_SUB_REQUIRED */
        final String message = entries.get("message").toString();
        final String category_error = entries.get("category_error").toString();
        if (accountErrorsPermanent.contains(category_error)) {
            throw new AccountInvalidException(message);
        } else if (downloadErrorsFileUnavailable.contains(category_error)) {
            throw new AccountUnavailableException(message, 5 * 60 * 1000);
        } else if (downloadErrorsHostUnavailable.contains(category_error)) {
            mhm.putError(accountErrorsTemporary, link, 5 * 60 * 1000l, message);
        } else if (downloadErrorsFileUnavailable.contains(category_error)) {
            mhm.handleErrorGeneric(account, link, message, 20);
        } else if (category_error.equalsIgnoreCase("FILE_NOT_FOUND")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, message);
        } else {
            /* Unknown error code */
            if (link != null) {
                mhm.handleErrorGeneric(account, link, message, 50, 1 * 60 * 1000l);
            } else {
                throw new AccountUnavailableException(message, 5 * 60 * 1000);
            }
        }
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "https://www." + getHost() + "/myaccount/";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[a-zA-Z0-9]{24}")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}