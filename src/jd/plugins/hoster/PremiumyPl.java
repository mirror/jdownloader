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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumy.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class PremiumyPl extends PluginForHost {

    private static final String                            API_ENDPOINT        = "http://premiumy.pl/jd,%s.html";
    private static final String                            NICE_HOST           = "premiumy.pl";
    private static final String                            NICE_HOSTproperty   = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            PROPERTY_LOGINTOKEN = "premiumypllogintoken";

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap  = new HashMap<Account, HashMap<String, Long>>();

    /* Last updated: 31.03.15 */
    private static final int                               defaultMAXDOWNLOADS = 20;
    private static final int                               defaultMAXCHUNKS    = 0;
    private static final boolean                           defaultRESUME       = true;

    private static Object                                  CTRLLOCK            = new Object();
    private int                                            statuscode          = 1;
    private Account                                        currAcc             = null;
    private DownloadLink                                   currDownloadLink    = null;
    private static String                                  currLogintoken      = null;

    @SuppressWarnings("deprecation")
    public PremiumyPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://premiumy.pl/store.html");
    }

    @Override
    public String getAGBLink() {
        return "https://premiumy.pl/1,regulamin.html";
    }

    private Browser newBrowser(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
        if (currLogintoken == null) {
            currLogintoken = this.getLoginToken();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
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
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            /* Create downloadlink */
            this.postAPISafe("checkLink", "url=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = PluginJSonUtils.getJsonValue(this.br, "downloadLink");
            String error = PluginJSonUtils.getJsonValue(this.br, "error");
            if (dllink == null || this.statuscode != 1 || error != null) {
                logger.warning("Final downloadlink is null");
                handleErrorRetries((error == null) ? "dllinknull" : error, 10, 60 * 60 * 1000l);
            }
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            updatestatuscode();
            handleAPIErrors(this.br);
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
        newBrowser(this.br);

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
                // con = br2.openHeadConnection(dllink); // doesn't work - throws Read timout
                URLConnectionAdapter con = br2.openGetConnection(dllink);
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
        if (br.containsHTML("<div class=\"contentTitle\">Błąd pobierania</div>")) {
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + "downloaderror", timesFailed);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Link unavailable", 86400000l);
        }
        if ("limitExceeded".equals(error)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Limit Exceeded", 86400000l);
        }

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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.setConstants(account, null);
        newBrowser(this.br);
        final AccountInfo ai = new AccountInfo();

        login(true);
        this.postAPISafe("accountInfo", "");

        final String trafficLeftStr = PluginJSonUtils.getJsonValue(this.br, "transfer");
        final String premium = PluginJSonUtils.getJsonValue(this.br, "premium");
        
        boolean isFree = false;
        if ("0".equals(premium) && "0".equals(trafficLeftStr)) {
            isFree = true;           
        }

        long trafficleft = 0;
        long timestampValidUntil = 0;

        String hostsValidDates = PluginJSonUtils.getJsonNested(this.br, "hosts");
        String validDates[][] = null;

        if (trafficLeftStr != null) {
            trafficleft = Long.parseLong(trafficLeftStr);
        }

        try {
            timestampValidUntil = Long.parseLong(premium);
        } catch (Exception e) {
            timestampValidUntil = 0;
        }
        boolean isSingleHoster = false;
        boolean isMulti = false;
        boolean isTransfer = false;
        if (timestampValidUntil > 0l) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account - Multi");
            timestampValidUntil = timestampValidUntil * 1000l;
            if (timestampValidUntil > System.currentTimeMillis()) {
                ai.setValidUntil(timestampValidUntil);
            }
            isMulti = true;
        }
        if (trafficleft > 0) {
            ai.setTrafficLeft(trafficleft);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account - Transfer");
            isTransfer = true;
        }
        if (isMulti && isTransfer) {
            ai.setStatus("Premium account - Transfer/Multi");
            /*
             * do not set traffic and valid date, 
			 * because Transfer has traffic and no date,
		     * Multi has data and no traffic
             */
            ai.setUnlimitedTraffic();
            ai.setValidUntil(-1l);

        } else {
            if (isTransfer || isFree) {
                // Free has premium="0" and transfer="0" the same as Hoster plan
                // check if it is hoster plan - check for expire date for specyfic hoster

                validDates = new Regex(hostsValidDates, "\"([^<>\"]+)\":(\\d+),?").getMatches();

                for (String[] validDate : validDates) {
                    timestampValidUntil = Long.parseLong(validDate[1]);
                    if (timestampValidUntil > 0) {
                        timestampValidUntil = timestampValidUntil * 1000l;
                        if (timestampValidUntil > System.currentTimeMillis()) {
                            ai.setValidUntil(timestampValidUntil);
                            break;
                        }
                    }

                }
                if (timestampValidUntil > 0) {
                    account.setType(AccountType.PREMIUM);
                    ai.setStatus("Premium account - Hoster");
                    isSingleHoster = true;
                } else {
                    if (!isTransfer) {
                        ai.setTrafficLeft(0);
                        account.setType(AccountType.FREE);
                        ai.setStatus("Free account");
                        return ai;
                    }
                }
            }
        }
        this.postAPISafe("hosts", "");

        final String[] hosts = this.br.getRegex("\"domain\":\"([^<>\"]+)\"").getColumn(0);

        // * correct list of hosters for single hoster plan
        if (!hostsValidDates.isEmpty() && isSingleHoster) {
            final String[] hostsNames = this.br.getRegex("\"name\":\"([^<>\"]+)\",?").getColumn(0);
            List<String> hostersAvailable = new ArrayList<String>();
            for (String[] validDate : validDates) {
                if (Long.parseLong(validDate[1]) > 0) {
                    for (int i = 0; i < hostsNames.length; i++) {
                        if (validDate[0].equals(hostsNames[i])) {
                            hostersAvailable.add(hosts[i]);
                            break;
                        }
                    }
                }
            }
            if (hostersAvailable.size() > 0) {
                ai.setMultiHostSupport(this, hostersAvailable);
            }
        } else {
            ai.setMultiHostSupport(this, Arrays.asList(hosts));
        }
        return ai;
    }

    private void login(final boolean force) throws IOException, PluginException {
        /* TODO: Maybe find a way that does not always require a full login! */
        this.br.postPage(String.format(API_ENDPOINT, "accountLogin"), "login=" + Encoding.urlEncode(this.currAcc.getUser()) + "&password=" + Encoding.urlEncode(this.currAcc.getPass()));
        this.updatestatuscode();
        currLogintoken = PluginJSonUtils.getJsonValue(this.br, "session");
        if (this.statuscode == 0 || currLogintoken == null || currLogintoken.equals("")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        this.currAcc.setProperty(PROPERTY_LOGINTOKEN, currLogintoken);
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
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, String postdata) throws IOException, PluginException {
        if (currLogintoken != null) {
            postdata += "&session=" + Encoding.urlEncode(currLogintoken);
        }
        this.br.postPage(String.format(API_ENDPOINT, accesslink), postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private String getLoginToken() {
        return currAcc.getStringProperty(PROPERTY_LOGINTOKEN, null);
    }

    /**
     * 0=error, 1 = everything ok, 666 = hell
     */
    private void updatestatuscode() {
        /* First look for errorcode */
        String error = PluginJSonUtils.getJsonValue(this.br, "status");
        if (inValidate(error)) {
            error = null;
        }
        if (error != null && error.matches("\\d+")) {
            statuscode = Integer.parseInt(error);
        } else if (error != null) {
            /* Text-errormessage?! */
            statuscode = 666;
        } else {
            statuscode = 1;
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Error but errors are not handled here! */
                break;
            case 1:
                /* Everything ok */
                break;
            case 666:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 20, 5 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return defaultMAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}