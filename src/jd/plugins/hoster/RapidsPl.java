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
import java.util.LinkedHashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapids.pl" }, urls = { "" })
public class RapidsPl extends PluginForHost {
    /* API documentation: https://new.rapids.pl/api */
    private static final String                  API_BASE             = "https://api.rapids.pl/api";
    /* 2020-03-24: Static implementation as key is nowhere to be found via API request. */
    private static LinkedHashMap<String, Object> individualHostLimits = new LinkedHashMap<String, Object>();
    /* Connection limits: 2020-03-24: According to API docs "Max Connections: 15 per user/minute" --> WTF --> Set it to unlimited for now */
    private static final int                     defaultMAXDOWNLOADS  = -1;
    private static final int                     defaultMAXCHUNKS     = 0;
    private static final boolean                 defaultRESUME        = false;
    private static final String                  PROPERTY_logintoken  = "token";
    private static final String                  PROPERTY_directlink  = "directlink";

    @SuppressWarnings("deprecation")
    public RapidsPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rapids.pl/");
    }

    @Override
    public String getAGBLink() {
        return "https://rapids.pl/pomoc/regulamin";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        /* Set headers according to API docs */
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("x-lang", "en");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 401, 423 });
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* Without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never get called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /* Stolen from LinkSnappyCom/CboxeraCom */
    private String cacheDLChecker(final DownloadLink link) throws Exception {
        final PluginProgress waitProgress = new PluginProgress(0, 100, null) {
            protected long lastCurrent    = -1;
            protected long lastTotal      = -1;
            protected long startTimeStamp = -1;

            @Override
            public PluginTaskID getID() {
                return PluginTaskID.WAIT;
            }

            @Override
            public String getMessage(Object requestor) {
                if (requestor instanceof ETAColumn) {
                    final long eta = getETA();
                    if (eta >= 0) {
                        return TimeFormatter.formatMilliSeconds(eta, 0);
                    }
                    return "";
                }
                return "Preparing your delayed file";
            }

            @Override
            public void updateValues(long current, long total) {
                super.updateValues(current, total);
                if (startTimeStamp == -1 || lastTotal == -1 || lastTotal != total || lastCurrent == -1 || lastCurrent > current) {
                    lastTotal = total;
                    lastCurrent = current;
                    startTimeStamp = System.currentTimeMillis();
                    // this.setETA(-1);
                    return;
                }
                long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
                if (currentTimeDifference <= 0) {
                    return;
                }
                long speed = (current * 10000) / currentTimeDifference;
                if (speed == 0) {
                    return;
                }
                long eta = ((total - current) * 10000) / speed;
                this.setETA(eta);
            }
        };
        waitProgress.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
        waitProgress.setProgressSource(this);
        int lastProgress = -1;
        try {
            final int maxWaitSeconds = 300;
            /* 2020-04-16: API docs say checking every 15 seconds is recommended, we always check after 5 seconds. */
            final int waitSecondsPerLoop = 5;
            int waitSecondsLeft = maxWaitSeconds;
            /* 0 = initializing (return code on first request), 1 = initializing2 (???), 2 = pending, 3 = done, 4 = error */
            int delayedStatus = 0;
            Integer currentProgress = 0;
            String finalDownloadurl = null;
            do {
                logger.info(String.format("Waiting for file to get loaded onto server - seconds left %d / %d", waitSecondsLeft, maxWaitSeconds));
                link.addPluginProgress(waitProgress);
                waitProgress.updateValues(currentProgress.intValue(), 100);
                for (int sleepRound = 0; sleepRound < waitSecondsPerLoop; sleepRound++) {
                    if (isAbort()) {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    } else {
                        Thread.sleep(1000);
                    }
                }
                if (currentProgress.intValue() != lastProgress) {
                    // lastProgressChange = System.currentTimeMillis();
                    lastProgress = currentProgress.intValue();
                }
                br.postPageRaw(API_BASE + "/files/check-and-add", String.format("{\"file\":\"%s\"}", link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
                try {
                    /* We have to use the parser here because json contains two 'status' objects ;) */
                    Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    final ArrayList<Object> data = (ArrayList<Object>) entries.get("data");
                    if (data.size() > 1) {
                        /* This should never happen */
                        logger.warning("WTF data array contains more than one item");
                    }
                    entries = (Map<String, Object>) data.get(0);
                    entries = (Map<String, Object>) entries.get("file");
                    Map<String, Object> statusmap = (Map<String, Object>) entries.get("status");
                    delayedStatus = (int) JavaScriptEngineFactory.toLong(statusmap.get("code"), 0);
                    finalDownloadurl = (String) entries.get("download");
                    final int tmpCurrentProgress = (int) JavaScriptEngineFactory.toLong(statusmap.get("percentages"), 0);
                    if (tmpCurrentProgress > currentProgress) {
                        /* Do not allow the progress to "go back" - rather leave it stuck! */
                        currentProgress = tmpCurrentProgress;
                    }
                } catch (final Throwable e) {
                    logger.info("Error parsing json response");
                    break;
                }
                waitSecondsLeft -= waitSecondsPerLoop;
            } while (delayedStatus != 3 && delayedStatus != 4);
            /* 2020-04-16: Downloadurl may always be available! Be sure to only return it on correct status!! It will not work otherwise! */
            if (delayedStatus == 3) {
                logger.info("Downloadurl should be available");
                return finalDownloadurl;
            } else {
                /* Either delayedStatus == 4 --> Error or timeout reached */
                if (delayedStatus == 4) {
                    logger.info("Error happened during serverside download");
                } else if (waitSecondsLeft <= 0) {
                    logger.info(String.format("Timeout happened during serverside download - exceeded %d seconds", maxWaitSeconds));
                } else {
                    logger.info("Unknown error happened during serverside download");
                }
                return null;
            }
        } finally {
            link.removePluginProgress(waitProgress);
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        String dllink = checkDirectLink(link, this.getHost() + PROPERTY_directlink);
        br.setFollowRedirects(true);
        if (dllink == null) {
            this.loginAPI(account, false);
            /*
             * 2020-04-16: We'll use the cacheChecker for all URLs as we do not know which ones provide direct downloads and which ones
             * don't (well we could know this before but the current method is safer as the status of such hosts could change at any time).
             */
            dllink = cacheDLChecker(link);
            if (StringUtils.isEmpty(dllink)) {
                handleErrors(this.br, account, link);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "dllinknull", 5 * 60 * 1000);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        link.setProperty(this.getHost() + PROPERTY_directlink, dl.getConnection().getURL().toString());
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors(this.br, account, link);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error", 5 * 60 * 1000);
        }
        this.dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                /* 2020-04-16: HeadRequest is not possible! */
                con = br2.openGetConnection(dllink);
                if (con.isContentDisposition()) {
                    return dllink;
                } else {
                    return null;
                }
            } catch (final Exception e) {
                logger.log(e);
                dllink = null;
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
        loginAPI(account, true);
        if (br.getURL() == null || !br.getURL().contains("/users")) {
            br.getPage(API_BASE + "/users");
            this.handleErrors(this.br, account, null);
        }
        final String trafficleft = PluginJSonUtils.getJson(br, "transfer");
        if (trafficleft == null || !trafficleft.matches("\\d+")) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
        } else {
            /*
             * Basically all accounts are premium - they only have traffic based accounts so instead of expiring, they will be out of
             * traffic at some point.
             */
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            ai.setTrafficLeft(Long.parseLong(trafficleft));
        }
        br.getPage(API_BASE + "/services");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("data");
        for (final Object hostO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) hostO;
            final String main_domain_without_tld = (String) entries.get("name");
            final boolean is_active = ((Boolean) entries.get("is_active")).booleanValue();
            // final boolean download_by_direct = ((Boolean) entries.get("download_by_direct")).booleanValue();
            final ArrayList<String> domains = (ArrayList<String>) entries.get("domains");
            if (StringUtils.isEmpty(main_domain_without_tld)) {
                /* This should never happen */
                continue;
            }
            if (!is_active) {
                logger.info("Skipping host because: unsupported: " + main_domain_without_tld);
                continue;
            }
            for (final String domain_with_tld : domains) {
                supportedhostslist.add(domain_with_tld);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginAPI(final Account account, final boolean forceAuthCheck) throws IOException, PluginException, InterruptedException {
        prepBR(this.br);
        String token = account.getStringProperty(PROPERTY_logintoken);
        /* 2020-04-15: Token expires after max 14 days but can get invalid at any time --> Refresh every 45 minutes */
        final int token_refresh_minutes = 45;
        final boolean needs_token_refresh = System.currentTimeMillis() - account.getCookiesTimeStamp("") >= token_refresh_minutes * 60 * 1000l;
        if (token != null && needs_token_refresh) {
            logger.info(String.format("Token needs to be refreshed as it is older than %d minutes", token_refresh_minutes));
        } else if (token != null) {
            logger.info("Attempting token login");
            br.getHeaders().put("Authorization", "Bearer " + token);
            if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !forceAuthCheck) {
                /* We trust our token --> Do not check them */
                logger.info("Trust login token as it is not that old");
                return;
            }
            br.getPage(API_BASE + "/users");
            if (br.getHttpConnection().getResponseCode() == 200) {
                logger.info("Token login successful");
                /* We don't really need the cookies but the timestamp ;) */
                account.saveCookies(br.getCookies(br.getHost()), "");
                return;
            } else {
                logger.info("Token login failed");
            }
        }
        /* Clear previous headers & cookies */
        logger.info("Performing full login");
        br = this.prepBR(new Browser());
        final String postData = String.format("{\"username\": \"%s\",\"password\": \"%s\"}", account.getUser(), account.getPass());
        br.postPageRaw(API_BASE + "/auth/login", postData);
        token = PluginJSonUtils.getJson(br, "access_token");
        if (StringUtils.isEmpty(token)) {
            handleErrors(br, account, null);
            /* This should never happen - do not permanently disable accounts for unexpected login errors! */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unknown login failure", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        logger.info("Login successful");
        account.setProperty(PROPERTY_logintoken, token);
        /* We don't really need the cookies but the timestamp ;) */
        account.saveCookies(br.getCookies(br.getHost()), "");
        br.getHeaders().put("Authorization", "Bearer " + token);
    }

    private void handleErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        /** TODO: This is a bit glitchy */
        final int responsecode = br.getHttpConnection().getResponseCode();
        final int errorcode = getErrorcode(br);
        String errormsg = getErrormessage(this.br);
        if (StringUtils.isEmpty(errormsg)) {
            errormsg = "Unknown error";
        }
        if (responsecode == 401) {
            /* Login invalid */
            if (errormsg.equalsIgnoreCase("TOKEN_EXPIRED")) {
                /* Indicates that the account did work before --> Only temp. disable it - token should be auto-refreshed on next check! */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (responsecode == 403) {
            /* Account blocked or not yet activated */
            throw new AccountUnavailableException(errormsg, 1 * 60 * 1000l);
        } else if (responsecode == 423) {
            /* TODO: Add API login captcha handling if possible */
            /*
             * E.g. {"message":"Adres z kt\u00f3rego si\u0119 \u0142\u0105czysz zosta\u0142 zablokowany","data":{"captcha-public-key":
             * "6LdSiKUUAAAAALCIB4OPOc4eIc4JA8JRbKD-yIuW"}}
             */
            if (StringUtils.isEmpty(getErrormessage(this.br))) {
                errormsg = "Login captcha required";
            }
            throw new AccountUnavailableException(errormsg, 5 * 60 * 1000l);
        }
        if (link == null && errorcode != -1) {
            /* {"message":"A user with the specified credentials was not found","code":2038} */
            if (responsecode == 404 || errorcode == 2038) {
                /* Invalid logindata */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                /* Unknown account error */
                throw new AccountUnavailableException(errormsg, 3 * 60 * 1000l);
            }
        } else {
            /* Handle download errors ONLY */
            if (errorcode == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormsg, 3 * 60 * 1000l);
            }
        }
    }

    private String getErrormessage(final Browser br) {
        return PluginJSonUtils.getJson(br, "message");
    }

    private int getErrorcode(final Browser br) {
        final String errorcodeStr = PluginJSonUtils.getJson(br, "code");
        if (errorcodeStr != null && errorcodeStr.matches("\\d+")) {
            return Integer.parseInt(errorcodeStr);
        } else {
            /* No errorcode */
            return -1;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}