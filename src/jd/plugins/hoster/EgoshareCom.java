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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "egoshare.com" }, urls = { "http://[\\w\\.]*?egoshare\\.com/download\\.php\\?id=[\\w]+" }, flags = { 2 })
public class EgoshareCom extends PluginForHost {

    private String captchaCode;
    private String passCode = null;
    private String url;

    public EgoshareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.egoshare.com/service.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.egoshare.com/faq.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("http://egoshare\\.com/", "http://www.egoshare.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.egoshare.com/", "king_mylang", "en");
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex(Pattern.compile("<title>.*?Your Data Recovery Solution -(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("You have requested <font.*?</font>(.*?).</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.egoshare.com/", "king_mylang", "en");
        br.getPage("http://www.egoshare.com/");
        br.postPage("http://www.egoshare.com/login.php", "act=login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&login=LOGIN");
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://www.egoshare.com/", "king_passhash") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage(br.getRedirectLocation());
        if (!br.getRegex("<td align=\"left\">.*?Premium Account.*?</td>").matches()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        String expired = br.getRegex("<b>Expired\\?</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (expired == null || !expired.trim().equalsIgnoreCase("no")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String finalUrl = null;
        br.getPage(downloadLink.getDownloadURL());
        finalUrl = br.getRegex("id=downloadfile style=\"display:none\">.*?<a href=\"(http.*?egoshare\\.com/getfile.*?)\"").getMatch(0);
        if (finalUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalUrl, true, 1);
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String validUntil = br.getRegex("Package Expire Date:</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            account.setValid(true);
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "MM/dd/yy", null));
        }
        String points = br.getRegex("Total Points:</b></td>.*?<td align=.*?>(\\d+)</td>").getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        return ai;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);

        /* CaptchaCode holen */
        captchaCode = getCaptchaCode("http://www.egoshare.com/captcha.php", downloadLink);
        Form form = br.getForm(1);
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }

        /* Überprüfen(Captcha,Password) */
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        if (br.containsHTML("Captcha number error or expired") || br.containsHTML("Unfortunately the password you entered is not correct")) {
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        /* Downloadlimit erreicht */
        if (br.containsHTML("max allowed download sessions") || br.containsHTML("this download is too big")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }

        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen, thx @dwd */
        String all = br.getRegex("eval\\(unescape\\(.*?\"\\)\\)\\);").getMatch(-1);
        String dec = br.getRegex("loadfilelink\\.decode\\(\".*?\"\\);").getMatch(-1);
        Context cx = ContextFactory.getGlobal().enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + dec + "} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        url = Context.toString(result);
        Context.exit();

        /* 5 seks warten */
        sleep(5000, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }
}
