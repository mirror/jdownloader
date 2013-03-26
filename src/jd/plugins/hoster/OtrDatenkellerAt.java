//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "otr.datenkeller.at" }, urls = { "http://(www\\.)?otr\\.datenkeller\\.at/\\?(file|getFile)=.+" }, flags = { 2 })
public class OtrDatenkellerAt extends PluginForHost {

    public static String  agent             = RandomUserAgent.generate();
    private final String  DOWNLOADAVAILABLE = "onclick=\"startCount";
    private final String  MAINPAGE          = "http://otr.datenkeller.at";
    private static Object LOCK              = new Object();

    public OtrDatenkellerAt(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        // Prevents premium problems
        this.setStartIntervall(15 * 1000l);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("getFile", "file").replaceAll("\\&referer=otrkeyfinder\\&lang=[a-z]+", ""));
    }

    @Override
    public String getAGBLink() {
        return "http://otr.datenkeller.at";
    }

    public String getDllink() throws Exception, PluginException {
        Regex allMatches = br.getRegex("onclick=\"startCount\\(\\d+, +\\d+, +\\'([^<>\"\\']+)\\', +\\'([^<>\"\\']+)\\', +\\'([^<>\"\\']+)\\'\\)");
        String firstPart = allMatches.getMatch(1);
        String secondPart = allMatches.getMatch(0);
        String thirdPart = allMatches.getMatch(2);
        if (firstPart == null || secondPart == null || thirdPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + firstPart + "/" + secondPart + "/" + thirdPart;
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String getDlpage(DownloadLink downloadLink) {
        return downloadLink.getDownloadURL().replace("?file=", "?getFile=");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dlPage = getDlpage(downloadLink);
        br.getPage(dlPage);
        String dllink = null;
        String lowSpeedLink;
        Browser br2 = br.cloneBrowser();
        if (br.containsHTML(DOWNLOADAVAILABLE)) {
            dllink = getDllink();
        } else {
            downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
            for (int i = 0; i <= 410; i++) {
                br.getPage(dlPage);
                String countMe = br.getRegex("\"(otrfuncs/countMe\\.js\\?r=\\d+)\"").getMatch(0);
                if (countMe != null)
                    countMe = "http://otr.datenkeller.at/" + countMe;
                else
                    countMe = "http://otr.datenkeller.at/otrfuncs/countMe.js?r=050210";
                br2.getPage("http://otr.datenkeller.at/images/style.css");
                br2.getPage(countMe);
                sleep(27 * 1000l, downloadLink);
                String position = br.getRegex("document\\.title = \"(\\d+/\\d+)").getMatch(0);
                if (position == null) position = br.getRegex("<td>Deine Position in der Warteschlange: </td><td>~(\\d+)</td></tr>").getMatch(0);
                if (position != null) downloadLink.getLinkStatus().setStatusText("Waiting for ticket...Position in der Warteschlange: " + position);
                if (br.containsHTML(DOWNLOADAVAILABLE)) {
                    br.getPage(dlPage);
                    dllink = getDllink();
                    break;
                }
                lowSpeedLink = br.getRegex("\"(\\?lowSpeed=[^<>\\'\"]+)\"").getMatch(0);
                if (i > 400 && lowSpeedLink != null) {
                    br2.getPage("http://otr.datenkeller.at/" + lowSpeedLink);
                    dllink = br2.getRegex(">Dein Download Link:<br>[\t\n\r ]+<a href=\"(http://[^<>\\'\"]+)\"").getMatch(0);
                    if (dllink == null) dllink = br2.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/low/[a-z0-9]+/[^<>\\'\"]+)\"").getMatch(0);
                    if (dllink != null) {
                        logger.info("Using lowspeed link for downloadlink: " + downloadLink.getDownloadURL());
                        break;
                    } else {
                        logger.warning("Failed to find low speed link, continuing to look for downloadticket...");
                    }
                }
                if (i > 403) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Didn't get a ticket");
                logger.info("Didn't get a ticket on try " + i + ". Retrying...Position: " + position);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", agent);
        br.getPage(link.getDownloadURL());
        if (!br.containsHTML("id=\"reqFileImg\"") && !br.containsHTML("\\(\\'#reqFile\\'\\)\\.hide\\(\\)\\.slideDown") && !br.containsHTML("onclick=\"window\\.open\\(\\'/\\?getFile")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = new Regex(link.getDownloadURL(), "otr\\.datenkeller\\.at/\\?file=(.+)").getMatch(0);
        String filesize = br.getRegex("Größe: </td><td align=\\'center\\'> (.*?) </td>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim().replaceAll("\\&referer=.*?", ""));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.replace("i", "")));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setDebug(true);
            br.setFollowRedirects(false);
            br.postPageRaw(MAINPAGE + "/index.php", "xjxfun=spenderLogin&xjxr=" + new Date().getTime() + "&xjxargs[]=S" + Encoding.urlEncode(account.getUser()) + "&xjxargs[]=S" + Encoding.urlEncode(account.getPass()));
            if (br.getCookie(MAINPAGE, "otrdat") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
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
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Spender Account OK");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        // Force login or download fails
        login(account, true);
        br.setFollowRedirects(false);
        br.getPage(getDlpage(link));
        String dllink = br.getRegex("type=\"text\" id=\"dlInp\" value=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://cluster\\.lastverteiler\\.net/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("wurde wegen Missbrauch geblockt")) {
                logger.info("Account wurde wegen Missbrauch geblockt.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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