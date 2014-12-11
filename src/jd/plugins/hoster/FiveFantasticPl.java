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

        try {
            br.getPage(link.getDownloadURL());

        } catch (Exception e) {
            if (e.getCause().getMessage().contains("Content-length too big")) {
                logger.severe("FiveFantastic: the link: " + link.getDownloadURL() + " seems to be folder. Folders are not supported!");
                // link.setFinalLinkState(FinalLinkState.FAILED_FATAL);
                // link.setSkipReason(SkipReason.INVALID_DESTINATION);
                link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                throw new PluginException(LinkStatus.ERROR_FATAL, "folders are not supported!");
                //

            }

            return AvailableStatus.UNCHECKABLE;
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
            br.getPage(link.getDownloadURL());

            if (br.containsHTML("Plik jest plikiem prywatnym.")) {

                logger.info("FiveFantastic: " + link.getDownloadURL() + " is private file - download impossible!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }

        if (br.containsHTML("<title>[\t\n\r ]+5fantastic\\.pl \\- najlepszy darmowy dysk internetowy \\- pliki udostępnione przez klubowiczów[\t\n\r ]+</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File not found or private!");
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
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Link temporary unavailable!", 60 * 1000L);
        }
        this.sleep(3000, downloadLink);

        // Select FREE download with speed limit
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updPobieraniePliku%7Cctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierz&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierz&__EVENTARGUMENT=&__ASYNCPOST=true&");

        this.sleep(3000, downloadLink);
        // Confirm TOS and get final link
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updPobieraniePliku%7Cctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&ctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin=on&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin&__EVENTARGUMENT=&__LASTFOCUS=&__ASYNCPOST=true&");

        if (br.containsHTML("Z Twojego numeru IP jest już pobierany plik. Poczekaj na zwolnienie zasobów, albo ściągnij plik za punkty")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download in progress detected from your IP!", 5 * 60 * 1000l);
        }
        final String dllink = br.getRegex("\"(https?://[a-z0-9]+\\.5fantastic\\.pl/[^<>\"]*?download\\.php\\?[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("jest aktualnie pobierany inny plik")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download in progress detected from your IP!", 5 * 60 * 1000l);
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
            ai.setStatus("Login failed or not Premium");
            UserIO.getInstance().requestMessageDialog(0, "5Fantastic Premium Error", "Login failed or not Premium!\r\nPlease check your Username and Password!");
            account.setValid(false);
            return ai;
        }

        final String[][] limits = br.getRegex("<div id=\"ctl00_updpunktyint\">[ \t\n\r]+<div style=\".*?\">[ \t\n\r]+<span id=\".*?\" style=\".*?\">[ \t\n\r]+([0-9]+)[ \t\n\r]+</span>[ \t\n\r]+</div>[ \t\n\r]+</div>[ \t\n\r]+<div style=\".*?punkty /([A-Za-z]+) transferu</div>").getMatches();
        if (limits.length != 0) {
            final String maxLimit = limits[0][0];

            final String unit = limits[0][1];
            // ai.setTrafficMax(SizeFormatter.getSize(maxLimit + " " + unit));
            ai.setTrafficLeft(SizeFormatter.getSize(maxLimit + " " + unit));

        } else {
            ai.setUnlimitedTraffic();
        }

        account.setValid(true);
        try {
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }

        ai.setStatus("Premium User");
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
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Premium Account is invalid or not recognized!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        login(account, true);
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
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Link temporary unavailable!", 60 * 1000L);
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
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download in progress detected from your IP!", 5 * 60 * 1000l);
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

}