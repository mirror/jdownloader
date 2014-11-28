//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.config.Property;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tb7.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class Tb7Pl extends PluginForHost {

    private String                                         MAINPAGE           = "http://tb7.pl/";

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Object                                  LOCK               = new Object();

    public Tb7Pl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "login");
    }

    private void login(Account account, boolean force) throws PluginException, IOException {
        synchronized (LOCK) {
            try {
                br.postPage(MAINPAGE + "login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "autologin") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
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
        login(account, true);

        if (!br.getURL().contains("mojekonto")) {
            br.getPage("/mojekonto");
        }
        if (br.containsHTML("Brak ważnego dostępu Premium")) {
            ai.setExpired(true);
            ai.setStatus("Account expired");
            return ai;
        } else if (br.containsHTML(">Brak ważnego dostępu Premium<")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else {
            validUntil = br.getRegex("<div class=\"textPremium\">Dostęp Premium ważny do (.*?)<br>").getMatch(0);
            if (validUntil == null) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            validUntil = validUntil.replace(" | ", " ");
        }

        /*
         * unfortunatelly there is no list with supported hosts anywhere on the page only PNG image at the main page
         */
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList("turbobit.net", "catshare.net", "rapidu.net", "rapidgator.net", "rg.to", "uploaded.to", "uploaded.net", "ul.to", "oboom.com", "fileparadox.in", "netload.in", "bitshare.com", "freakshare.net", "freakshare.com", "uploadable.ch", "lunaticfiles.com", "fileshark.pl"));
        long expireTime = TimeFormatter.getMilliSeconds(validUntil, "dd.MM.yyyy HH:mm", Locale.ENGLISH);
        ai.setValidUntil(expireTime);
        account.setValid(true);
        ai.setMultiHostSupport(this, supportedHosts);
        ai.setProperty("Turbobit traffic", "Unlimited");
        final String otherHostersLimitLeft = br.getRegex(" Pozostały limit na serwisy dodatkowe: ([^<>\"\\']+)<br />").getMatch(0);
        ai.setProperty("Other hosters traffic", SizeFormatter.getSize(otherHostersLimitLeft));
        ai.setStatus("Premium User (TB: unlimited," + " Other: " + otherHostersLimitLeft + ")");

        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "regulamin";
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
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        boolean resume = true;
        showMessage(link, "Phase 1/3: Login");

        login(acc, false);
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        dl = null;
        // each time new download link is generated
        // (so even after user interrupted download) - transfer
        // is reduced, so:
        // first check if the property generatedLink was previously generated
        // if so, then try to use it, generated link store in link properties
        // for future usage (broken download etc)
        String generatedLinkTb7 = (String) link.getProperty("generatedLinkTb7");

        String generatedLink = (generatedLinkTb7 == null) ? null : generatedLinkTb7;

        if (generatedLink == null) {

            /* generate new downloadlink */
            String url = Encoding.urlEncode(link.getDownloadURL());
            String postData = "step=1" + "&content=" + url;
            showMessage(link, "Phase 2/3: Generating Link");
            br.postPage(MAINPAGE + "mojekonto/sciagaj", postData);
            if (br.containsHTML("Wymagane dodatkowe [0-9.]+ MB limitu")) {
                logger.severe("Tb7.pl(Error): " + br.getRegex("(Wymagane dodatkowe [0-9.]+ MB limitu)"));
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download limit exceeded!", 1 * 60 * 1000l);
            }
            postData = "step=2" + "&0=on";
            br.postPage(MAINPAGE + "mojekonto/sciagaj", postData);

            // New Regex, but not tested if it works for all files (not video)
            // String generatedLink =
            // br.getRegex("<div class=\"download\">(<a target=\"_blank\" href=\"mojekonto/ogladaj/[0-9A-Za-z]*?\">Oglądaj online</a> / )*?<a href=\"([^\"<>]+)\" target=\"_blank\">Pobierz</a>").getMatch(1);
            // Old Regex
            generatedLink = br.getRegex("<div class=\"download\"><a href=\"([^\"<>]+)\" target=\"_blank\">Pobierz</a>").getMatch(0);
            if (generatedLink == null) {
                // New Regex (works with video files)
                generatedLink = br.getRegex("<div class=\"download\">(<a target=\"_blank\" href=\"mojekonto/ogladaj/[0-9A-Za-z]*?\">Oglądaj online</a> / )<a href=\"([^\"<>]+)\" target=\"_blank\">Pobierz</a>").getMatch(1);
            }
            if (generatedLink == null) {
                logger.severe("Tb7.pl(Error): " + generatedLink);
                //
                // after x retries we disable this host and retry with normal plugin
                // but because traffic limit is decreased even if there's a problem
                // with download (seems like bug) - we limit retries to 2
                //
                if (link.getLinkStatus().getRetryCount() >= 2) {
                    try {
                        // disable hoster for 30min
                        tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                    } catch (Exception e) {
                    }
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 2 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
            }
            link.setProperty("generatedLinkTb7", generatedLink);
        }

        // wait, workaround
        sleep(1 * 1000l, link);
        int chunks = 0;

        // generated fileshark link allows only 1 chunk
        // because download doesn't support more chunks and
        // and resume (header response has no: "Content-Range" info)
        if (link.getBrowserUrl().contains("fileshark.pl") || link.getDownloadURL().contains("fileshark.pl")) {
            chunks = 1;
            resume = false;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedLink, resume, chunks);
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) // unknown
        // error
        {
            br.followConnection();
            if (br.containsHTML("<div id=\"message\">Ważność linka wygasła.</div>")) {
                // previously generated link expired,
                // clear the property and restart the download
                // and generate new link
                sleep(10 * 1000l, link, "Previously generated Link expired!");
                logger.info("Tb7.pl: previously generated link expired - removing it and restarting download process.");
                link.setProperty("generatedLinkTb7", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

            if (br.getBaseURL().contains("notransfer")) {
                /* No traffic left */
                acc.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "No traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.getBaseURL().contains("serviceunavailable")) {
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
            if (br.getBaseURL().contains("connecterror")) {
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
            if (br.getBaseURL().contains("invaliduserpass")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (br.getBaseURL().contains("notfound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("Wymagane dodatkowe [0-9.]+ MB limitu")) {
                logger.severe("Tb7.pl(Error): " + br.getRegex("(Wymagane dodatkowe [0-9.]+ MB limitu)"));
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download limit exceeded!", 1 * 60 * 1000l);
            }

        }

        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            tempUnavailableHoster(acc, link, 20 * 60 * 1000l);
        }
        showMessage(link, "Phase 3/3: Begin download");
        dl.startDownload();
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
    }

    public void showAccountDetailsDialog(Account account) {
        AccountInfo ai = account.getAccountInfo();
        long otherHostersLimit = Long.parseLong(ai.getProperty("Other hosters traffic").toString(), 10);
        jd.gui.UserIO.getInstance().requestMessageDialog("Tb7.pl Account", "Account type: Premium\n" + "TurboBit limit: " + ai.getProperty("Turbobit traffic") + "\nOther hosters limit: " + SizeFormatter.formatBytes(otherHostersLimit));
    }

}