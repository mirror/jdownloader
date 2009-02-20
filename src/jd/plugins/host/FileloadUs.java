//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
// import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
// import jd.plugins.Account;
// import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
// import jd.utils.JDLocale;

public class FileloadUs extends PluginForHost {

    private static final String AGB_LINK = "http://fileload.us/tos.html";
    // private int simultanpremium = 50;

    public FileloadUs (PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://fileload.us/premium.html");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.setDebug(true);
        Form[] Forms = br.getForms();
        Form Form1 = Forms[0];
        Form1.setAction(downloadLink.getDownloadURL());
        Form1.remove("method_premium");
        Form1.put("referer", Encoding.urlEncode(downloadLink.getDownloadURL()));
        br.submitForm(Form1);
        if (br.containsHTML("You have to wait")) {
            int minutes, seconds = 0;
            minutes = Integer.parseInt(br.getRegex("have\\s+to\\s+wait\\s+(\\d+)\\s+minutes").getMatch(0));
            seconds = Integer.parseInt(br.getRegex("minutes\\,\\s+(\\d+)\\s+seconds").getMatch(0));
            int waittime = ((60*minutes)+seconds+1)*1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        else {
            Forms = br.getForms();
            Form1 = br.getFormbyProperty("name", "F1");
            if (Form1 == null) throw new  PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            // TODO: AntiCaptcha Method would allow simultanous connections
            String captchaurl = br.getRegex(Pattern.compile("below:</b></td></tr>\\s+<tr><td><img src=\"(.*?)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            URLConnectionAdapter con = br.openGetConnection(captchaurl);
            File file = this.getLocalCaptchaFile(this);
            Browser.download(file, con);
            String code = Plugin.getCaptchaCode(file, this, downloadLink);
            Form1.put("code",code);
            Form1.setAction(downloadLink.getDownloadURL());
            this.sleep(15000, downloadLink);
            br.submitForm(Form1);
            URLConnectionAdapter con2 = br.getHttpConnection();
            if (con2.getContentType().contains("html")) {
                String error = br.getRegex("class=\"err\">(.*?)</font>").getMatch(0);
                logger.warning(error);
                if (error.equalsIgnoreCase("Wrong captcha") || error.equalsIgnoreCase("Session expired")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                else throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 10000);
            }
            dl = br.openDownload(downloadLink, br.getRedirectLocation());
            dl.startDownload();
        }
    }

    @Override
    // TODO: AntiCaptcha Method would allow simultanous connections
    // if user is quick; he can enter captchas one-by-one and then server allow him simulatanous downloads
    // that's why I left it 10.
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }
/*   //TODO: free_reg & premium accounts handling on JD users request
 
    public void handleFree0(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        String linkurl = br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(15000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.clearCookies("uploader.pl");
        br.getPage("http://uploader.pl/en/login.php");
        String cookie1 = br.getCookie("http://uploader.pl/", "yab_uid");
        String cookie2 = br.getCookie("http://uploader.pl/", "yab_passhash");
        String cookie3 = br.getCookie("http://uploader.pl/", "yab_logined");
        String cookie4 = br.getCookie("http://uploader.pl/", "yab_sess_id");
        String cookie5 = br.getCookie("http://uploader.pl/", "PHPSESSID");
        String cookie6 = br.getCookie("http://uploader.pl/", "yab_last_click");
        br.setCookie("http://uploader.pl/", "yab_uid", cookie1);
        br.setCookie("http://uploader.pl/", "yab_passhash", cookie2);
        br.setCookie("http://uploader.pl/", "yab_logined", cookie3);
        br.setCookie("http://uploader.pl/", "yab_sess_id", cookie4);
        br.setCookie("http://uploader.pl/", "PHPSESSID", cookie5);
        br.setCookie("http://uploader.pl/", "yab_last_click", cookie6);
        Form login = br.getForm(0);
        login.put("user", Encoding.urlEncode(account.getUser()));
        login.put("pass", Encoding.urlEncode(account.getPass()));
        login.put("autologin", "0");
        br.submitForm(login);
        cookie1 = br.getCookie("http://uploader.pl/", "yab_uid");
        cookie2 = br.getCookie("http://uploader.pl/", "yab_passhash");
        cookie3 = br.getCookie("http://uploader.pl/", "yab_logined");
        br.setCookie("http://uploader.pl/", "yab_uid", cookie1);
        br.setCookie("http://uploader.pl/", "yab_passhash", cookie2);
        br.setCookie("http://uploader.pl/", "yab_logined", cookie3);
        br.setCookie("http://uploader.pl/", "yab_sess_id", cookie4);
        br.setCookie("http://uploader.pl/", "PHPSESSID", cookie5);
        br.setCookie("http://uploader.pl/", "yab_last_click", cookie6);
        if (cookie1.equalsIgnoreCase("0")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    private boolean isPremium() throws IOException {
        br.getPage("http://uploader.pl/en/members.php?overview=1");
        if (br.containsHTML("package_info'\\)\"><b>Zareje")) return false;
        return true;
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setStatus(JDLocale.L("plugins.hoster.UploaderPl.freememberacc", "Free registered user account"));
            ai.setValid(true);
            return ai;
        }
        String expired = br.getRegex("<b>Expired\\?</b></td>\\s*<td[^>]*>(.*?) <a href").getMatch(0);
        if (!expired.equalsIgnoreCase("No")) {
            ai.setValid(false);
            ai.setStatus(JDLocale.L("plugins.hoster.UploaderPl.accountexpired", "Account expired"));
            return ai;
        }
        String expires = br.getRegex("<b>Package Expire Date</b></td>\\s*<td[^>]*>(.*?)</td>").getMatch(0);
        expires = expires.trim();
        if (!expires.equalsIgnoreCase("Never")) ai.setValidUntil(Regex.getMilliSeconds(expires, "mm/dd/yy", Locale.UK));
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        getFileInformation(downloadLink);
        login(account);
        if (!this.isPremium()) {
            simultanpremium = 20;
            handleFree0(downloadLink);
            return;
        } else {
            if (simultanpremium + 1 > 50) {
                simultanpremium = 50;
            } else {
                simultanpremium++;
            }
        }
        String linkurl = br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(30000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }
*/
    
    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        // this.setBrowserExclusive();
        br.getPage("http://fileload.us/?op=change_lang&lang=english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("You\\s+have\\s+requested:\\s+(.*?)\\s+-").getMatch(0));
        String filesize = br.getRegex("Size\\s+\\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}