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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debriditalia.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOMasdfasdfsadfsdgfd32423" }, flags = { 2 })
public class DebridItaliaCom extends antiDDoSForHost {

    public DebridItaliaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.debriditalia.com/premium.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.debriditalia.com/premium.php";
    }

    private static final String                            NICE_HOST                        = "debriditalia.com";
    private static final String                            NICE_HOSTproperty                = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NOCHUNKS                         = "NOCHUNKS";
    private static final String                            MAX_RETRIES_UNAVAILABLE_PROPERTY = "MAX_RETRIES_UNAVAILABLE";
    private static final int                               DEFAULT_MAX_RETRIES_UNAVAILABLE  = 30;
    private static final String                            MAX_RETRIES_DL_ERROR_PROPERTY    = "MAX_RETRIES_DL_ERROR";
    private static final int                               DEFAULT_MAX_RETRIES_DL_ERROR     = 50;

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap               = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                          = null;
    private DownloadLink                                   currDownloadLink                 = null;

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setConnectTimeout(60 * 1000);
            prepBr.setReadTimeout(60 * 1000);
            /* 401 can happen when user enters invalid logindata */
            prepBr.addAllowedResponseCodes(401);
            prepBr.getHeaders().put("User-Agent", "JDownloader");
        }
        return prepBr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        if (account.getUser().equals("") || account.getPass().equals("")) {
            /* Server returns 401 if you send empty fields (logindata) */
            accountInvalid();
        }
        final AccountInfo ac = new AccountInfo();
        ac.setProperty("multiHostSupport", Property.NULL);
        ac.setUnlimitedTraffic();
        if (!loginAPI(account)) {
            if (br.containsHTML("<status>expired</status>")) {
                ac.setStatus("Account is expired!");
                ac.setExpired(true);
                account.setValid(false);
                return ac;
            }
            accountInvalid();
        }
        final String expire = br.getRegex("<expiration>(\\d+)</expiration>").getMatch(0);
        if (expire == null) {
            ac.setStatus("Account is invalid. Invalid or unsupported accounttype!");
            accountInvalid();
        }
        ac.setValidUntil(Long.parseLong(expire) * 1000l);

        // now let's get a list of all supported hosts:
        getPage("https://debriditalia.com/api.php?hosts");
        final String[] hosts = br.getRegex("\"([^<>\"]*?)\"").getColumn(0);
        final List<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        supportedHosts.add("filesmonster.com");
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setStatus("Premium account");
        return ac;
    }

    private void accountInvalid() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);

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
        // prevent ddos work around
        if (link.getBooleanProperty("hasFailed", false)) {
            final int hasFailedInt = link.getIntegerProperty("hasFailedWait", 60);
            // nullify old storeables
            link.setProperty("hasFailed", Property.NULL);
            link.setProperty("hasFailedWait", Property.NULL);
            sleep(hasFailedInt * 1001, link);
        }

        showMessage(link, "Generating link");

        // since no requests are done with this.br we need to manually set so checkdirectlink is correct
        prepBrowser(br, "https://debriditalia.com/");
        String dllink = checkDirectLink(link, "debriditaliadirectlink");
        if (dllink == null) {
            String host_downloadlink = link.getDownloadURL();
            /* Workaround for serverside debriditalia bug. */
            if ((link.getHost().equals("share-online.biz") || link.getHost().equals("depfile.com")) && host_downloadlink.startsWith("https://")) {
                host_downloadlink = host_downloadlink.replace("https://", "http://");
            }
            final String encodedLink = Encoding.urlEncode(host_downloadlink);
            getPage("https://debriditalia.com/api.php?generate=on&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + encodePassword(account.getPass()) + "&link=" + encodedLink);
            /* Either server error or the host is broken (we have to find out by retrying) */
            if (br.containsHTML("ERROR: not_available")) {
                int timesFailed = link.getIntegerProperty("timesfaileddebriditalia_not_available", 0);
                int maxRetriesIfUnavailable = getPluginConfig().getIntegerProperty(MAX_RETRIES_UNAVAILABLE_PROPERTY, DEFAULT_MAX_RETRIES_UNAVAILABLE);
                if (timesFailed <= maxRetriesIfUnavailable) {
                    timesFailed++;
                    logger.fine("Hoster unavailable! Retry attempt " + timesFailed + " of " + maxRetriesIfUnavailable);
                    link.setProperty("timesfaileddebriditalia_not_available", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    logger.fine("Hoster unavailable! Max. retry attempts reached!");
                    link.setProperty("timesfaileddebriditalia_not_available", Property.NULL);
                    tempUnavailableHoster(15 * 60 * 1000l);
                }

            } else if (br.containsHTML("ERROR: not_supported")) {
                logger.info("Current host is not supported");
                tempUnavailableHoster(3 * 60 * 60 * 1000l);
            }
            dllink = br.getRegex("(https?://(\\w+\\.)?debriditalia\\.com/dl/.+)").getMatch(0);
            if (dllink == null) {
                logger.info("debriditalia.com: Unknown error - final downloadlink is missing");
                int timesFailed = link.getIntegerProperty("timesfaileddebriditalia_unknownerror_dllink_missing", 0);
                if (timesFailed <= 20) {
                    timesFailed++;
                    link.setProperty("timesfaileddebriditalia_unknownerror_dllink_missing", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    logger.info("debriditalia.com: Unknown error - final downloadlink is missing -> Disabling current host");
                    link.setProperty("timesfaileddebriditalia_unknownerror_dllink_missing", Property.NULL);
                    tempUnavailableHoster(60 * 60 * 1000l);
                }
            }
        }

        int chunks = 0;
        if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false)) {
            chunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            int maxRetriesOnDownloadError = getPluginConfig().getIntegerProperty(MAX_RETRIES_DL_ERROR_PROPERTY, DEFAULT_MAX_RETRIES_DL_ERROR);
            if (br.containsHTML("No htmlCode read")) {
                logger.info("debriditalia.com: Unknown download error");
                handleErrorRetries("unknowndlerror", maxRetriesOnDownloadError, 5 * 60 * 1000l);
            }
            logger.info("debriditalia.com: Unknown download error 2" + br.toString());
            handleErrorRetries("unknowndlerror2", maxRetriesOnDownloadError, 1 * 60 * 60 * 1000l);
        }
        // Directlinks can be used for up to 2 days
        link.setProperty("debriditaliadirectlink", dllink);

        try {
            // start the dl
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                    link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

            throw e;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private boolean loginAPI(final Account acc) throws Exception {
        getPage("https://debriditalia.com/api.php?check=on&u=" + Encoding.urlEncode(acc.getUser()) + "&p=" + encodePassword(acc.getPass()));
        if (!br.containsHTML("<status>valid</status>") || br.getHttpConnection().getResponseCode() == 401) {
            return false;
        }
        return true;
    }

    /** Workaround(s) for special chars issues with login passwords. */
    private String encodePassword(String password) {
        if (!password.contains("%")) {
            password = Encoding.urlEncode(password);
        }
        return password;
    }

    private void tempUnavailableHoster(long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
        return true;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @SuppressWarnings("deprecation")
    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (downloadLink.getDownloadURL().contains("clz.to/")) {
            dllink = downloadLink.getDownloadURL();
        }
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
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
    private void handleErrorRetries(final String error, final int maxRetries, final long timeout) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            logger.fine("Unknown download error! Retry attempt " + timesFailed + " of " + maxRetries);
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            // prevent ddos
            this.currDownloadLink.setProperty("hasFailed", true);
            this.currDownloadLink.setProperty("hasFailedWait", 60);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            logger.fine("Unknown download error! Max. retry attempts reached!");
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(1 * 60 * 60 * 1000l);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    protected void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), MAX_RETRIES_UNAVAILABLE_PROPERTY, JDL.L("plugins.hoster.debriditaliacom.maxRetriesUnavailable", "Maximum Retries If Unavailable")).setDefaultValue(DEFAULT_MAX_RETRIES_UNAVAILABLE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), MAX_RETRIES_DL_ERROR_PROPERTY, JDL.L("plugins.hoster.debriditaliacom.maxRetriesDlError", "Maximum Retries On Download Error")).setDefaultValue(DEFAULT_MAX_RETRIES_DL_ERROR));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}