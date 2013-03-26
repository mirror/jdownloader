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
import java.util.Locale;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "czshare.com" }, urls = { "http://(www\\.)?(czshare\\.com/((files/)?\\d+/[A-Za-z0-9_\\.\\-]+(/.{1})?|download_file\\.php\\?id=\\d+\\&code=[A-Za-z0-9\\-]+)|www\\d+\\.czshare\\.com/profi\\.php\\?id=\\d+\\&kod=[A-Za-z0-9\\-]+)" }, flags = { 2 })
public class CZShareCom extends PluginForHost {

    private static AtomicInteger SIMULTANEOUS_PREMIUM = new AtomicInteger(-1);
    private static Object        LOCK                 = new Object();
    private static final String  MAINPAGE             = "http://czshare.com/";
    private static final String  CAPTCHATEXT          = "captcha\\.php";

    public CZShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://czshare.com/create_user.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        Regex linkInfo = new Regex(link.getDownloadURL(), "czshare\\.com/download_file\\.php\\?id=(\\d+)\\&code=([A-Za-z0-9]+)");
        if (linkInfo.getMatch(0) == null && linkInfo.getMatch(1) == null) {
            linkInfo = new Regex(link.getDownloadURL(), "czshare\\.com/(\\d+)/([A-Za-z0-9_]+)/");
            if (linkInfo.getMatch(0) == null && linkInfo.getMatch(1) == null) {
                linkInfo = new Regex(link.getDownloadURL(), ".*?czshare\\.com/profi\\.php\\?id=(\\d+)\\&kod=([A-Za-z0-9]+)");
            }
        }
        if (linkInfo.getMatch(0) != null && linkInfo.getMatch(1) != null) link.setUrlDownload("http://czshare.com/" + linkInfo.getMatch(0) + "/" + linkInfo.getMatch(1) + "/x");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String trafficleft = br.getRegex("kredit: <strong>(.*?)</").getMatch(0);
        // Regex probably broken
        String expires = br.getRegex("Velikost kreditu.*?Platnost do</td>.*?<td>.*?<td>(.*?)</td>").getMatch(0);
        if (expires != null && !"neomezená".equals(expires)) ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "dd.MM.yy HH:mm", Locale.GERMANY));
        if (trafficleft != null) ai.setTrafficLeft(trafficleft.replace(",", "."));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.czshare.com/pravidla.html";
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
        if (br.containsHTML("Z Vaší IP adresy momentálně probíhá jiné stahování\\. Využijte PROFI")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.czsharecom.ipalreadydownloading", "IP already downloading"), 12 * 60 * 1001);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        handleErrors();
        if (!br.containsHTML("Stáhnout FREE(</span>)?</a><a href=\"/download\\.php\\?id=")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.CZShareCom.nofreeslots", "No free slots available"), 60 * 1000);
        br.setFollowRedirects(true);
        String freeLink = br.getRegex("allowTransparency=\"true\"></iframe><a href=\"(/.*?)\"").getMatch(0);
        if (freeLink == null) freeLink = br.getRegex("\"(/download\\.php\\?id=\\d+.*?code=.*?)\"").getMatch(0);
        if (freeLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://czshare.com" + Encoding.htmlDecode(freeLink));
        handleErrors();
        String file = br.getRegex("name=\"file\" value=\"(.*?)\"").getMatch(0);
        String size = br.getRegex("name=\"size\" value=\"(\\d+)\"").getMatch(0);
        String server = br.getRegex("name=\"server\" value=\"(.*?)\"").getMatch(0);
        if (!br.containsHTML(CAPTCHATEXT) || file == null || size == null || server == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode("http://czshare.com/captcha.php", downloadLink);
        br.postPage("http://czshare.com/download.php", "id=" + new Regex(downloadLink.getDownloadURL(), "czshare\\.com/(\\d+)/.*?").getMatch(0) + "&file=" + file + "&size=" + size + "&server=" + server + "&captchastring2=" + Encoding.urlEncode(code) + "&freedown=Ov%C4%9B%C5%99it+a+st%C3%A1hnout");
        if (br.containsHTML("Chyba 6 / Error 6")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000);
        if (br.containsHTML(">Zadaný ověřovací kód nesouhlasí") || br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("<p class=\"button2\" id=\"downloadbtn\" style=\"display:none\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.czshare\\.com/download\\.php\\?id=[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Waittime can be skipped */
        // int wait = 50;
        // final String waittime =
        // br.getRegex("countdown_number = (\\d+);").getMatch(0);
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("unknown")) {
            br.followConnection();
            if (br.containsHTML("Soubor je dočasně nedostupný\\.") || dl.getConnection().getContentType().contains("unknown")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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
        if (code == null) code = br.getRegex("\\&amp;code=(.*?)\"").getMatch(0);
        String linkID = new Regex(downloadLink.getDownloadURL(), "czshare\\.com/(\\d+)/").getMatch(0);
        if (linkID == null || code == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://czshare.com/profi_down.php", "id=" + linkID + "&code=" + code);
        String dllink = br.getRedirectLocation();
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

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCustomCharset("utf-8");
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
                br.postPage("http://czshare.com/index.php", "login-name=" + Encoding.urlEncode(account.getUser()) + "&login-password=" + Encoding.urlEncode(account.getPass()) + "&trvale=on&Prihlasit=P%C5%99ihl%C3%A1sit");
                if (br.getCookie(MAINPAGE, "trvale") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("/error.php?co=4") || br.containsHTML("Omluvte, prosím, výpadek databáze\\. Na opravě pracujeme")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        /** First regex is only for video links */
        String filename = br.getRegex("onmouseover=\"video_thumb_start\\(this,\\'http://www\\d+\\.czshare\\.com/images_velke\\',\\'\\d+\\'\\)\" title=\"([^<>\"/]+)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("page-download\"><img src.*?alt=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = Encoding.htmlDecode(br.getRegex("<div class=\"left\\-col\">[\t\n\r ]+<h1>(.*?)<span>\\&nbsp;</span></h1>").getMatch(0));
            if (filename == null) filename = Encoding.htmlDecode(br.getRegex("<title>(.*?) CZshare\\.com download</title>").getMatch(0));
        }
        String filesize = br.getRegex("Velikost: (.*?)<").getMatch(0);
        if (filename == null || filesize == null || "0 B".equals(filesize)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Set final filename here because server sends html encoded filenames
        downloadLink.setFinalFileName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".").replace(" ", "")));
        return AvailableStatus.TRUE;
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
}