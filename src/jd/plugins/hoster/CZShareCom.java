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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sdilej.cz", "czshare.com" }, urls = { "https?://(www\\.)?sdilej\\.cz/\\d+/.{1}", "fhirtogjnrogjmrogowcertvntzjuilthbfrwefdDELETE_MErvrgjzjz7ef" }, flags = { 2, 0 })
public class CZShareCom extends PluginForHost {

    private static AtomicInteger SIMULTANEOUS_PREMIUM = new AtomicInteger(-1);
    private static Object        LOCK                 = new Object();
    private static final String  MAINPAGE             = "http://sdilej.cz/";
    private static final String  CAPTCHATEXT          = "captcha\\.php";

    public CZShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sdilej.cz/registrace");
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
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    public Boolean rewriteHost(DownloadLink link) {
        if (link != null && "czshare.com".equals(link.getHost())) {
            link.setHost("sdilej.cz");
            return true;
        }
        return false;
    }

    public Boolean rewriteHost(Account acc) {
        if (acc != null && "czshare.com".equals(acc.getHoster())) {
            acc.setHoster("sdilej.cz");
            return true;
        }
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        /* Mark old links as offline */
        if (downloadLink.getDownloadURL().contains("czshare.com/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("/error.php?co=4") || br.containsHTML("Omluvte, prosím, výpadek databáze\\. Na opravě pracujeme")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<div class=\"left\\-col\">[\t\n\r ]+<h1>([^<>\"]*?)<span>\\&nbsp;</span>").getMatch(0);
        final String filesize = br.getRegex("Velikost: (.*?)<").getMatch(0);
        if (filename == null || filesize == null || "0 B".equals(filesize)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Set final filename here because server sends html encoded filenames
        downloadLink.setFinalFileName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".").replace(" ", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        final String trafficleft = br.getRegex("kredit: <strong>(.*?)</").getMatch(0);
        if (trafficleft == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }

        }
        ai.setTrafficLeft(trafficleft.replace(",", "."));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://sdilej.cz/VOP.pdf";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return SIMULTANEOUS_PREMIUM.get();
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML("Z Vaší IP adresy momentálně probíhá jiné stahování\\. Využijte PROFI")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.czsharecom.ipalreadydownloading", "IP already downloading"), 12 * 60 * 1001);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        handleErrors();
        if (!br.containsHTML("Stáhnout FREE(</span>)?</a><a href=\"/download\\.php\\?id=")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.CZShareCom.nofreeslots", "No free slots available"), 60 * 1000);
        }
        br.setFollowRedirects(true);
        String freeLink = br.getRegex("allowTransparency=\"true\"></iframe><a href=\"(/.*?)\"").getMatch(0);
        if (freeLink == null) {
            freeLink = br.getRegex("\"(/download\\.php\\?id=\\d+.*?code=.*?)\"").getMatch(0);
        }
        if (freeLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("http://sdilej.cz" + Encoding.htmlDecode(freeLink));
        handleErrors();
        String file = br.getRegex("name=\"file\" value=\"(.*?)\"").getMatch(0);
        String size = br.getRegex("name=\"size\" value=\"(\\d+)\"").getMatch(0);
        String server = br.getRegex("name=\"server\" value=\"(.*?)\"").getMatch(0);
        if (!br.containsHTML(CAPTCHATEXT) || file == null || size == null || server == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String code = getCaptchaCode("http://sdilej.cz/captcha.php", downloadLink);
        br.postPage("http://sdilej.cz/download.php", "id=" + new Regex(downloadLink.getDownloadURL(), "sdilej\\.cz/(\\d+)/.*?").getMatch(0) + "&file=" + file + "&size=" + size + "&server=" + server + "&captchastring2=" + Encoding.urlEncode(code) + "&freedown=Ov%C4%9B%C5%99it+a+st%C3%A1hnout");
        if (br.containsHTML("Chyba 6 / Error 6")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000);
        }
        if (br.containsHTML(">Zadaný ověřovací kód nesouhlasí") || br.containsHTML(CAPTCHATEXT)) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("<p class=\"button2\" id=\"downloadbtn\" style=\"display:none\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://www\\d+\\.sdilej\\.cz/download\\.php\\?id=[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Waittime can be skipped */
        // int wait = 50;
        // final String waittime =
        // br.getRegex("countdown_number = (\\d+);").getMatch(0);
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("unknown")) {
            br.followConnection();
            if (br.containsHTML("Soubor je dočasně nedostupný\\.") || dl.getConnection().getContentType().contains("unknown")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        String code = br.getRegex("<input type=\"hidden\" name=\"code\" value=\"(.*?)\"").getMatch(0);
        if (code == null) {
            code = br.getRegex("\\&amp;code=(.*?)\"").getMatch(0);
        }
        String linkID = new Regex(downloadLink.getDownloadURL(), "sdilej\\.cz/(\\d+)/").getMatch(0);
        if (linkID == null || code == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage("http://sdilej.cz/profi_down.php", "id=" + linkID + "&code=" + code);
        final String dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCustomCharset("utf-8");
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("SSL") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }

                br.setFollowRedirects(true);
                br.postPage("https://sdilej.cz/index.php", "trvale=on&Prihlasit=P%C5%99ihl%C3%A1sit+SSL&login-name=" + Encoding.urlEncode(account.getUser()) + "&login-password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "trvale") == null) {
                    final String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
            } catch (PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}