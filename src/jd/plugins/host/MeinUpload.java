//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;

public class MeinUpload extends PluginForHost {

    private static final String AGB_LINK = "http://meinupload.com/#help.html";

    public MeinUpload(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://meinupload.com/register.php?g=2");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.host.meinupload.error." + error, error));
        }
        br.setDebug(true);
        Form form = br.getFormbyValue("Free");

        if (form != null) {
            // Old version 1.9.08
            br.submitForm(form);

            Form captcha = br.getForms()[1];
            String captchaCode = getCaptchaCode("http://meinupload.com/captcha.php", downloadLink);
            captcha.put("captchacode", captchaCode);
            br.submitForm(captcha);
        }
        String url = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);

        dl = new RAFDownload(this, downloadLink, br.createRequest(url));
        dl.connect(br);

        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.host.meinupload.serverdefect", "Serivce not available"), 10 * 60 * 1000l);

        }
        dl.startDownload();
    }

    private void login(Account account) throws IOException {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.clearCookies(getHost());

        br.getPage("http://meinupload.com/index.php");
        Form login = br.getFormbyValue("Login");
        login.put("act", "login");
        login.put("user", account.getUser());
        login.put("pass", account.getPass());
        br.submitForm(login);
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);

        login(account);
        br.getPage("http://meinupload.com/members.php");
        String expire = br.getRegex("<b>Packet runs out on</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (expire == null) {
            ai.setValid(false);
            ai.setStatus("Account invalid. Logins wrong?");
            return ai;
        }

        String points = br.getRegex("Bonuspoints overall</b></td>.*?<td align=.*?>(\\d+?)&nbsp;\\(([\\d\\.]+?)&#x80;\\)</t").getMatch(0);
        String cash = br.getRegex("Bonuspoints overall</b></td>.*?<td align=.*?>(\\d+?)&nbsp;\\(([\\d\\.]+?)&#x80;\\)</t").getMatch(1);
        String files = br.getRegex("Hosted Files</b></td>.*?<td align=.*?>(.*?)  <a href").getMatch(0);

        ai.setStatus("Account is ok.");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "MM/dd/yy", null));

        ai.setPremiumPoints(Integer.parseInt(points));
        ai.setAccountBalance(Integer.parseInt(cash.replaceAll("\\.", "")));
        ai.setFilesNum(Integer.parseInt(files));

        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        login(account);
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.host.meinupload.error." + error, error));

        }

        String url = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);

        dl = new RAFDownload(this, downloadLink, br.createRequest(url));
        dl.setChunkNum(1);
        dl.setResume(true);
        dl.connect();
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.host.meinupload.serverdefect", "Serivce not available"), 10 * 60 * 1000l);

        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.getPage(downloadLink.getDownloadURL());

        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            downloadLink.getLinkStatus().setErrorMessage(error);
            return false;

        }
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        downloadLink.setName(filename);

        Form form = br.getFormbyValue("Free");
        if (form != null) br.submitForm(form);
        try {
            String s = br.getRegex("Dateigr.*e:</b></td>.*<td align=left>(.*?[MB|KB|B])</td>").getMatch(0);
            long size = Regex.getSize(s);
            if (size > 0) downloadLink.setDownloadSize(size);
        } catch (Exception e) {
        }
        return true;
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}