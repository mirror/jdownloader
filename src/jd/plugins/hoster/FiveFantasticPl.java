//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "5fantastic.pl" }, urls = { "http://(www\\.)?5fantastic\\.pl/plikosfera/\\d+/[A-Za-z]+/\\d+" }, flags = { 0 })
public class FiveFantasticPl extends PluginForHost {
    private static Object LOCK   = new Object();
    private String        HOSTER = "http://www.5fantastic.pl";

    public FiveFantasticPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HOSTER + "/index.aspx");
    }

    @Override
    public String getAGBLink() {
        return HOSTER + "/strona.aspx?gidart=4558";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate, lzma, sdch");
        final String downloadUrl = link.getDownloadURL();

        try {
            br.getPage(downloadUrl);
        } catch (Exception e) {
            final String errorMessage = e.getCause().getMessage();
            final String newName = getPhrase("LINK_UNCHECKABLE");
            if (!link.getName().contains(newName)) {
                link.setName(link.getName() + " " + newName);
            }
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);

            if (errorMessage.contains("Content-length too big")) {
                try {
                    link.setComment(getPhrase("FOLDERS_NOT_SUPPORTED"));
                } catch (final Throwable t) {
                }
                logger.severe("FiveFantastic: the link: " + downloadUrl + " seems to be folder. Folders are not supported!");
                throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("FOLDERS_NOT_SUPPORTED"));
            }
            try {
                link.setComment(getPhrase("TRYING_TO_GET_PAGE") + downloadUrl + getPhrase("GOT_ERROR") + errorMessage);
            } catch (final Throwable t) {
            }

            logger.severe("FiveFantastic: trying to get page for link: " + downloadUrl + ",  got error: " + errorMessage);
            throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("ERROR") + errorMessage);
        }

        if (br.containsHTML("Plik jest plikiem prywatnym.")) {
            // check if the link is user's private file
            // only logged user has access to his private files
            List<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            boolean isPremium = false;
            Account premiumAccount = null;
            if (accs == null || accs.size() == 0) {
                logger.info("No account present.");

            } else {
                for (Account account : accs) {
                    if (account.getProperty("Premium") == "T") {
                        premiumAccount = account;
                        isPremium = true;
                        break;
                    }
                }
            }

            if (isPremium) {
                try {
                    login(premiumAccount, true);
                } catch (Exception e) {
                    logger.info("5Fantastic Premium Error: Login failed or not Premium!");

                }
            }
            br.getPage(downloadUrl);

            if (br.containsHTML("Plik jest plikiem prywatnym.")) {
                try {
                    link.setComment(getPhrase("PRIVATE_FILE"));
                } catch (final Throwable t) {
                }
                logger.info("FiveFantastic: " + link.getDownloadURL() + " is private file - download impossible!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, getPhrase("FILE_ERROR"));
            }
        }

        if (br.containsHTML("<title>[\t\n\r ]+5fantastic\\.pl \\- najlepszy darmowy dysk internetowy \\- pliki udostępnione przez klubowiczów[\t\n\r ]+</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, getPhrase("FILE_ERROR"));
        }
        final String filename = br.getRegex("<title>[\t\n\r ]+5fantastic\\.pl \\- ([^<>\"]*?) \\- najlepszy darmowy dysk internetowy[\t\n\r ]+</title>").getMatch(0);
        final String filesize = br.getRegex(">Rozmiar: </span><span style=\"margin\\-left:5px;line\\-height:15px;\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String encodedLink = Encoding.urlEncode(downloadLink.getDownloadURL());
        String vsKey = br.getRegex("type=\"hidden\" name=\"vs_key\" id=\"vs_key\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (vsKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        vsKey = Encoding.urlEncode(vsKey);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-MicrosoftAjax", "Delta=true");

        // Get file
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updLnkPobierz%7Cctl00%24ctnMetryka%24metrykaPliku%24lnkPobierz&ctl00_ScriptManager1_HiddenField=&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24lnkPobierz&__EVENTARGUMENT=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&__ASYNCPOST=true&");

        if (br.containsHTML("Przepraszamy, plik jest chwilowo niedostepny")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("TEMPORARY_UNAVAILABLE"), 60 * 1000L);
        }
        this.sleep(3000, downloadLink);

        // Select FREE download with speed limit
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updPobieraniePliku%7Cctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierz&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierz&__EVENTARGUMENT=&__ASYNCPOST=true&");

        this.sleep(3000, downloadLink);
        // Confirm TOS and get final link
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updPobieraniePliku%7Cctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&ctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin=on&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin&__EVENTARGUMENT=&__LASTFOCUS=&__ASYNCPOST=true&");

        if (br.containsHTML("Z Twojego numeru IP jest już pobierany plik. Poczekaj na zwolnienie zasobów, albo ściągnij plik za punkty")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("DOWNLOAD_DETECTED"), 5 * 60 * 1000l);
        }
        final String dllink = br.getRegex("\"(https?://[a-z0-9]+\\.5fantastic\\.pl/[^<>\"]*?download\\.php\\?[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("jest aktualnie pobierany inny plik")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("DOWNLOAD_DETECTED"), 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        boolean hours = false;
        try {
            login(account, true);
        } catch (PluginException e) {
            ai.setStatus(getPhrase("NO_PREMIUM"));
            UserIO.getInstance().requestMessageDialog(0, getPhrase("PREMIUM_ERROR"), getPhrase("LOGIN_FAILED"));
            account.setValid(false);
            return ai;
        }

        checkTrafficLimits(ai);

        account.setValid(true);
        try {
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }

        ai.setStatus(getPhrase("PREMIUM_USER"));
        // save property for checking availability of private files
        account.setProperty("Premium", "T");
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        String user = Encoding.urlEncode(account.getUser());
        String pwd = Encoding.urlEncode(account.getPass());
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = user.equals(account.getStringProperty("name", user));
                if (acmatch) {
                    acmatch = pwd.equals(account.getStringProperty("pass", pwd));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(HOSTER, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0");
                br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                br.getHeaders().put("Accept-Language", "pl-PL,pl;q=0.8,en-US;q=0.6,en;q=0.4");

                br.getPage(HOSTER + "/index.aspx");

                String vsKey = br.getRegex("type=\"hidden\" name=\"vs_key\" id=\"vs_key\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (vsKey == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                vsKey = Encoding.urlEncode(vsKey);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("X-MicrosoftAjax", "Delta=true");

                br.postPage(HOSTER + "/index.aspx", "ctl00%24ScriptManager1=ctl00%24panelLogowania%24updPanelLogowania%7Cctl00%24panelLogowania%24lnkZalogujMnie&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&loginText=" + user + "&passwordText=" + pwd + "&ctl00%24panelLogowania%24chkZapamietaj=on&ctl00%24cntStrona%24txt1strWyszukiwanie=Wybierz%20dzia%C5%82%20i%20wpisz%20nazw%C4%99%20pliku%20lub%20szukane%20s%C5%82owo&__EVENTTARGET=ctl00%24panelLogowania%24lnkZalogujMnie&__EVENTARGUMENT=&__ASYNCPOST=true&");
                br.getPage(HOSTER + "/Strona_glowna");
                if (!br.containsHTML("<div id=\"ctl00_userNick\" style=\".*\">" + user + "</div>")) {
                    logger.warning("Couldn't determine premium status or account is Free not Premium!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("PREMIUM_INVALID"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(HOSTER);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", user);
                account.setProperty("pass", pwd);
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    void checkTrafficLimits(AccountInfo ai) {
        final String[][] limits = br.getRegex("<div id=\"ctl00_updpunktyint\">[ \t\n\r]+<div style=\".*?\">[ \t\n\r]+<span id=\".*?\" style=\".*?\">[ \t\n\r]+([0-9]+)[ \t\n\r]+</span>[ \t\n\r]+</div>[ \t\n\r]+</div>[ \t\n\r]+<div style=\".*?punkty /([A-Za-z]+) transferu</div>").getMatches();
        if (limits.length != 0) {
            final String maxLimit = limits[0][0];

            final String unit = limits[0][1];
            // ai.setTrafficMax(SizeFormatter.getSize(maxLimit + " " + unit));
            ai.setTrafficLeft(SizeFormatter.getSize(maxLimit + " " + unit));

        } else {
            // ai.setUnlimitedTraffic();
            ai.setTrafficLeft(0);
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        login(account, true);

        AccountInfo ai = account.getAccountInfo();
        checkTrafficLimits(ai);
        if (ai.getTrafficLeft() <= 0) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("NO_TRAFFIC_LEFT"));
        }
        final String encodedLink = Encoding.urlEncode(downloadLink.getDownloadURL());
        br.getPage(downloadLink.getDownloadURL());
        String vsKey = br.getRegex("type=\"hidden\" name=\"vs_key\" id=\"vs_key\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (vsKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        vsKey = Encoding.urlEncode(vsKey);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-MicrosoftAjax", "Delta=true");

        // Get file
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updLnkPobierz%7Cctl00%24ctnMetryka%24metrykaPliku%24lnkPobierz&ctl00_ScriptManager1_HiddenField=&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24lnkPobierz&__EVENTARGUMENT=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&ctl00%24ctnMetryka%24metrykaPliku%24Komentarze%24txtKomentarz=&__ASYNCPOST=true&");
        // Accept premium download and Get final link
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updPobieraniePliku%7Cctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierzPlatna&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&ctl00%24ctnMetryka%24metrykaPliku%24Komentarze%24txtKomentarz=&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierzPlatna&__EVENTARGUMENT=&__ASYNCPOST=true&");

        if (br.containsHTML("Przepraszamy, plik jest chwilowo niedostepny")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("TEMPORARY_UNAVAILABLE"), 60 * 1000L);
        }
        this.sleep(500, downloadLink);

        final String dllink = br.getRegex("\"(https?://[a-z0-9]+\\.5fantastic\\.pl/[^<>\"]*?download\\.php\\?[^<>\"]*?)\" id=\"ctl00_ctnMetryka_metrykaPliku_lnkSciagnijPlik\">").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("jest aktualnie pobierany inny plik")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("DOWNLOAD_DETECTED"), 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("FOLDERS_NOT_SUPPORTED", "The link seems to be folder. Folders are not supported.");
                                                      put("LINK_UNCHECKABLE", "(Link uncheckable - read Comment column)");
                                                      put("TRYING_TO_GET_PAGE", "FiveFantastic: trying to get page for link:");
                                                      put("GOT_ERROR", ",  got error: ");
                                                      put("ERROR", "Error: ");
                                                      put("PRIVATE_FILE", "This is private file - download impossible");
                                                      put("FILE_ERROR", "File not found or private!");
                                                      put("TEMPORARY_UNAVAILABLE", "Link temporary unavailable!");
                                                      put("DOWNLOAD_DETECTED", "Download in progress detected from your IP!");
                                                      put("PREMIUM_ERROR", "5Fantastic Premium Error");
                                                      put("LOGIN_FAILED", "Login failed or not Premium!\r\nPlease check your Username and Password!");
                                                      put("PREMIUM_INVALID", "Premium Account is invalid or not recognized!");
                                                      put("NO_TRAFFIC_LEFT", "No Premium traffic left!");
                                                      put("NO_PREMIUM", "Login failed or not Premium!");
                                                      put("PREMIUM_USER", "Premium User");
                                                  }
                                              };

    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("FOLDERS_NOT_SUPPORTED", "Wybrany link jest folderem. Foldery nie są obsługiwane");
                                                      put("LINK_UNCHECKABLE", "(Link nieweryfikowalny - sprawdź kolumnę Komentarz)");
                                                      put("TRYING_TO_GET_PAGE", "FiveFantastic: próba pobrania strony dla linku:");
                                                      put("GOT_ERROR", ",  zwrócony błąd: ");
                                                      put("ERROR", "Błąd: ");
                                                      put("PRIVATE_FILE", "Plik prywatny - pobieranie niemożliwe");
                                                      put("FILE_ERROR", "Plik nie znaleziony lub plik prywatny!");
                                                      put("TEMPORARY_UNAVAILABLE", "Link chwilowo niedostępny!");
                                                      put("DOWNLOAD_DETECTED", "Wykryto trwające pobieranie z twojego adresu IP!");
                                                      put("PREMIUM_ERROR", "5Fantastic: Błąd Konta Premium");
                                                      put("LOGIN_FAILED", "Błąd logowania lub konto nie jest Premium!\r\nSprawdź dane logowania: login/hasło!");
                                                      put("PREMIUM_INVALID", "Nieprawidłowe lub nierozpoznane konto Premium!");
                                                      put("NO_TRAFFIC_LEFT", "Brak dostępnego transferu Premium!");
                                                      put("NO_PREMIUM", "Błędny login lub brak Premium!");
                                                      put("PREMIUM_USER", "Użytkownik Premium");
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