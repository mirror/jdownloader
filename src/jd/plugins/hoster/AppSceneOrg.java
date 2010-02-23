//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org & pspzockerscene
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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
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

//appscene.org by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "appscene.org" }, urls = { "http://[\\w\\.]*?appscene\\.org/(download/[0-9a-zA-Z]+|download\\.php\\?id=\\d+)" }, flags = { 2 })
public class AppSceneOrg extends PluginForHost {

    public AppSceneOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.appscene.org/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.appscene.org/about.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.containsHTML("or has been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        for (int i = 0; i <= 4; i++) {
            Form captchaForm = br.getForm(0);
            String captchaurl = br.getRegex("\"(http://www.appscene.org/captcha/.*?)\"").getMatch(0);
            if (captchaForm == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code;
            if (i > 2) {
                code = getCaptchaCode(null, captchaurl, downloadLink);
            } else {
                code = getCaptchaCode(captchaurl, downloadLink);
            }
            captchaForm.put("captcha", code);
            br.submitForm(captchaForm);
            if (br.getRedirectLocation().contains("appscene.org/download")) {
                br.getPage(br.getRedirectLocation());
                continue;
            }
            break;
        }
        String captchaurl = br.getRegex("\"(http://www.appscene.org/captcha/.*?)\"").getMatch(0);
        if (captchaurl != null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.getRedirectLocation().contains("appscene.org/download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.appscene.org/premium");
        br.postPage("http://www.appscene.org/premium_login", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        if (!br.containsHTML("Premium Area") || br.getCookie("http://www.appscene.org/", "premium_id") == null || br.getCookie("http://www.appscene.org/", "premium_pass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        String trafficleft = br.getRegex("\\(( [0-9]+ B )\\)").getMatch(0);
        if (trafficleft == null) trafficleft = br.getRegex("Bandwidth remaining:(.*? GB)").getMatch(0);
        if (trafficleft != null) ai.setTrafficLeft(Regex.getSize(trafficleft));
        ai.setStatus("Premium User");
        return ai;
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String finallink = br.getRedirectLocation();
        jd.plugins.BrowserAdapter.openDownload(br, link, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
