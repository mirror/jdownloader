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

import jd.PluginWrapper;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file2upload.net" }, urls = { "http://[\\w\\.]*?file2upload\\.(net|com)/download/[0-9]+/" }, flags = { 2 })
public class File2UploadDotNet extends PluginForHost {

    public File2UploadDotNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://file2upload.net/membership?paid");
    }

    @Override
    public String getAGBLink() {
        return "http://file2upload.net/toc";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.file2upload.net");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("acc_login", Encoding.urlEncode(account.getUser()));
        form.put("acc_pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.setFollowRedirects(false);
        if (!br.containsHTML("Account area")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://file2upload.net/account");
        String hostedFiles = br.getRegex("<td><b>Hosted Files</b></td>.*?<td>(\\d+).*?</td>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        String expired = br.getRegex("<td><b>Expired\\?</b></td>.*?<td>.*?<span>(.*?)</span>").getMatch(0);
        account.setValid(true);
        if (expired != null && expired.contains("No")) {
            String expireDate = br.getRegex("<td><b>Package Expire Date</b></td>.*?<table.*?</table>.*?<span>(.*?)</span>").getMatch(0);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "dd.MM.yyyy", null));
        } else {
            ai.setExpired(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String passCode = null;
        if (br.containsHTML("Your package allow only")) {
            // sleep(5 * 60 * 1001l, downloadLink);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Too much parallel downloads", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (br.containsHTML("Enter password to download this file")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            Form pwform = br.getForm(1);
            if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            pwform.put("password", passCode);
            br.submitForm(pwform);
        }
        if (br.containsHTML("Wrong password")) {
            logger.warning("Wrong password");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        if (br.containsHTML("This file was expired")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dllink = br.getRegex("class=\"important\" href=\"(.*?)\">Click").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 2;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(false);
        if (br.containsHTML("File does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("content=\"The new file hosting service! we provides web space for your documents, pictures, music and movies., (.*?)\"></meta>").getMatch(0));
        String filesize = Encoding.htmlDecode(br.getRegex("<tr><td><b>File size</b>:</td><td>(.*?)</td></tr>").getMatch(0));
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        // This host got asecurity issue, if you enter the right pssword and the
        // wrong captcha (if a password is required), you can still download and
        // only if you DON'T have to enter a Password, the Captcha is required.
        if (!br.containsHTML("Enter password to download this file")) {
            String captchaurl = "http://file2upload.net/captcha.php";
            String code = getCaptchaCode(captchaurl, downloadLink);
            captchaForm.put("code", code);
        }
        if (br.containsHTML("Enter password to download this file")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            captchaForm.put("password", passCode);
        }
        br.submitForm(captchaForm);
        if (!br.containsHTML("download_link")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        if (br.containsHTML("This file was expired")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dllink = br.getRegex("class=\"important\" href=\"(.*?)\">Click to download! <").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}