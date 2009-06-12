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

package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

public class UploadingCom extends PluginForHost {
    private static int simultanpremium = 1;

    public UploadingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://www.uploading.com/premium/");
    }

    // @Override
    public String getAGBLink() {
        return "http://uploading.com/terms/";
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.uploading.com/");
        if (br.containsHTML("UPGRADE TO PREMIUM")) return false;
        if (br.containsHTML("Premium account")) return true;
        return false;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();        
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.getPage("http://www.uploading.com/");
        br.getPage("http://www.uploading.com/login/");
        br.postPage("http://www.uploading.com/login/", "log_ref=&login=" + Encoding.urlEncode(account.getUser()) + "&pwd=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://www.uploading.com/", "ulogin") == null || br.getCookie("http://www.uploading.com/", "upass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    // @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setValid(true);
            ai.setStatus("Free Membership");
            return ai;
        }
        br.getPage("http://www.uploading.com/profile/");
        String validUntil = br.getRegex("Premium Account access is valid until (.*?)\\.").getMatch(0);
        ai.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyy-MM-dd", null));
        ai.setValid(true);
        return ai;
    }

    // @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        if (!isPremium()) {
            simultanpremium = 1;
            handleFree0(link);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        br.getPage(link.getDownloadURL());
        Form form = br.getForm(2);
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = br.openDownload(link, form, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            dl = br.openDownload(link, form, true, 1);
        }
        dl.startDownload();
    }

    public void handleFree0(DownloadLink link) throws Exception {
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("YOU REACHED YOUR COUNTRY DAY LIMIT")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDLocale.L("plugins.hoster.uploadingcom.errors.countrylimitreached", "You reached your country daily limit"), 60 * 60 * 1000l);
        Form form = br.getFormbyProperty("id", "downloadform");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        br.submitForm(form);
        this.sleep(70000l, link);
        br.setFollowRedirects(false);
        form = br.getFormbyProperty("id", "downloadform");
        br.submitForm(form);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.setFollowRedirects(true);
        dl = br.openDownload(link, br.getRedirectLocation(), false, 1);
        dl.startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        try {
            setBrowserExclusive();
            br.setFollowRedirects(true);
            br.setCookie("http://www.uploading.com/", "_lang", "en");
            br.setCookie("http://www.uploading.com/", "setlang", "en");
            br.getPage(downloadLink.getDownloadURL());
            br.cloneBrowser().getPage("http://img.uploading.com/css/blue.main.css");
            String quant = br.getRegex("<img src=\"(http://pixel.quantserve.com/pixel/.*?)\"").getMatch(0);
            Browser brc = br.cloneBrowser();
            URLConnectionAdapter con = brc.openGetConnection(quant);
            con.disconnect();
            con = brc.openGetConnection("http://img.uploading.com/bb_bg_big.png");
            con.disconnect();
            String filesize = br.getRegex("File size:(.*?)<br").getMatch(0);
            String filename = br.getRegex(Pattern.compile("Download file.*?<b>(.*?)</b>", Pattern.DOTALL)).getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<b>(.*?)</b>  File size").getMatch(0);

            }
            if (filesize == null || filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
            return AvailableStatus.TRUE;
        } catch (Exception e) {

            throw e;
        }
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if (br.containsHTML("YOU REACHED YOUR COUNTRY DAY LIMIT")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDLocale.L("plugins.hoster.uploadingcom.errors.countrylimitreached", "You reached your country daily limit"), 60 * 60 * 1000l);
        Form form = br.getFormbyProperty("id", "downloadform");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        br.submitForm(form);
        this.sleep(100000l, downloadLink);
        br.setFollowRedirects(false);
        form = br.getFormbyProperty("id", "downloadform");
        br.submitForm(form);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), false, 1);
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    // @Override
    public int getTimegapBetweenConnections() {
        return 100;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    // @Override
    public void resetPluginGlobals() {

    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}
