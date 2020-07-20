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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jd.http.Request;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
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
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "downster.net" }, urls = { "https?://downster\\.net/api/download/get/\\d+/.+" })
public class DownsterNet extends antiDDoSForHost {

    public DownsterNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://downster.net");
    }

    @Override
    public String getAGBLink() {
        return "https://downster.net/legal";
    }

    private static final String          API_BASE                      = "https://downster.net/api";
    private static MultiHosterManagement mhm                           = new MultiHosterManagement("downster.net");
    private static final String          DLLINK_PROP_NAME              = "downsterdllink";
    private static final String          DLLINK_EXPIRE_PROP_NAME       = "downsterdllinkexpiration";
    private static final String          NOCHUNKS                      = "NOCHUNKS";
    private static final String          MAX_RETRIES_DL_ERROR_PROPERTY = "MAX_RETRIES_DL_ERROR";
    private static final int             DEFAULT_MAX_RETRIES_DL_ERROR  = 50;
    private String                       userFlowId                    = "";
    private String                       dllink                        = null;

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setConnectTimeout(60 * 1000); // application default 20 seconds
            // prepBr.setReadTimeout(60 * 1000); // application default is already 60 seconds
            prepBr.setHeader("Content-Type", "application/json");
            prepBr.addAllowedResponseCodes(new int[] { 401, 403, 412, 422, 503, 512 });
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
            prepBr.getHeaders().put("X-Jdl-Version", "" + getVersion());
        }
        return prepBr;
    }

    @Override
    protected void sendRequest(Browser ibr, Request request) throws Exception {
        ibr.getHeaders().put("X-Flow-ID", "JDL_" + userFlowId + "_" + randomFlowId());
        super.sendRequest(ibr, request);
    }

    private void loadUserFlowId(final Account account) {
        userFlowId = account.getStringProperty("flowId", null);
        if (userFlowId == null) {
            userFlowId = randomFlowId();
            account.setProperty("flowId", userFlowId);
        }
    }

    private String randomFlowId() {
        String flowId = "";
        for (int i = 0; i < 2; i++) {
            String hex = Integer.toHexString((int) (Math.random() * 255));
            flowId += hex.length() > 1 ? hex : "0" + hex;
        }
        return flowId;
    }

    private String login(final Account account) throws Exception {
        synchronized (account) {
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
            }
            loadUserFlowId(account);
            getPage(API_BASE + "/user/info");
            if ("true".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
                // refresh values
                account.saveCookies(br.getCookies(getHost()), "");
                return null;
            }
            if (cookies != null) {
                account.clearCookies("");
            }
            String json = null;
            json = PluginJSonUtils.ammendJson(json, "email", account.getUser());
            json = PluginJSonUtils.ammendJson(json, "password", account.getPass());
            postPage(API_BASE + "/user/authenticate", json);
            if ("true".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
                account.saveCookies(br.getCookies(getHost()), "");
                return null;
            }
            return PluginJSonUtils.getJsonValue(br, "error");
        }
    }

    private class Hoster {

        public String  name;
        public Long    limit;
        public Long    used;
        public Integer percentage;

        public Hoster() {
        }
    }

    private List<Hoster> getHosters(final Account account) throws Exception {
        getPage(API_BASE + "/download/usage");
        if ("false".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCould not get hoster list: " + PluginJSonUtils.getJsonValue(br, "error"));
        }
        final List<Hoster> hosters = new ArrayList<Hoster>();
        final String data = PluginJSonUtils.getJsonArray(br, "data");
        for (String hosterJson : PluginJSonUtils.getJsonResultsFromArray(data)) {
            final Hoster hoster = new Hoster();
            hoster.name = PluginJSonUtils.getJsonValue(hosterJson, "hoster");
            hoster.limit = Long.parseLong(PluginJSonUtils.getJsonValue(hosterJson, "limit"));
            hoster.used = Long.parseLong(PluginJSonUtils.getJsonValue(hosterJson, "used"));
            hoster.percentage = Integer.parseInt(PluginJSonUtils.getJsonValue(hosterJson, "percentage"));
            hosters.add(hoster);
        }
        return hosters;
    }

    private void accountInvalid() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if ("".equals(account.getUser()) || "".equals(account.getPass())) {
            /* Server returns 401 if you send empty fields (logindata) */
            accountInvalid();
        }
        final AccountInfo ac = new AccountInfo();
        String loginError = login(account);
        if (loginError != null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + loginError);
        }
        final String premiumUntil = PluginJSonUtils.getJsonValue(br, "premiumUntil");
        final Long premiumUntilTs = TimeFormatter.getMilliSeconds(premiumUntil, "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
        if (premiumUntilTs > 0) {
            // premiumUntil is reported in seconds but we needs milliseconds
            ac.setValidUntil(premiumUntilTs, br);
            ac.setUnlimitedTraffic();
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
            ac.setExpired(true);
        }
        final List<String> hosters = new ArrayList<String>();
        for (final Hoster hoster : getHosters(account)) {
            hosters.add(hoster.name);
        }
        ac.setMultiHostSupport(this, hosters);
        return ac;
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        String loginError = login(account);
        if (loginError != null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + loginError);
        }
        dllink = checkDirectLink(link);
        if (dllink == null) {
            String downloadlink = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            getPage(API_BASE + "/download/get?url=" + downloadlink);
            /* Either server error or the host is broken (we have to find out by retrying) */
            int status = br.getHttpConnection().getResponseCode();
            String error = PluginJSonUtils.getJsonValue(br, "error");
            if (status == 510 || status == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, error);
            } else if (status == 422) { // Limit reached
                mhm.putError(account, link, 30 * 60 * 1000l, error);
            } else if (status >= 500 || "false".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
                mhm.putError(account, link, 60 * 1000l, error);
            }
            dllink = PluginJSonUtils.getJsonValue(br, "downloadUrl");
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "No download link could be generated", 20, 5 * 60 * 1000l);
            }

            final String expiresAt = PluginJSonUtils.getJsonValue(br, "expires");
            final Long expiresAtTs = TimeFormatter.getMilliSeconds(expiresAt, "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            // Direct download links can be used for up to 30 minutes
            link.setProperty(DLLINK_PROP_NAME, dllink);
            link.setProperty(DLLINK_EXPIRE_PROP_NAME, expiresAtTs);
        }
        handleDl(link, account);
    }

    private void handleDl(final DownloadLink link, final Account account) throws Exception {
        int chunks = 0;
        if (link.getBooleanProperty(DownsterNet.NOCHUNKS, false)) {
            chunks = 1;
        }
        // don't think we need to wrap this in try/catch the core should handle this already.
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, chunks);
        } catch (final SocketTimeoutException e) {
            throw new PluginException(LinkStatus.ERROR_RETRY, e.getMessage());
        }
        int statusCode = dl.getConnection().getResponseCode();
        if (statusCode >= 400) {
            // Read response body to get the error message
            dl.getConnection().getRequest().read(false);
            switch (statusCode) {
                case 429: // To many parallel requests
                    mhm.putError(account, link, 60 * 1000l, br.toString());
                case 422: // Limit reached
                    mhm.putError(account, link, 30 * 60 * 1000l, br.toString());
                case 400: // Bad request
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, br.toString(), 5 * 1000l);
            }
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            int maxRetriesOnDownloadError = getPluginConfig().getIntegerProperty(MAX_RETRIES_DL_ERROR_PROPERTY, DEFAULT_MAX_RETRIES_DL_ERROR);
            mhm.handleErrorGeneric(account, link, "unknowndlerror", maxRetriesOnDownloadError, 10 * 60 * 1000l);
        }
        try {
            // start the dl
            if (!dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(DownsterNet.NOCHUNKS, false) == false) {
                    link.setProperty(DownsterNet.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk error handling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(DownsterNet.NOCHUNKS, false) == false) {
                link.setProperty(DownsterNet.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private String checkDirectLink(final DownloadLink downloadLink) {
        final String dllink = downloadLink.getStringProperty(DLLINK_PROP_NAME);
        final Long expiresAt = downloadLink.getLongProperty(DLLINK_EXPIRE_PROP_NAME, 0);
        if (dllink != null) {
            if (expiresAt != 0 && expiresAt > System.currentTimeMillis()) {
                return dllink;
            }
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }
}