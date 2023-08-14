//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public abstract class RapidtrafficCore extends PluginForHost {
    protected abstract MultiHosterManagement getMultiHosterManagement();

    private static final String PROPERTY_ACCOUNTINFO_TRAFFICLEFT = "traffic_left";

    public RapidtrafficCore(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getBaseURL() + "konto");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    private String getBaseURL() {
        return "http://" + this.getHost() + "/";
    }

    public void init() {
    }

    private Browser prepBR(final Browser br) {
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        return br;
    }

    private void login(final Account account, final boolean validateCookies) throws PluginException, IOException {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(cookies);
                    if (!validateCookies) {
                        /* Trust cookies without check */
                        return;
                    }
                    logger.info("Verifying cookies...");
                    br.getPage(this.getBaseURL() + "konto");
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                br.setAcceptLanguage("pl-PL,pl;q=0.8,en-US;q=0.6,en;q=0.4");
                br.postPage(getBaseURL() + "index.php", "v=konto%7Cmain&c=aut&f=loginUzt&friendlyredir=1&usr_login=" + Encoding.urlEncode(account.getUser()) + "&usr_pass=" + Encoding.urlEncode(account.getPass()));
                if (!this.isLoggedIN(br)) {
                    if (br.containsHTML("Podano nieprawidłową parę login - hasło lub konto nie zostało aktywowane")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("INVALID_LOGIN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (!isLoggedIN(br)) { // double-check
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("INVALID_LOGIN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        /* Check for presence of 'logout' button. */
        if (br.containsHTML("'/wyloguj'")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        String validUntil = null;
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (br.getRequest() == null || !br.getURL().contains("konto")) {
            br.getPage("/konto");
        }
        final String hosterNames = " " + br.getRegex("(?i)Tutaj wklej linki do plików z <strong>(.*)</strong>, które chcesz ściągnąć").getMatch(0) + ",";
        final String[] hostDomains = new Regex(hosterNames, " ([^,<>\"]*?),").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hostDomains));
        ai.setMultiHostSupport(this, supportedHosts);
        String transferLeftStr = br.getRegex("(?i)Pozostały transfer: <b>(-?\\d+\\.\\d+ [GM]B)</b>").getMatch(0).replace(".", ",");
        String trafficLeftHumanReadable = "Unknown";
        if (transferLeftStr != null) {
            transferLeftStr = transferLeftStr.replace(".", ",").trim();
            final long trafficLeftLong;
            if (transferLeftStr.startsWith("-")) {
                trafficLeftLong = -SizeFormatter.getSize(transferLeftStr);
            } else {
                trafficLeftLong = SizeFormatter.getSize(transferLeftStr);
            }
            /* Do not set this value on accountInfo as we cannot trust it 100%!!! */
            // ai.setTrafficLeft(trafficLeftLong);
            ai.setProperty(PROPERTY_ACCOUNTINFO_TRAFFICLEFT, trafficLeftLong);
            trafficLeftHumanReadable = SIZEUNIT.formatValue((SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue(), trafficLeftLong);
        }
        /* Inactive --> Free account --> Free accounts can still have leftover "traffic left" values though (can be negative values). */
        if (br.containsHTML("(?i)Konto ważne do\\s*:\\s*<b>\\s*nieaktywne\\s*</b>")) {
            ai.setExpired(true);
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        } else {
            validUntil = br.getRegex("(?i)Konto ważne do\\s*:\\s*<b>(\\d{4}\\-\\d{2}\\-\\d{2})</b>").getMatch(0);
            if (validUntil == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setType(AccountType.PREMIUM);
            ai.setUnlimitedTraffic();
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "yyyy-MM-dd", Locale.ENGLISH));
            ai.setStatus(getPhrase("PREMIUM") + " (" + getPhrase("TRAFFIC_LEFT") + ": " + trafficLeftHumanReadable + ")");
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return getBaseURL() + "regulamin.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(final DownloadLink link, final String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        getMultiHosterManagement().runCheck(account, link);
        boolean resume = true;
        showMessage(link, "Phase 1/4: Login");
        login(account, true);
        final String userId = br.getRegex("<input type='hidden' name='usr' value='(\\d+)' id='usr_check' />").getMatch(0);
        if (userId == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String generatedLink = checkDirectLink(link, this.getDirecturlproperty());
        if (generatedLink == null) {
            /* generate new downloadlink */
            String url = Encoding.urlEncode(link.getDownloadURL());
            String postData = "v=usr%2Csprawdzone%7Cusr%2Clinki&c=pob&f=sprawdzLinki&usr=" + userId + "&progress_type=check&linki=" + url;
            showMessage(link, "Phase 2/4: Checking Link");
            br.postPage(getBaseURL() + "index.php", postData);
            sleep(2 * 1000l, link);
            if (br.containsHTML("<td class='file_error' id='linkstatus_1'>Błędny link</td>")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("BAD_LINK"), 10 * 60 * 1000l);
            } else if (br.containsHTML("Rozmiar pobieranych plików przekracza dostępny transfer")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("NO_TRAFFIC"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            postData = "v=usr%2Cpliki%7Cusr%2Clinki&c=pob&f=zapiszRozpoczete&usr=" + userId + "&progress_type=verified&link_ok%5B1%5D=" + url;
            br.postPage(getBaseURL() + "index.php", postData);
            String fileId = "";
            sleep(2 * 1000l, link);
            for (int i = 1; i <= 3; i++) {
                if (!br.containsHTML(">Gotowy</td>")) {
                    sleep(3 * 1000l, link);
                    br.getPage(getBaseURL() + "index.php");
                } else {
                    fileId = br.getRegex("<td class='file_status' id='fstatus_(\\d+)_0'>Gotowy</td>").getMatch(0);
                    if (fileId != null) {
                        break;
                    }
                }
            }
            if (StringUtils.isEmpty(fileId)) {
                /* This should never happen */
                getMultiHosterManagement().handleErrorGeneric(account, link, "Failed to get internal fileID", 50, 3 * 60 * 1000l);
            }
            postData = "v=usr%2Cpliki&c=fil&f=usunUsera&perm=wygeneruj+linki&fil%5B" + fileId + "%5D=on";
            showMessage(link, "Phase 3/4: Generating Link");
            br.postPage(getBaseURL() + "index.php", postData);
            sleep(2 * 1000l, link);
            generatedLink = br.getRegex("(?i)<h2>Wygenerowane linki bezpośrednie</h2><textarea rows='1' style='width: 650px; height: 40px'>(.*)</textarea>").getMatch(0);
            if (generatedLink == null) {
                getMultiHosterManagement().handleErrorGeneric(account, link, "Failed to find final downloadurl", 50, 3 * 60 * 1000l);
            }
            link.setProperty(getDirecturlproperty(), generatedLink);
        }
        sleep(1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedLink, resume, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            // not tested!
            if (br.containsHTML("<div id=\"message\">Ważność linka wygasła.</div>")) {
                // previously generated link expired,
                // clear the property and restart the download
                // and generate new link
                sleep(10 * 1000l, link, "Previously generated Link expired!");
                logger.info("Generated directurl expired - removing it and restarting download process.");
                link.removeProperty("generatedLinkRapidtraffic");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            // not tested! - test if the error occurs
            if (br.getBaseURL().contains("notransfer")) {
                /* No traffic left */
                account.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("NO_TRAFFIC"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.getBaseURL().contains("serviceunavailable")) {
                getMultiHosterManagement().putError(account, link, 3 * 60 * 1000l, "Host unavailable");
            } else if (br.getBaseURL().contains("connecterror")) {
                getMultiHosterManagement().handleErrorGeneric(account, link, "connecterror", 50, 3 * 60 * 1000l);
            } else if (br.getBaseURL().contains("notfound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        showMessage(link, "Phase 4/4: Begin download");
        dl.startDownload();
    }

    private String getDirecturlproperty() {
        return this.getHost() + "_directurl";
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            getMultiHosterManagement().runCheck(account, link);
            return true;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void extendAccountSettingsPanel(final Account account, final PluginConfigPanelNG panel) {
        AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            final long availableTraffic = ai.getLongProperty(PROPERTY_ACCOUNTINFO_TRAFFICLEFT, 0);
            if (availableTraffic >= 0) {
                panel.addStringPair(_GUI.T.lit_traffic_left(), SIZEUNIT.formatValue((SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue(), availableTraffic));
            }
        }
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("INVALID_LOGIN", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
                                                      put("LOGIN_ERROR", "Rapidtraffic.pl: Login Error");
                                                      put("LOGIN_FAILED", "Login failed!\r\nPlease check your Username and Password!");
                                                      put("HOST_UNAVAILABLE", "Host is temporarily unavailable via ");
                                                      put("RETRY", "Retry in few secs");
                                                      put("NO_TRAFFIC", "No traffic left");
                                                      put("LOGIN_FAILED_NOT_PREMIUM", "Login failed or not Premium");
                                                      put("PREMIUM", "Premium User");
                                                      put("TRAFFIC_LEFT", "Traffic Left");
                                                      put("ACCOUNT_TYPE", "Account type");
                                                      put("BAD_LINK", "Multihoster rapidtraffic.pl reports: Bad link!");
                                                  }
                                              };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("INVALID_LOGIN", "\r\nNieprawidłowy login/hasło!\r\nCzy jesteś pewien, że poprawnie wprowadziłeś nazwę użytkownika i hasło? Sugestie:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź nazwę użytkownika/hasło ręcznie, bez użycia funkcji Kopiuj i Wklej.");
                                                      put("LOGIN_ERROR", "Rapidtraffic.pl: Błąd logowania");
                                                      put("LOGIN_FAILED", "Logowanie nieudane!\r\nZweryfikuj proszę Nazwę Użytkownika i Hasło!");
                                                      put("HOST_UNAVAILABLE", "Pobieranie z tego serwisu jest tymczasowo niedostępne w ");
                                                      put("RETRY", "Ponowna próba za kilka sekund");
                                                      put("NO_TRAFFIC", "Brak dostępnego transferu");
                                                      put("LOGIN_FAILED_NOT_PREMIUM", "Nieprawidłowe konto lub konto nie-Premium");
                                                      put("PREMIUM", "Użytkownik Premium");
                                                      put("TRAFFIC_LEFT", "Pozostały transfer");
                                                      put("ACCOUNT_TYPE", "Typ konta");
                                                      put("BAD_LINK", "Serwis rapidtraffic.pl zgłasza: Błędny Link!");
                                                  }
                                              };

    /**
     * Returns a Polish/English translation of a phrase. We don't use the JDownloader translation framework since we need only Polish and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(final String key) {
        if ("pl".equals(System.getProperty("user.language")) && phrasesPL.containsKey(key)) {
            return phrasesPL.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }
}