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
import java.util.LinkedHashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 36397 $", interfaceVersion = 3, names = { "fastfiles.pl" }, urls = { "https?://(?:www\\.)?fastfiles\\.pl/download/\\d+/[^/]+" })
public class FastfilesPl extends PluginForHost {
    private static final String                            API_BASE                     = "https://fastfiles.pl/api/apidownload.php";
    private static final String                            NICE_HOST                    = "fastfiles.pl";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            api_errortext_file_not_found = "File doesn't exists";
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private static final int                               defaultMAXDOWNLOADS          = -1;
    private static final int                               defaultMAXCHUNKS             = 0;
    private static final boolean                           defaultRESUME                = true;
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    @SuppressWarnings("deprecation")
    public FastfilesPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://fastfiles.pl/premium2.html");
    }

    @Override
    public String getAGBLink() {
        return "https://fastfiles.pl/regulamin.html";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setReadTimeout(1 * 60 * 1000);
        br.setConnectTimeout(1 * 60 * 1000);
        return br;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        this.getAPISafe(API_BASE + "?request=filecheck&url=" + Encoding.urlEncode(link.getDownloadURL()));
        final String error = this.getErrormessage(this.br);
        if (!StringUtils.isEmpty(error)) {
            /* Something WTF just happened */
            return AvailableStatus.UNCHECKABLE;
        }
        final String filename = PluginJSonUtils.getJsonValue(br, "filename");
        final String filesize = PluginJSonUtils.getJsonValue(br, "filesize");
        if (StringUtils.isEmpty(filename) || StringUtils.isEmpty(filesize)) {
            /* Something WTF just happened */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        this.setConstants(account, link);
        handleDL(account, link);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            br.getPage(API_BASE + "?request=getfile&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = PluginJSonUtils.getJsonValue(br, "downloadlink");
            if (StringUtils.isEmpty(dllink)) {
                final String error = this.getErrormessage(br);
                if (api_errortext_file_not_found.equalsIgnoreCase(error)) {
                    handleErrorRetries("bad_api_error_file_not_found", 10, 60 * 60 * 1000l);
                }
                logger.warning("Final downloadlink is null");
                handleKnownErrors(this.br);
                handleErrorRetries("dllinknull", 10, 60 * 60 * 1000l);
            }
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            handleKnownErrors(this.br);
            handleErrorRetries("unknowndlerror", 10, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        this.setConstants(account, link);
        login(false);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        this.currDownloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(disableTime);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        login(true);
        final String accounttype = PluginJSonUtils.getJsonValue(br, "typ");
        final String validuntil = PluginJSonUtils.getJsonValue(br, "expires");
        final String trafficleft = PluginJSonUtils.getJsonValue(br, "traffic");
        long timestamp_validuntil = 0;
        if (validuntil != null) {
            timestamp_validuntil = TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        if ("premium".equalsIgnoreCase(accounttype) && timestamp_validuntil > System.currentTimeMillis()) {
            ai.setValidUntil(timestamp_validuntil, this.br);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        }
        ai.setTrafficLeft(trafficleft);
        this.getAPISafe(API_BASE + "?request=hostlist&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<String> supportedhostslist = (ArrayList<String>) entries.get("supported_hosts");
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final boolean force) throws IOException, PluginException {
        this.getAPISafe(API_BASE + "?request=usercheck&username=" + Encoding.urlEncode(this.currAcc.getUser()) + "&password=" + Encoding.urlEncode(this.currAcc.getPass()));
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        } else if (this.currDownloadLink.getHost().equals(this.getHost())) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        this.br.getPage(accesslink);
        handleKnownErrors(this.br);
    }

    // private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
    // this.br.postPage(accesslink, postdata);
    // handleKnownErrors(this.br);
    // }
    private void handleKnownErrors(final Browser br) throws PluginException {
        final String error = getErrormessage(this.br);
        if (!StringUtils.isEmpty(error)) {
            if (error.equalsIgnoreCase(api_errortext_file_not_found)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (error.equalsIgnoreCase("Not authenticated")) {
                /* Login failed */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (error.equalsIgnoreCase("You have reached your bandwidth limit")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
    }

    private String getErrormessage(final Browser br) {
        return PluginJSonUtils.getJson(br, "error");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}