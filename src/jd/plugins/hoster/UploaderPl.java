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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.Locale;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploader.pl" }, urls = { "http://[\\w\\.]*?uploader.pl/([a-z]{2}/)?file/\\d+/" }, flags = { 2 })
public class UploaderPl extends PluginForHost {

    private int simultanpremium = 1;
    private static final Object PREMLOCK = new Object();

    public UploaderPl(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://uploader.pl/register.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/pl/", "/en/"));
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        handleFree0(downloadLink);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void handleFree0(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        String linkurl = br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.clearCookies("uploader.pl");
        br.getPage("http://uploader.pl/en/login.php");
        Form login = br.getForm(0);
        login.put("user", Encoding.urlEncode(account.getUser()));
        login.put("pass", Encoding.urlEncode(account.getPass()));
        login.put("autologin", "0");
        br.submitForm(login);
        String cookie1 = br.getCookie("http://uploader.pl/", "yab_uid");
        if (cookie1 == null || cookie1.equalsIgnoreCase("0")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private boolean isPremium() throws IOException {
        br.getPage("http://uploader.pl/en/members.php?overview=1");
        if (br.containsHTML("package_info'\\)\"><b>Zareje")) return false;
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setStatus(JDL.L("plugins.hoster.UploaderPl.freememberacc", "Free registered user account"));
            account.setValid(true);
            return ai;
        }
        String expired = br.getRegex("<b>Expired\\?</b></td>\\s*<td[^>]*>(.*?) <a href").getMatch(0);
        if (!expired.equalsIgnoreCase("No")) {
            account.setValid(false);
            ai.setStatus(JDL.L("plugins.hoster.UploaderPl.accountexpired", "Account expired"));
            return ai;
        }
        String expires = br.getRegex("<b>Package Expire Date</b></td>\\s*<td[^>]*>(.*?)</td>").getMatch(0);
        expires = expires.trim();
        if (!expires.equalsIgnoreCase("Never")) ai.setValidUntil(Regex.getMilliSeconds(expires, "mm/dd/yy", Locale.UK));
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        synchronized (PREMLOCK) {
            return simultanpremium;
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        boolean free = false;
        synchronized (PREMLOCK) {
            requestFileInformation(downloadLink);
            login(account);
            if (!this.isPremium()) {
                simultanpremium = 10;
                free = true;
            } else {
                if (simultanpremium + 1 > 20) {
                    simultanpremium = 20;
                } else {
                    simultanpremium++;
                }
            }
        }
        if (free) {
            handleFree0(downloadLink);
        } else {
            String linkurl = br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
            if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public String getAGBLink() {
        return "http://uploader.pl/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://uploader.pl/en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("has already been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("File name:</b></td>\\s*<td[^>]*>\\s*(.+?)\\s*</td>").getMatch(0));
        String filesize = br.getRegex("File size:</b></td>\\s*<td[^>]*>\\s*(.+?)\\s*</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName((filename));
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
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
