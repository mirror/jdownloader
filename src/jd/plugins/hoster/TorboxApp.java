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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "torbox.app" }, urls = { "" })
public class TorboxApp extends PluginForHost {
    private final String                 API_BASE                                                 = "https://api.torbox.app/v1/api";
    private static MultiHosterManagement mhm                                                      = new MultiHosterManagement("torbox.app");
    private final String                 PROPERTY_SERVERSIDE_FILE_ID                              = "file_id";
    private final String                 PROPERTY_SERVERSIDE_HASH                                 = "hash";
    private final String                 PROPERTY_ACCOUNT_NOTIFICATIONS_DISPLAYED_UNTIL_TIMESTAMP = "notifications_displayed_until_timestamp";

    @SuppressWarnings("deprecation")
    public TorboxApp(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/pricing");
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
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
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

    private String getMultihosterHash(final DownloadLink link) {
        return link.getStringProperty(getPropertyKey(PROPERTY_SERVERSIDE_HASH));
    }

    private void setMultihosterFileID(final DownloadLink link, final String file_id) {
        link.setProperty(getPropertyKey(PROPERTY_SERVERSIDE_FILE_ID), file_id);
    }

    private void setMultihosterHash(final DownloadLink link, final String hash) {
        link.setProperty(getPropertyKey(PROPERTY_SERVERSIDE_HASH), hash);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        final String directlinkproperty = this.getPropertyKey("directlink");
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        final String dllink;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            mhm.runCheck(account, link);
            this.login(account, false);
            final String stored_file_id = getMultihosterFileID(link);
            String file_id = null;
            String hash = getMultihosterHash(link);
            /* TODO: Detect old fileIDs and/or only re-use them for X time or never re-use them. */
            boolean isNewFileID = false;
            final boolean allowReUseStoredFileID = true;
            if (stored_file_id != null && allowReUseStoredFileID) {
                logger.info("Re-using stored internal fileID: " + stored_file_id);
                file_id = stored_file_id;
            } else {
                logger.info("Creating internal file_id");
                final Request req_createwebdownload = br.createPostRequest(API_BASE + "/webdl/createwebdownload", "link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
                final Map<String, Object> entries = (Map<String, Object>) this.callAPI(req_createwebdownload, account, link);
                // hash, auth_id
                file_id = entries.get("webdownload_id").toString();
                hash = entries.get("hash").toString();
                /* Save this ID to re-use on next try. */
                this.setMultihosterFileID(link, file_id);
                this.setMultihosterHash(link, hash);
                if (StringUtils.equals(file_id, stored_file_id)) {
                    logger.info("createwebdownload has returned same internal fileID which we already know: " + stored_file_id);
                } else {
                    isNewFileID = true;
                }
            }
            /*
             * 2024-06-11: Looks like this doesn't work or it needs some time until cached items get listed as cached so at this moment
             * let's not do this and try downloading straight away.
             */
            final boolean doCachecheck = false;
            if (doCachecheck) {
                logger.info("Checking downloadability of internal file_id: " + file_id + " | hash: " + hash);
                final UrlQuery query_checkcached = new UrlQuery();
                query_checkcached.add("hash", hash);
                query_checkcached.add("format", "list");
                final Request req_checkcached = br.createGetRequest(API_BASE + "/webdl/checkcached?" + query_checkcached.toString());
                final Map<String, Object> resp_checkcached = (Map<String, Object>) this.callAPI(req_checkcached, account, link);
                final Map<String, Object> cachemap = (Map<String, Object>) resp_checkcached.get(hash);
                if (cachemap == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverside download is not yet ready", 30 * 1000);
                }
            }
            logger.info("Trying to init download for internal file_id: " + file_id + " | hash: " + hash);
            final UrlQuery query_requestdl = new UrlQuery();
            query_requestdl.add("token", this.getApikey(account));
            query_requestdl.add("web_id", file_id);
            query_requestdl.add("zip", "false");
            final Request req_requestdl = br.createGetRequest(API_BASE + "/webdl/requestdl?" + query_requestdl.toString());
            dllink = this.callAPI(req_requestdl, account, link).toString();
            /* Store directurl so we can re-use it next time. */
            link.setProperty(directlinkproperty, dllink);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadlink did not lead to file content", 50, 5 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> user = login(account, true);
        /**
         * In GUI, used only needs to enter API key so we'll set the username for him here. </br>
         * This is also important to be able to keep the user from adding the same account multiple times.
         */
        account.setUser(user.get("email").toString());
        final String created_at = user.get("created_at").toString();
        final String premium_expires_at = (String) user.get("premium_expires_at");
        long premiumExpireTimestamp = -1;
        if (premium_expires_at != null) {
            premiumExpireTimestamp = TimeFormatter.getMilliSeconds(premium_expires_at, "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH);
        }
        ai.setCreateTime(TimeFormatter.getMilliSeconds(created_at, "yyyy-MM-dd'T'HH:mm:ss.SSSSX", Locale.ENGLISH));
        if (premiumExpireTimestamp > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(premiumExpireTimestamp, br);
        } else {
            account.setType(AccountType.FREE);
        }
        final String subscribedStr;
        if ((Boolean) user.get("is_subscribed")) {
            subscribedStr = "Yes";
        } else {
            subscribedStr = "No";
        }
        ai.setStatus("[!BETA PLUGIN!] " + account.getType().getLabel() + " | Subscribed: " + subscribedStr + " | Dl so far: " + SizeFormatter.formatBytes(((Number) user.get("total_bytes_downloaded")).longValue()));
        /* Obtain list of supported hosts */
        final Request req_hosters = br.createGetRequest(API_BASE + "/webdl/hosters");
        final List<Map<String, Object>> hosterlist = (List<Map<String, Object>>) this.callAPI(req_hosters, account, null);
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
        final boolean enableNotifications = true;
        if (enableNotifications) {
            try {
                final long timestampNotificationsDisplayed = account.getLongProperty(PROPERTY_ACCOUNT_NOTIFICATIONS_DISPLAYED_UNTIL_TIMESTAMP, 0);
                final Request req_notifications = br.createGetRequest(API_BASE + "/notifications/mynotifications");
                final List<Map<String, Object>> notifications = (List<Map<String, Object>>) this.callAPI(req_notifications, account, null);
                int counterDisplayed = 0;
                for (final Map<String, Object> notification : notifications) {
                    final long notification_created_at = TimeFormatter.getMilliSeconds(notification.get("created_at").toString(), "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH);
                    if (notification_created_at < timestampNotificationsDisplayed) {
                        /* Assume that this notification has already been displayed in the past. */
                        continue;
                    }
                    logger.info("Displaying notification with ID: " + notification.get("id"));
                    displayBubblenotifyMessage(notification.get("title").toString(), notification.get("message").toString());
                    if (timestampNotificationsDisplayed == 0 && counterDisplayed >= 5) {
                        logger.info("First time we are displaying notifications for this account. Let's not bombard the user with all past notifications he has.");
                        break;
                    }
                }
                logger.info("Total number of notifications: " + notifications.size() + " | Displayed this run: " + counterDisplayed);
            } catch (final Exception e) {
                logger.log(e);
                logger.warning("Exception happened in notification handling");
            } finally {
                /* Save this timestamp so we are able to know to which time we've already displayed notifications. */
                account.setProperty(PROPERTY_ACCOUNT_NOTIFICATIONS_DISPLAYED_UNTIL_TIMESTAMP, System.currentTimeMillis());
            }
        }
        return ai;
    }

    private Map<String, Object> login(final Account account, final boolean validateLogins) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            setLoginHeaders(br, account);
            if (!validateLogins) {
                /* Do not validate logins */
                return null;
            }
            final Request loginreq = br.createGetRequest(API_BASE + "/user/me");
            final Map<String, Object> resp = (Map<String, Object>) this.callAPI(loginreq, account, null);
            return resp;
        }
    }

    private void setLoginHeaders(final Object obj, final Account account) {
        if (obj instanceof Request) {
            ((Request) obj).getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + getApikey(account));
        } else {
            ((Browser) obj).getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + getApikey(account));
        }
    }

    /* API docs: https://api-docs.torbox.app/ */
    private Object callAPI(final Request req, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        // setLoginHeaders(req, account);
        req.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + getApikey(account));
        br.getPage(req);
        return checkErrors(account, link);
    }

    private String getApikey(final Account account) {
        return correctPassword(account.getPass());
    }

    private Object checkErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        try {
            final Object jsonO = restoreFromString(br.toString(), TypeRef.OBJECT);
            if (jsonO == null || !(jsonO instanceof Map)) {
                return jsonO;
            }
            handleErrorMap(account, link, (Map<String, Object>) jsonO);
            if (jsonO instanceof Map) {
                final Map<String, Object> thismap = (Map<String, Object>) jsonO;
                return thismap.get("data");
            }
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
        final Boolean successO = (Boolean) entries.get("success");
        if (Boolean.TRUE.equals(successO)) {
            /* No error */
            return;
        }
        final String errormsg = entries.get("detail").toString();
        if (link != null) {
            /* E.g. {"success":false,"detail":"Failed to request web download. Please try again later.","data":null} */
            mhm.handleErrorGeneric(account, link, errormsg, 50, 1 * 60 * 1000l);
        } else {
            throw new AccountInvalidException(errormsg);
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
        } else if (correctPassword(str).matches("[a-f0-9]{32}")) {
            return true;
        } else {
            return false;
        }
    }

    private void displayBubblenotifyMessage(final String title, final String msg) {
        BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
            @Override
            public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                return new BasicNotify("TorBox: " + title, msg, new AbstractIcon(IconKey.ICON_INFO, 32));
            }
        });
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}