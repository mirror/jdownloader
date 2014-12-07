//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 26092 $", interfaceVersion = 3, names = { "myfastfile.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MyFastFileCom extends PluginForHost {

    // DEV NOTES
    // password is APIKey from users profile.
    // max-debrid.com sister site

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private final String                                   mName              = "www.myfastfile.com";
    private static final String                            NICE_HOST          = "myfastfile.com";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private final String                                   mProt              = "https://";
    private int                                            count              = 0;
    private String                                         ec                 = null;

    private int                                            statuscode         = 0;
    private Account                                        currAcc            = null;
    private DownloadLink                                   currDownloadLink   = null;

    // repeat is one more than desired
    private final int                                      sessionRepeat      = 4;
    private final int                                      globalRepeat       = 4;
    private final String                                   sessionRetry       = "sessionRetry";
    private final String                                   globalRetry        = "globalRetry";
    private static final long                              maxtraffic_daily   = 32212254720l;

    public MyFastFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/premium");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        final AccountInfo ac = new AccountInfo();
        br.setFollowRedirects(true);
        // account is valid, let's fetch account details:
        getAPISafe(mProt + mName + "/api.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        try {
            final float daysleft = Float.parseFloat(getJson("days_left"));
            if (daysleft > 0) {
                long validuntil = System.currentTimeMillis() + (long) (daysleft * 1000 * 60 * 60 * 24);
                ac.setValidUntil(validuntil);
                ac.setUnlimitedTraffic();
                account.setType(AccountType.PREMIUM);
                ac.setStatus("Premium Account");
            } else {
                ac.setTrafficLeft(0);
                /* TODO: Obey this information via API and also show it for premium accounts */
                ac.setTrafficMax(maxtraffic_daily);
                account.setType(AccountType.FREE);
                ac.setStatus("Registered (free) account");
            }
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCan not parse days_left!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }

        // now it's time to get all supported hosts
        getAPISafe("/api.php?hosts");
        if (inValidStatus()) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCan not parse supported hosts!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        String hostsArray = getJsonArray("hosts");
        final String[] hosts = new Regex(hostsArray, "\"(.*?)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        ac.setMultiHostSupport(this, supportedHosts);
        account.setValid(true);
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    /* TODO: Move all-or most of the errorhandling into handleAPIErrors */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);
        count = link.getIntegerProperty(sessionRetry, 0) + 1;
        ec = "(" + count + "/" + sessionRepeat + ")";
        // work around
        if (link.getBooleanProperty("hasFailed", false)) {
            final int hasFailedInt = link.getIntegerProperty("hasFailedWait", 60);
            // nullify old storeables
            link.setProperty("hasFailed", Property.NULL);
            link.setProperty("hasFailedWait", Property.NULL);
            sleep(hasFailedInt * 1001, link);
        }

        // generate downloadlink:
        showMessage(link, "Phase 1/2: Generate download link");
        br.setFollowRedirects(true);
        getAPISafe(mProt + mName + "/api.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));

        if (inValidStatus() && "null".equalsIgnoreCase(getJson("link"))) {
            link.setProperty(sessionRetry, count);
            logger.info("API error, API returned NULL, no downloadlink available...temporarily disabling current host...");
            tempUnavailableHoster(account, link, 60 * 60 * 1000l);
        }

        // parse json
        if (br.containsHTML("Max atteint\\s*!")) {
            link.setProperty(sessionRetry, count);
            // max for this host reached, try again in 1h
            tempUnavailableHoster(account, link, 60 * 60 * 1000l);
        }

        if (br.containsHTML("nvalidlink")) {
            link.setProperty(sessionRetry, count);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Invalid link/unsupported format");
        }

        String dllink = getJson("link");
        if (inValidate(dllink) || !dllink.matches("https?://.+|/.+")) {
            link.setProperty(sessionRetry, count);
            if (!inValidate(dllink) && dllink.contains("Cannot debrid link")) {
                logger.severe("Can not debrid link :: " + ec);
                tempUnavailableHoster(account, link, 20 * 60 * 1000l);
            }
            String msg = "Unknown issue: " + ec;
            logger.severe(msg);
            if (count >= sessionRepeat) {
                tempUnavailableHoster(account, link, 30 * 60 * 1000l);
            } else {
                // temp unavailable will ditch to next download candidate, and retry doesn't respect wait times... !
                link.setProperty(sessionRetry, count);
                link.setProperty("hasFailed", true);
                link.setProperty("hasFailedWait", 15);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        showMessage(link, "Phase 2/2: Download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -12);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (count >= sessionRepeat) {
                /* disable hoster for 1h */
                link.setProperty(sessionRetry, count);
                tempUnavailableHoster(60 * 60 * 1000);
            }
            showMessage(link, "Failed for unknown reason " + ec);
            // temp unavailable will ditch to next download candidate, and retry doesn't respect wait times... !
            link.setProperty(sessionRetry, count);
            link.setProperty("hasFailed", true);
            link.setProperty("hasFailedWait", 60);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();

    }

    @SuppressWarnings("unused")
    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /** 0 = everything ok, 1-? = possible errors */
    private void updatestatuscode() {
        String error = null;
        if (inValidStatus()) {
            error = getJson("msg");
        }
        if (error != null) {
            if (error.equals("Cannot login Check your username or pass")) {
                statuscode = 1;
            } else if (error.equals("Your account is not premium")) {
                statuscode = 2;
            } else {
                /* TODO: Enable code below once all known errors are correctly handled */
                // statuscode = 666;
            }
        } else {
            statuscode = 0;
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* Invalid account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.\r\nWichtig: Das Passwort muss dein APIKey sein, siehe dein Profil auf der " + mName + " Webseite.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.\r\nNote: Password has to be APIKey, see Account Profile on " + mName + "website.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2:
                /*
                 * Free accounts have no traffic - disable them on downloadtry (should actually never happen as they're added with ZERO
                 * trafficleft)
                 */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            default:
                /* Unknown error */
                logger.warning("Unknown API error happened!");
                /* TODO: Implement all- or as many errors as possible, then activate the code below */
                // statusMessage = "Unknown error";
                // logger.info(NICE_HOST + ": Unknown API error");
                // handleErrorRetries("unknownAPIerror", 10);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
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
    private void handleErrorRetries(final String error, final int maxRetries) throws PluginException {
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
            // tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
            /* TODO: Remove plugin defect once all known errors are correctly handled */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error);
        }
    }

    private boolean inValidStatus() {
        return !"ok".equalsIgnoreCase(getJson("status"));
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("[\r\n\t ]+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        // we reset session retry and int global here to allow for multiple retries, with tempUnailable inbetween
        downloadLink.setProperty(sessionRetry, Property.NULL);
        final int gr = downloadLink.getIntegerProperty(globalRetry, 0) + 1;
        downloadLink.setProperty(globalRetry, gr);
        if (gr >= globalRepeat) {
            // prevent more than globalRepeat retries.
            throw new PluginException(LinkStatus.ERROR_FATAL, "Exausted Global Retry!");
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty(sessionRetry, Property.NULL);
        link.setProperty(globalRetry, Property.NULL);
    }

}