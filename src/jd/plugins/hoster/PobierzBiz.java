//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pobierz.biz" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class PobierzBiz extends PluginForHost {
    /* Tags: pobierz.biz, rapidtraffic.pl */
    private String                                         MAINPAGE           = "http://pobierz.biz/";
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Object                                  LOCK               = new Object();

    public PobierzBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "konto");
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private void login(Account account) throws PluginException, IOException {
        synchronized (LOCK) {
            try {
                br.setCustomCharset("utf-8");
                br.setCookiesExclusive(true);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                br.setAcceptLanguage("pl-PL,pl;q=0.9,en;q=0.8");
                br.postPage(MAINPAGE + "index.php", "v=konto%7Cmain&c=aut&f=loginUzt&friendlyredir=1&usr_login=" + Encoding.urlEncode(account.getUser()) + "&usr_pass=" + Encoding.urlEncode(account.getPass()));
                String redirectPage = br.getRedirectLocation();
                if (redirectPage != null) {
                    br.getPage(redirectPage);
                } else {
                    if (br.containsHTML("Podano nieprawidłową parę login - hasło lub konto nie zostało aktywowane")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("INVALID_LOGIN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE + "/konto");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        String validUntil = null;
        final AccountInfo ai = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setStatus(getPhrase("LOGIN_FAILED_NOT_PREMIUM"));
            UserIO.getInstance().requestMessageDialog(0, getPhrase("LOGIN_ERROR"), getPhrase("LOGIN_FAILED"));
            account.setValid(false);
            return ai;
        }
        if (!br.getURL().contains("konto")) {
            br.getPage(MAINPAGE + "mojekonto");
        }
        final String hosterNames = " " + br.getRegex("Tutaj wklej linki do plików z <strong>(.*)</strong>, które chcesz ściągnąć").getMatch(0) + ",";
        final String[] hostDomains = new Regex(hosterNames, " ([^,<>\"]*?),").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hostDomains));
        account.setValid(true);
        ai.setMultiHostSupport(this, supportedHosts);
        String transferLeft = br.getRegex("Pozostały transfer: <b>(\\d+\\.\\d+ [GM]B)</b>").getMatch(0).replace(".", ",");
        long trafficLeftLong = ((transferLeft == null) ? 0 : SizeFormatter.getSize(transferLeft));
        if (br.containsHTML("Konto ważne do: <b>nieaktywne</b>")) {
            ai.setExpired(true);
            ai.setProperty("free", true);
            ai.setTrafficLeft(trafficLeftLong);
            ai.setStatus(getPhrase("PREMIUM_EXPIRED"));
            return ai;
        } else {
            validUntil = br.getRegex("Konto ważne do: <b>(\\d{4}\\-\\d{2}\\-\\d{2})</b>").getMatch(0);
            if (validUntil == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("PLUGIN_BROKEN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            ai.setProperty("free", false);
            ai.setUnlimitedTraffic();
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "yyyy-MM-dd", Locale.ENGLISH));
            ai.setProperty("Available traffic", trafficLeftLong);
            ai.setStatus(getPhrase("PREMIUM") + " (" + getPhrase("TRAFFIC_LEFT") + ": " + SizeFormatter.formatBytes(trafficLeftLong) + ")");
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "regulamin.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("HOST_UNAVAILABLE") + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        boolean resume = true;
        showMessage(link, "Phase 1/4: Login");
        login(account);
        String userId = br.getRegex("<input type='hidden' name='usr' value='(\\d+)' id='usr_check' />").getMatch(0);
        if (userId == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("PLUGIN_BROKEN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        dl = null;
        String generatedLink = checkDirectLink(link, "generatedLinkPobierzBiz");
        if (generatedLink == null) {
            /* generate new downloadlink */
            String url = Encoding.urlEncode(link.getDownloadURL());
            String postData = "v=usr%2Csprawdzone%7Cusr%2Clinki&c=pob&f=sprawdzLinki&usr=" + userId + "&progress_type=check&linki=" + url;
            showMessage(link, "Phase 2/4: Checking Link");
            br.postPage(MAINPAGE + "index.php", postData);
            sleep(2 * 1000l, link);
            if (br.containsHTML("Rozmiar pobieranych plików przekracza dostępny transfer")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("NO_TRAFFIC"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (!br.containsHTML("<td class='file_ok' id='linkstatus_1'>OK</td>")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("INVALID_LINK"));
            }
            postData = "v=usr%2Cpliki%7Cusr%2Clinki&c=pob&f=zapiszRozpoczete&usr=" + userId + "&progress_type=verified&link_ok%5B1%5D=" + url;
            br.postPage(MAINPAGE + "index.php", postData);
            String fileId = "";
            sleep(2 * 1000l, link);
            for (int i = 1; i <= 3; i++) {
                if (!br.containsHTML(">Gotowy</td>")) {
                    sleep(3 * 1000l, link);
                    br.getPage(MAINPAGE + "index.php");
                } else {
                    fileId = br.getRegex("<td class='file_status' id='fstatus_(\\d+)_0'>Gotowy</td>").getMatch(0);
                    if (fileId != null) {
                        break;
                    }
                }
            }
            if (fileId == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("PLUGIN_BROKEN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            postData = "v=usr%2Cpliki&c=fil&f=usunUsera&perm=wygeneruj+linki&fil%5B" + fileId + "%5D=on";
            showMessage(link, "Phase 3/4: Generating Link");
            br.postPage(MAINPAGE + "index.php", postData);
            sleep(2 * 1000l, link);
            generatedLink = br.getRegex("<h2>Wygenerowane linki bezpośrednie</h2><textarea rows='1' style='width: 650px; height: 40px'>(.*)</textarea>").getMatch(0);
            if (generatedLink == null) {
                logger.severe("Pobierz.biz (Error): " + generatedLink);
                if (link.getLinkStatus().getRetryCount() >= 2) {
                    try {
                        // disable hoster for 30min
                        tempUnavailableHoster(account, link, 30 * 60 * 1000l);
                    } catch (Exception e) {
                    }
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 2 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("RETRY") + msg, 20 * 1000l);
            }
            link.setProperty("generatedLinkPobierzBiz", generatedLink);
        }
        sleep(1 * 1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedLink, resume, 0);
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) // unknown
        // error
        {
            br.followConnection();
            // not tested!
            if (br.containsHTML("<div id=\"message\">Ważność linka wygasła.</div>")) {
                // previously generated link expired,
                // clear the property and restart the download
                // and generate new link
                sleep(10 * 1000l, link, "Previously generated Link expired!");
                logger.info("Pobierz.biz: previously generated link expired - removing it and restarting download process.");
                link.setProperty("generatedLinkPobierzBiz", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            // not tested! - test if the error occurs
            if (br.getBaseURL().contains("notransfer")) {
                /* No traffic left */
                account.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("NO_TRAFFIC"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.getBaseURL().contains("serviceunavailable")) {
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
            if (br.getBaseURL().contains("connecterror")) {
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
            if (br.getBaseURL().contains("notfound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            tempUnavailableHoster(account, link, 20 * 60 * 1000l);
        }
        showMessage(link, "Phase 4/4: Begin download");
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    // try redirected link
                    boolean resetGeneratedLink = true;
                    String redirectConnection = br2.getRedirectLocation();
                    if (redirectConnection != null) {
                        if (redirectConnection.contains("pobierz.biz")) {
                            con = br2.openGetConnection(redirectConnection);
                            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                                resetGeneratedLink = true;
                            } else {
                                resetGeneratedLink = false;
                            }
                        }
                    }
                    if (resetGeneratedLink) {
                        downloadLink.setProperty(property, Property.NULL);
                        dllink = null;
                    }
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
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
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void extendAccountSettingsPanel(Account acc, PluginConfigPanelNG panel) {
        AccountInfo ai = acc.getAccountInfo();
        if (ai != null) {
            long availableTraffic = Long.parseLong(ai.getProperty("Available traffic").toString(), 10);
            if (availableTraffic >= 0) {
                panel.addStringPair(_GUI.T.lit_traffic_left(), SizeFormatter.formatBytes(availableTraffic));
            }
        }
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("INVALID_LOGIN", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
                                                      put("LOGIN_ERROR", "Pobierz.biz: Login Error");
                                                      put("LOGIN_FAILED", "Login failed!\r\nPlease check your Username and Password!");
                                                      put("PLUGIN_BROKEN", "\r\nPlugin broken, please contact the JDownloader Support!");
                                                      put("HOST_UNAVAILABLE", "Host is temporarily unavailable via ");
                                                      put("RETRY", "Retry in few secs");
                                                      put("NO_TRAFFIC", "No traffic left");
                                                      put("LOGIN_FAILED_NOT_PREMIUM", "Login failed or not Premium");
                                                      put("PREMIUM", "Premium User");
                                                      put("TRAFFIC_LEFT", "Traffic Left");
                                                      put("PREMIUM_EXPIRED", "Premium expired");
                                                      put("ACCOUNT_TYPE", "Account type");
                                                      put("INVALID_LINK", "Pobierz.biz reports: Invalid link");
                                                  }
                                              };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("INVALID_LOGIN", "\r\nNieprawidłowy login/hasło!\r\nCzy jesteś pewien, że poprawnie wprowadziłeś nazwę użytkownika i hasło? Sugestie:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź nazwę użytkownika/hasło ręcznie, bez użycia funkcji Kopiuj i Wklej.");
                                                      put("LOGIN_ERROR", "Pobierz.biz: Błąd logowania");
                                                      put("LOGIN_FAILED", "Logowanie nieudane!\r\nZweryfikuj proszę Nazwę Użytkownika i Hasło!");
                                                      put("PLUGIN_BROKEN", "\r\nBłąd wtyczki, skontaktuj się z działem wsparcia JDownloadera!");
                                                      put("HOST_UNAVAILABLE", "Pobieranie z tego serwisu jest tymczasowo niedostępne w ");
                                                      put("RETRY", "Ponowna próba za kilka sekund");
                                                      put("NO_TRAFFIC", "Brak dostępnego transferu");
                                                      put("LOGIN_FAILED_NOT_PREMIUM", "Nieprawidłowe konto lub konto nie-Premium");
                                                      put("PREMIUM", "Użytkownik Premium");
                                                      put("TRAFFIC_LEFT", "Pozostały transfer");
                                                      put("PREMIUM_EXPIRED", "Konto Premium wygasło");
                                                      put("ACCOUNT_TYPE", "Typ konta");
                                                      put("INVALID_LINK", "Pobierz.biz zwraca: Błędny link");
                                                  }
                                              };

    /**
     * Returns a Polish/English translation of a phrase. We don't use the JDownloader translation framework since we need only Polish and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("pl".equals(System.getProperty("user.language")) && phrasesPL.containsKey(key)) {
            return phrasesPL.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }
}