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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigasize.com" }, urls = { "http://[\\w\\.]*?gigasize\\.com/get/[a-z0-9]+" }, flags = { 2 })
public class GigaSizeCom extends PluginForHost {

    private static final String AGB_LINK = "http://www.gigasize.com/page.php?p=terms";

    public String agent = "Mozilla/5.0 (Windows; U; Windows NT 6.0; chrome://global/locale/intl.properties; rv:1.8.1.12) Gecko/2008102920  Firefox/3.0.0";

    public GigaSizeCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.gigasize.com/register.php");
        setStartIntervall(5000l);
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setDebug(true);
        br.getHeaders().put("User-Agent", agent);
        br.getPage("http://www.gigasize.com");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        try {
            String token = br.getPage("http://www.gigasize.com/formtoken");
            br.postPage("http://www.gigasize.com/signin", "func=&token=" + token + "&signRem=1&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        } finally {
            br.getHeaders().put("X-Requested-With", null);
        }
        if (br.getCookie("http://gigasize.com", "MIIS_GIGASIZE_AUTH") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("premium\":1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.gigasize.com/myfiles.php");
        return br.getRegex("<div class=\"logged pu\"><em class=\"png\">").matches();
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
        br.getPage("http://www.gigasize.com/settings");
        String expirein = br.getRegex("Expiration date:.*?(\\d+-\\d+-\\d+ \\d+:\\d+:\\d+)").getMatch(0);
        account.setValid(true);
        if (expirein != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expirein, "yyyy-MM-dd HH:mm:ss", null));
        }
        return ai;
    }

    private String getID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "/get/(.+)").getMatch(0);
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.getPage(parameter.getDownloadURL());
        Form form = br.getForm(2);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(form);
        br.setFollowRedirects(true);
        String token = br.getPage("http://www.gigasize.com/formtoken");
        br.postPage("http://www.gigasize.com/getoken", "fileId=" + getID(parameter) + "&token=" + token + "&rnd=" + System.currentTimeMillis());
        String url = br.getRegex("redirect\":\"(http:.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = url.replaceAll("\\\\/", "/");
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        String adsCaptcha = br.getRegex("iframe src='(http://api.adsca.*?)'").getMatch(0);
        if (adsCaptcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser brc = br.cloneBrowser();
        brc.getPage(adsCaptcha);
        String captchaURL = brc.getRegex("img src=\"(http:.*?)\"").getMatch(0);
        String captchaKEY = brc.getRegex("Code:.*?code\">(.*?)<").getMatch(0);
        String captchaCODE = null;
        Browser brt = br;
        br = brc;
        try {
            brc.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
            captchaCODE = getCaptchaCode(captchaURL, downloadLink);
        } finally {
            br = brt;
        }
        br.postPage("http://www.gigasize.com/getoken", "fileId=" + getID(downloadLink) + "&adUnder=&adscaptcha_response_field=" + captchaCODE + "&adscaptcha_challenge_field=" + captchaKEY);
        if (!br.containsHTML("status\":1")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        sleep(30 * 1000l, downloadLink);
        String token = br.getPage("http://www.gigasize.com/formtoken");
        br.postPage("http://www.gigasize.com/getoken", "fileId=" + getID(downloadLink) + "&token=" + token + "&rnd=" + System.currentTimeMillis());
        String url = br.getRegex("redirect\":\"(http:.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = url.replaceAll("\\\\/", "/");
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", agent);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("error\">Download error<\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[] dat = br.getRegex("<strong title=\"(.*?)\".*?File size:.*?>(.*?)<").getRow(0);
        if (dat.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(dat[0]);
        downloadLink.setDownloadSize(SizeFormatter.getSize(dat[1]));
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
