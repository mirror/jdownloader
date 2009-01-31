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

import java.io.IOException;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class UploaderPl extends PluginForHost {

    private static final String AGB_LINK = "http://uploader.pl/rules.php";
    private int simultanpremium = 50;

    public UploaderPl(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://uploader.pl/register.php");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        String linkurl =  br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(30000, downloadLink); // uncomment when they find a better way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    public void handleFree0(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        String linkurl =  br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(15000, downloadLink); // uncomment when they find a better way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

    private void login(Account account) throws IOException, PluginException {
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
        String linkurl =  br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(30000, downloadLink); // uncomment when they find a better way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        //this.setBrowserExclusive();
        br.getPage("http://uploader.pl/en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("has already been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("File name:</b></td>\\s*<td[^>]*>\\s*(.+?)\\s*</td>").getMatch(0));
        String filesize = br.getRegex("File size:</b></td>\\s*<td[^>]*>\\s*(.+?)\\s*</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4384 $");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}