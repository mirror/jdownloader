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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "torbox.app" }, urls = { "" })
public class TorboxApp extends PluginForHost {
    /* API docs: https://api-docs.torbox.app/ */
    private final String                 API_BASE                    = "https://api.torbox.app/v1/api";
    private static MultiHosterManagement mhm                         = new MultiHosterManagement("torbox.app");
    private final boolean                resume                      = true;
    private final int                    maxchunks                   = 0;
    private final String                 PROPERTY_SERVERSIDE_FILE_ID = "file_id";

    @SuppressWarnings("deprecation")
    public TorboxApp(PluginWrapper wrapper) {
        super(wrapper);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.enablePremium("https://" + this.getHost() + "/pricing");
        }
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/terms";
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

    private String getPropertyKey(final String property) {
        return this.getHost() + "_" + property;
    }

    private String getMultihosterFileID(final DownloadLink link) {
        return link.getStringProperty(getPropertyKey(PROPERTY_SERVERSIDE_FILE_ID));
    }

    private void setMultihosterFileID(final DownloadLink link, final String file_id) {
        link.setProperty(getPropertyKey(PROPERTY_SERVERSIDE_FILE_ID), file_id);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        this.login(account, false);
        if (!attemptStoredDownloadurlDownload(link)) {
            String file_id = getMultihosterFileID(link);
            if (file_id == null) {
                logger.info("Creating internal file_id");
                final Map<String, Object> postdata = new HashMap<String, Object>();
                postdata.put("url", link.getDefaultPlugin().buildExternalDownloadURL(link, this));
                /*
                 * This timestamp is later used to notify the user that everything requested within a specified timespan has been
                 * downloaded.
                 */
                postdata.put("group_id", System.currentTimeMillis() / 1000);
                postdata.put("notif_db", false);
                postdata.put("notif_email", false);
                br.postPageRaw(API_BASE + "/services/downloadfile", JSonStorage.serializeToJson(postdata));
                this.checkErrors(account, link);
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                handleErrorMap(account, link, entries);
                file_id = entries.get("file_id").toString();
                /* Save this ID to re-use on next try. */
                this.setMultihosterFileID(link, file_id);
            }
            logger.info("Trying to init download for internal file_id: " + file_id);
            final UrlQuery query = new UrlQuery();
            query.add("id", file_id);
            query.add("sort", "id");
            query.add("order", "asc");
            query.add("offset", "0");
            query.add("limit", "1");
            /*
             * This will return a list of files based on our criteria --> This list should only contain one result which is the file we
             * want.
             */
            br.getPage(API_BASE + "/files?" + query.toString());
            final Map<String, Object> dlresponse = restoreFromString(br.toString(), TypeRef.MAP);
            handleErrorMap(account, link, dlresponse);
            final List<Map<String, Object>> files = (List<Map<String, Object>>) dlresponse.get("result");
            if (files.isEmpty()) {
                /* We're too fast or server too slow: Expected file is not yet on serverside queue list. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverside download in progress (serverside file_id " + file_id + " is not yet on queue list)", 5 * 1000l);
            }
            final Map<String, Object> file = files.get(0);
            final String dllink = (String) file.get("download_url");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverside download in progress " + file.get("download_percent") + "%", 10 * 1000l);
            }
            link.setProperty(this.getPropertyKey("directlink"), dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String directurlproperty = this.getPropertyKey("directlink");
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(directurlproperty);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                dl = null;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> user = login(account, true);
        final String created_at = user.get("created_at").toString();
        final String premium_expires_at = (String) user.get("premium_expires_at");
        long premiumExpireTimestamp = -1;
        if (premium_expires_at != null) {
            premiumExpireTimestamp = TimeFormatter.getMilliSeconds(premium_expires_at, "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH);
        }
        final Boolean is_subscribed = (Boolean) user.get("is_subscribed");
        ai.setCreateTime(TimeFormatter.getMilliSeconds(created_at, "yyyy-MM-dd'T'HH:mm:ss.SSSSX", Locale.ENGLISH));
        ai.setValidUntil(TimeFormatter.getMilliSeconds(premium_expires_at, "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH));
        if (premiumExpireTimestamp > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        ai.setStatus(account.getType().getLabel() + " | Subscribed: " + is_subscribed + " | Dl so far: " + SizeFormatter.formatBytes(((Number) user.get("total_bytes_downloaded")).longValue()));
        final Request req = br.createGetRequest(API_BASE + "/webdl/hosters");
        final Map<String, Object> hostinfo = (Map<String, Object>) this.callAPI(req, account, null);
        final List<Map<String, Object>> hosterlist = (List<Map<String, Object>>) hostinfo.get("data");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final Map<String, Object> hosterlistitem : hosterlist) {
            final String domain = hosterlistitem.get("domain").toString();
            if ((Boolean) hosterlistitem.get("status")) {
                supportedHosts.add(domain);
            } else {
                logger.info("Skipping currently unsupported/offline host: " + domain);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private Map<String, Object> login(final Account account, final boolean validateLogins) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            final Request loginreq = br.createGetRequest(API_BASE + "/user/me");
            final Map<String, Object> resp = (Map<String, Object>) this.callAPI(loginreq, account, null);
            return (Map<String, Object>) resp.get("data");
        }
    }

    private Object callAPI(final Request req, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        req.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + account.getPass());
        br.getPage(req);
        return checkErrors(account, link);
    }

    private Object checkErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        try {
            final Object jsonO = restoreFromString(br.toString(), TypeRef.OBJECT);
            if (jsonO == null || !(jsonO instanceof Map)) {
                return jsonO;
            }
            handleErrorMap(account, link, (Map<String, Object>) jsonO);
            return jsonO;
        } catch (final JSonMapperException jme) {
            final String errortext = "Bad API response";
            if (link != null) {
                mhm.handleErrorGeneric(account, this.getDownloadLink(), errortext, 50, 5 * 60 * 1000l);
            } else {
                throw Exceptions.addSuppressed(new AccountUnavailableException(errortext, 1 * 60 * 1000l), jme);
            }
        }
        return null;
    }

    private void handleErrorMap(final Account account, final DownloadLink link, final Map<String, Object> entries) throws PluginException, InterruptedException {
        // TODO: Add functionality
        final Object errorO = entries.get("error");
        if (errorO == null) {
            /* No error */
            return;
        }
        final int errorcode = ((Number) errorO).intValue();
        if (errorcode == 0) {
            /* No error */
            return;
        }
        final Map<Integer, String> errorCodeMapping = null;
        if (errorCodeMapping.containsKey(errorcode)) {
            /* Known errorcode */
            final String errorMsg = errorCodeMapping.get(errorcode);
            if (errorcode == 105) {
                /* IP is temporarily banned */
                throw new AccountUnavailableException(errorMsg, 5 * 60 * 1000l);
            } else if (errorcode == 106) {
                /* Invalid logins */
                throw new AccountInvalidException(errorMsg);
            } else if (link != null) {
                mhm.handleErrorGeneric(account, link, "Error " + errorcode, 50, 1 * 60 * 1000l);
            } else {
                throw new AccountUnavailableException(errorMsg, 3 * 60 * 1000l);
            }
        } else {
            /* Unknown errorcode */
            if (link != null) {
                mhm.handleErrorGeneric(account, link, "Error " + errorcode, 50, 1 * 60 * 1000l);
            } else {
                throw new AccountInvalidException("Error " + errorcode);
            }
        }
    }

    private static String correctPassword(final String pw) {
        if (pw != null) {
            return pw.trim().replace("-", "");
        } else {
            return null;
        }
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "https://" + getHost() + "/settings";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[a-f0-9\\-]{32,36}")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}