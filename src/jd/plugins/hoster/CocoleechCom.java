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

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cocoleech.com" }, urls = { "" })
public class CocoleechCom extends PluginForHost {
    /* 2024-06-14: Alternative domain: cocodebrid.com */
    private static final String          API_ENDPOINT       = "https://members.cocoleech.com/auth/api";
    private static final String          PROPERTY_DIRECTURL = "cocoleechcom_directlink";
    private static final String          PROPERTY_MAXCHUNKS = "cocoleechcom_maxchunks";
    private static MultiHosterManagement mhm                = new MultiHosterManagement("cocoleech.com");

    @SuppressWarnings("deprecation")
    public CocoleechCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://members.cocoleech.com/");
        this.setStartIntervall(3000l);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    @Override
    public String getAGBLink() {
        return "https://members.cocoleech.com/terms";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        /* Last updated: 2017-02-08 according to admin request. */
        final int maxChunks = link.getIntegerProperty(PROPERTY_MAXCHUNKS, -4);
        return maxChunks;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
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
    /** This should never get called. */
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    /** This should never get called. */
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        final String dllink;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            /* Request creation of downloadlink */
            this.br.getPage(API_ENDPOINT + "?key=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            handleAPIErrors(this.br, account, link);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Object chunksO = entries.get("chunks");
            if (chunksO != null) {
                final String maxchunksStr = chunksO.toString();
                if (!StringUtils.isEmpty(maxchunksStr) && maxchunksStr.matches("^\\d+$")) {
                    final int maxChunks = -Integer.parseInt(maxchunksStr);
                    link.setProperty(PROPERTY_MAXCHUNKS, maxChunks);
                }
            }
            dllink = (String) entries.get("download");
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50, 5 * 60 * 1000l);
            }
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
            dl.setFilenameFix(true);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getContentType().contains("json")) {
                    handleAPIErrors(this.br, account, link);
                }
                mhm.handleErrorGeneric(account, link, "Unknown download error", 50, 5 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(PROPERTY_DIRECTURL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        this.dl.startDownload();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final Map<String, Object> entries = login(account);
        /*
         * 2021-11-29: Users enter API key only from now on --> Try to find username in API answer and set it so accounts in JD still have
         * unique username strings!
         */
        final String username = (String) entries.get("username");
        if (!StringUtils.isEmpty(username)) {
            account.setUser(username);
        }
        final String accounttype = (String) entries.get("type");
        final String trafficleft = (String) entries.get("traffic_left");
        final String validuntil = (String) entries.get("expire_date");
        long timestampValiduntil = 0;
        if (validuntil != null) {
            timestampValiduntil = TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        final AccountInfo ai = new AccountInfo();
        if ("premium".equalsIgnoreCase(accounttype)) {
            ai.setValidUntil(timestampValiduntil, br);
            account.setType(AccountType.PREMIUM);
            account.setConcurrentUsePossible(true);
            /*
             * 2017-02-08: Accounts do usually not have general traffic limits - however there are individual host traffic limits see
             * mainpage (when logged in) --> Right side "Daily Limit(s)"
             */
            if (StringUtils.equalsIgnoreCase(trafficleft, "unlimited")) {
                ai.setUnlimitedTraffic();
            } else {
                ai.setTrafficLeft(Long.parseLong(trafficleft));
            }
        } else {
            account.setType(AccountType.FREE);
            /*
             * 2016-05-05: According to admin, free accounts cannot download anything.
             */
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(1);
            ai.setTrafficLeft(0);
        }
        /* Overwrite previously set status in case an account package-name is available */
        final String accountPackage = (String) entries.get("package"); // E.g. "1 Month Premium" or "No Package" for free accounts
        if (!StringUtils.isEmpty(accountPackage)) {
            ai.setStatus(accountPackage);
        }
        br.getPage(API_ENDPOINT + "/hosts-status");
        final Map<String, Object> hoststatusmap = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final ArrayList<String> supportedhostslist = new ArrayList();
        final List<Map<String, Object>> hosters = (List<Map<String, Object>>) hoststatusmap.get("result");
        for (final Map<String, Object> hostinfo : hosters) {
            final String host = (String) hostinfo.get("host");
            final String status = (String) hostinfo.get("status");
            if (StringUtils.isEmpty(host)) {
                /* Skip invalid items */
                continue;
            }
            if ("online".equalsIgnoreCase(status)) {
                supportedhostslist.add(host);
            } else {
                logger.info("Not adding currently serverside deactivated host: " + host);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private Map<String, Object> login(final Account account) throws Exception {
        synchronized (account) {
            account.setPass(correctPassword(account.getPass()));
            if (!isAPIKey(account.getPass())) {
                throw new AccountInvalidException("Invalid API key format");
            }
            br.getPage(API_ENDPOINT + "/info?key=" + Encoding.urlEncode(account.getPass()));
            /* No error here = account is valid. */
            return handleAPIErrors(this.br, account, null);
        }
    }

    private Map<String, Object> handleAPIErrors(final Browser br, final Account account, final DownloadLink link) throws Exception {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException ignore) {
            /* This should never happen. */
            final String msg = "Invalid API response";
            final long wait = 1 * 60 * 1000;
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, wait);
            } else {
                throw new AccountUnavailableException(msg, wait);
            }
        }
        /**
         * 2021-09-10: "status" field which contains a number looks to always be "100" regardless of the error message e.g. </br>
         * {"status":"100","message":"Incorrect log-in or password."} </br>
         * {"status":"100","message":"Your IP is blocked for today. Please contact support."} </br>
         * {"status":"100","message":"Link is dead."}
         */
        final String statusmsg = (String) entries.get("message");
        if (StringUtils.isEmpty(statusmsg)) {
            /* No error */
            return entries;
        }
        if (statusmsg.equalsIgnoreCase("Incorrect log-in or password.")) {
            throw new AccountInvalidException(statusmsg);
        } else if (statusmsg.equalsIgnoreCase("Incorrect API key.")) {
            String errormsg = statusmsg + "\r\nFind your API Key here: " + getAPILoginHelpURL().replaceFirst("(?i)^https?://", "");
            errormsg += "\r\nIf you're using myjdownloader, enter your API Key into both the username and password fields.";
            throw new AccountInvalidException(errormsg);
        } else if (statusmsg.equalsIgnoreCase("Premium membership expired.")) {
            throw new AccountUnavailableException(statusmsg, 5 * 60 * 1000l);
        } else if (statusmsg.equalsIgnoreCase("Your IP is blocked for today. Please contact support.")) {
            /* Put all account temp. unavailable errors here. */
            throw new AccountUnavailableException(statusmsg, 5 * 60 * 1000l);
        } else if (statusmsg.equalsIgnoreCase("Link is dead.")) {
            /* File is offline according to multihoster-API. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            /* Unknown error or link based error */
            if (link == null) {
                throw new AccountUnavailableException(statusmsg, 3 * 60 * 1000l);
            } else {
                mhm.handleErrorGeneric(account, link, statusmsg, 50, 5 * 60 * 1000l);
            }
        }
        return entries;
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account != null) {
            return Integer.MAX_VALUE;
        } else {
            return 0;
        }
    }

    private static boolean isAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[a-f0-9]{24}")) {
            return true;
        } else {
            return false;
        }
    }

    private static String correctPassword(final String pw) {
        if (pw != null) {
            return pw.trim();
        } else {
            return null;
        }
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "https://members." + getHost() + "/settings";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[a-f0-9]{24}")) {
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