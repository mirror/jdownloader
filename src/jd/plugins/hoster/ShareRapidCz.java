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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-rapid.cz" }, urls = { "http://(www\\.)?(share\\-rapid\\.(biz|com|info|cz|eu|info|net|sk)|((mediatack|rapidspool|e\\-stahuj|premium\\-rapidshare|qiuck|rapidshare\\-premium|share\\-credit|srapid|share\\-free)\\.cz)|((strelci|share\\-ms|)\\.net)|jirkasekyrka\\.com|((kadzet|universal\\-share)\\.com)|sharerapid\\.(biz|cz|net|org|sk)|stahuj\\-zdarma\\.eu|share\\-central\\.cz|rapids\\.cz)/stahuj/([0-9]+/.+|[a-z0-9]+)" }, flags = { 2 })
public class ShareRapidCz extends PluginForHost {

    private static AtomicInteger maxPrem  = new AtomicInteger(1);

    private static final String  MAINPAGE = "http://share-rapid.com/";

    public ShareRapidCz(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://share-rapid.com/dobiti/?zeme=1");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        // Complete list of all domains, maybe they buy more....
        // http://share-rapid.com/informace/
        String downloadlinklink = link.getDownloadURL();
        if (downloadlinklink != null) {
            downloadlinklink = downloadlinklink.replaceAll("(share-rapid\\.(biz|com|info|cz|eu|info|net|sk)|((mediatack|rapidspool|e\\-stahuj|premium\\-rapidshare|qiuck|rapidshare\\-premium|share\\-credit|share\\-free|srapid)\\.cz)|((strelci|share\\-ms|)\\.net)|jirkasekyrka\\.com|((kadzet|universal\\-share)\\.com)|sharerapid\\.(biz|cz|net|org|sk)|stahuj\\-zdarma\\.eu|share\\-central\\.cz|rapids\\.cz)", "share-rapid.com");
        }
        link.setUrlDownload(downloadlinklink);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        long realTraffic = 0l;
        String trafficleft = null;
        /**
         * Expire unlimited -> Unlimited traffic for a specified amount of time Normal expire -> Expire date + trafficleft
         * 
         * */
        final String expireUnlimited = br.getRegex("<td>Paušální stahování aktivní\\. Vyprší </td><td><strong>([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4} - [0-9]{1,2}:[0-9]{1,2})</strong>").getMatch(0);
        if (expireUnlimited != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireUnlimited, "dd.MM.yy - HH:mm", Locale.ENGLISH));
            ai.setUnlimitedTraffic();
            /** Remove property in case account was a free acount but changes to */
            account.setProperty("freeaccount", false);
            ai.setStatus("Premium User with unlimited traffic");
            account.setValid(true);
            return ai;
        } else {
            trafficleft = br.getMatch("<td>GB:</td><td>([^<>\"]*?)<a");
            if (trafficleft != null) {
                logger.info("Available traffic equals: " + trafficleft);
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
                realTraffic = SizeFormatter.getSize(trafficleft);
                trafficleft = ", " + trafficleft.trim() + " traffic left";
            } else {
                trafficleft = "";
                ai.setUnlimitedTraffic();
            }
            final String expires = br.getMatch("Neomezený tarif vyprší</td><td><strong>([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4} - [0-9]{1,2}:[0-9]{1,2})</strong>");
            if (expires != null) ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "dd.MM.yy - HH:mm", Locale.ENGLISH));
        }
        if (realTraffic > 0l) {
            /**
             * Max simultan downloads (higher than 1) only works if you got any
             */
            ai.setStatus("Premium User" + trafficleft);
            /** Remove property in case account was a free acount but changes to */
            account.setProperty("freeaccount", false);
            final String maxSimultanDownloads = br.getRegex("<td>Max\\. počet paralelních stahování: </td><td>(\\d+) <a href").getMatch(0);
            if (maxSimultanDownloads != null) {
                try {
                    final int maxSimultan = Integer.parseInt(maxSimultanDownloads);
                    maxPrem.set(maxSimultan);
                    account.setMaxSimultanDownloads(maxSimultan);
                } catch (final Throwable e) {
                    /* not available in 0.9xxx */
                }
            } else {
                try {
                    account.setMaxSimultanDownloads(1);
                } catch (final Throwable e) {
                    /* not available in 0.9xxx */
                }
            }
            try {
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in 0.9xxx */
            }
        } else {
            ai.setStatus("Registered (free) User");
            account.setProperty("freeaccount", true);
            try {
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in 0.9xxx */
            }
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://share-rapid.com/informace/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Disk, na kterém se soubor nachází, je dočasně odpojen, zkuste to prosím později")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is on a damaged hard drive disk", 60 * 60 * 1000); }
        if (br.containsHTML("Soubor byl chybně nahrán na server")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file isn't uploaded correctly", 60 * 60 * 1000); }
        final String dllink = br.getRegex("\"(http://s[0-9]{1,2}\\.share-rapid\\.com/download.*?)\"").getMatch(0);
        if (dllink == null && br.containsHTML("(Stahování je přístupné pouze přihlášeným uživatelům|class=\"error_div\"><strong>Stahov)")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
        }
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "Please contact the support jdownloader.org"); }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Disk, na kterém se soubor nachází, je dočasně odpojen, zkuste to prosím později")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is on a damaged hard drive disk", 60 * 60 * 1000); }
        if (br.containsHTML("Soubor byl chybně nahrán na server")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file isn't uploaded correctly", 60 * 60 * 1000); }
        if (br.containsHTML("Již Vám došel kredit a vyčerpal jste free limit")) {
            logger.info("share-rapid.cz: Not enough traffic left!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        String dllink = br.getRegex("\"(http://s[0-9]{1,2}\\.share-rapid\\.com/download.*?)\"").getMatch(0);
        boolean nonTrafficPremium = false;
        if (dllink == null) {
            if (br.containsHTML(">Stahování zdarma je možné jen přes náš")) {
                nonTrafficPremium = true;
            }
        }
        if (nonTrafficPremium == true || (dllink == null && account.getBooleanProperty("freeaccount"))) {
            final Browser br2 = new Browser();
            br2.getHeaders().put("User-Agent", "share-rapid downloader");
            br2.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br2.getHeaders().put("Accept-Charset", "iso-8859-1, utf-8, utf-16");
            br2.getHeaders().put("Accept-Encoding", "deflate, gzip, identity");
            br2.getHeaders().put("Accept-Language", "en");
            br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
            br2.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));

            br2.getPage("http://share-rapid.com/userinfo.php");
            br2.getPage("http://share-rapid.com/login.php");

            br2.getHeaders().put("Accept", "*/*");

            br2.postPageRaw("http://share-rapid.com/checkfiles.php", "files=" + Encoding.urlEncode(downloadLink.getDownloadURL()));
            br = br2.cloneBrowser();
            dllink = downloadLink.getDownloadURL();
        }
        if (dllink == null) {
            if (br.containsHTML(">Stahování zdarma je možné jen přes náš")) {
                logger.info("share-rapid.cz: No traffic left, disabling premium...");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("(was not found on this server|No htmlCode read)")) {
                /** Show other errormessage if free account was used */
                if (account.getBooleanProperty("freeaccount")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharerapidcz.maybenofreedownloadpossible", "Error: Maybe this file cannot be downloaded as a freeuser: Buy traffic or try again later"), 60 * 60 * 1000);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        br.setDebug(true);
        br.setCookie(MAINPAGE, "lang", "cs");
        br.getPage("http://share-rapid.com/prihlaseni/");
        final String lang = System.getProperty("user.language");
        final Form form = br.getForm(0);
        if (form == null) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        form.put("login", Encoding.urlEncode(account.getUser()));
        form.put("pass1", Encoding.urlEncode(account.getPass()));
        form.remove("remember");
        br.submitForm(form);
        if (!br.containsHTML("<td>GB:</td>")) {
            br.getPage("http://share-rapid.com/mujucet/");
            if (!br.containsHTML("<td>GB:</td>")) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setCookie(MAINPAGE, "lang", "cs");
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(true);
        if (br.containsHTML("Nastala chyba 404") || br.containsHTML("Soubor byl smazán")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = Encoding.htmlDecode(br.getRegex("style=\"padding: 12px 0px 0px 10px; display: block\">(.*?)</ br>").getMatch(0));
        if (filename == null) {
            filename = Encoding.htmlDecode(br.getRegex("<title>(.*?)- Share-Rapid</title>").getMatch(0));
        }
        final String filesize = Encoding.htmlDecode(br.getRegex("Velikost:</td>.*?<td class=\"h\"><strong>.*?(.*?)</strong></td>").getMatch(0));
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}