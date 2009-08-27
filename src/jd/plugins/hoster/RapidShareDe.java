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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.de" }, urls = { "http://[\\w\\.]*?rapidshare.de/files/[\\d]{3,9}/.*" }, flags = { 2 })
public class RapidShareDe extends PluginForHost {

    public RapidShareDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapidshare.de/en/premium.html");
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("https://ssl.rapidshare.de/cgi-bin/premiumzone.cgi");
        br.postPage("https://ssl.rapidshare.de/cgi-bin/premiumzone.cgi", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&german=on");
        if (br.getCookie("http://rapidshare.de", "user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String validUntil = br.getRegex("Account ist g.*?bis (.*?)\\. Wenn").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil.trim(), "EEE, dd. MMM yyyy", null));
            account.setValid(true);
        }
        String left = br.getRegex("Derzeit sind noch <b>(.*?)</b>").getMatch(0);
        ai.setTrafficLeft(left);
        ai.setTrafficMax(Regex.getSize("20 GB"));
        return ai;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        long waittime;
        try {
            waittime = Long.parseLong(br.getRegex("<script>var.*?\\= ([\\d]+)").getMatch(0)) * 1000;

            this.sleep((int) waittime, downloadLink);
        } catch (Exception e) {
            try {
                waittime = Long.parseLong(br.getRegex("Oder warte (\\d*?) Minute").getMatch(0)) * 60000;
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
            } catch (PluginException e2) {
                throw e2;
            } catch (Exception es) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
        }

        String ticketCode = Encoding.htmlDecode(new Regex(br, "unescape\\(\\'(.*?)\\'\\)").getMatch(0));

        Form form = Form.getForms(ticketCode)[0];
        File captchaFile = getLocalCaptchaFile(".png");
        String captchaAdress = new Regex(ticketCode, "<img src=\"(.*?)\">").getMatch(0);
        br.getDownload(captchaFile, captchaAdress);
        if (!captchaFile.exists() || captchaFile.length() == 0) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        String code = null;

        code = getCaptchaCode(captchaFile, downloadLink);
        form.put("captcha", code);
        br.setFollowRedirects(true);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form).startDownload();

        File l = new File(downloadLink.getFileOutput());
        if (l.length() < 10240) {
            String local = JDIO.getLocalFile(l);
            if (Regex.matches(local, "Zugriffscode falsch")) {
                l.delete();
                l.deleteOnExit();
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }

        }
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        String url = null;
        if (br.getRedirectLocation() == null) {
            Form form = br.getForm(1);
            form.getInputFields().remove(2);
            br.submitForm(form);
            url = br.getRegex("Download.*?>.*?</font>:</b>.*?href=\"(http://.*?rapidshare.*?)\">").getMatch(0);
        } else {
            url = br.getRedirectLocation();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        dl.startDownload();
    }

    public String getAGBLink() {
        return "http://rapidshare.de/de/faq.html";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());
            Form[] forms = br.getForms();
            if (forms.length < 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            br.submitForm(forms[1]);

            String[][] regExp = new Regex(br, "<p>Du hast die Datei <b>(.*?)</b> \\(([\\d]+)").getMatches();
            downloadLink.setDownloadSize(Integer.parseInt(regExp[0][1]) * 1024);
            downloadLink.setName(regExp[0][0]);
            return AvailableStatus.TRUE;
        } catch (Exception e) {
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
