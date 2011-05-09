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
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "czshare.com" }, urls = { "http://[\\w\\.]*?czshare\\.com/(files/\\d+/[\\w_]+|\\d+/[\\w_]+/[^\\s]+|download_file\\.php\\?id=\\d+&file=[^\\s]+)" }, flags = { 2 })
public class CZShareCom extends PluginForHost {

    private int simultanpremium = 20;

    public CZShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://czshare.com/create_user.php");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">Z vaší IP adresy bohužel momentálně probíhá stahování jiného souboru")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.czsharecom.ipalreadydownloading", "IP already downloading"), 15 * 60 * 1000);
        if (!br.containsHTML("Stáhnout FREE</span></a><a href=\"/download\\.php\\?id=")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.CZShareCom.nofreeslots", "No free slots available"), 60 * 1000);
        br.setFollowRedirects(false);
        String freeLink = br.getRegex("allowTransparency=\"true\"></iframe><a href=\"(/.*?)\"").getMatch(0);
        if (freeLink == null) freeLink = br.getRegex("\"(/download\\.php\\?id=\\d+\\&code=.*?)\"").getMatch(0);
        if (freeLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://czshare.com" + freeLink);
        Form down = br.getForm(0);
        String captchaUrl = br.getRegex("action=\"free\\.php\" method=\"post\">[\t\n\r ]+<img src=\"(.*?)\"").getMatch(0);
        if (captchaUrl == null) captchaUrl = br.getRegex("\"(captcha\\.php\\?ticket=.*?\\&id=\\d+)\"").getMatch(0);
        if (down == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        down.remove(null);
        captchaUrl = "http://czshare.com/" + captchaUrl;
        String code = getCaptchaCode(captchaUrl, downloadLink);
        down.put("captchastring", code);
        br.submitForm(down);
        if (br.containsHTML("Chyba 6 / Error 6")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000);
        if (br.containsHTML("Nesouhlasi kontrolni kod")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Soubor je dočasně nedostupný\\.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void login(Account account) throws Exception {
        // this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        // br.clearCookies("czshare.com");
        br.getPage("http://czshare.com/prihlasit.php");
        Form login = br.getForm(0);
        login.put("jmeno2", Encoding.urlEncode(account.getUser()));
        login.put("heslo", Encoding.urlEncode(account.getPass()));
        login.put("trvale", "0");
        br.submitForm(login);
        if (!br.containsHTML("odhl.sit")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://czshare.com/profi/platnost.php");
        if (br.containsHTML("Nemáte žádný platný kredit")) {
            account.setValid(false);
            ai.setStatus(JDL.L("plugins.hoster.CZShareCom.nocreditleft", "No traffic credit left"));
            return ai;
        }
        String trafficleft = br.getRegex("Velikost kreditu.*?Platnost do</td>.*?<td>(.*?)</td>").getMatch(0);
        String expires = br.getRegex("Velikost kreditu.*?Platnost do</td>.*?<td>.*?<td>(.*?)</td>").getMatch(0);
        if (expires != null && !expires.equals("neomezená")) ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "dd.MM.yy HH:mm", Locale.GERMANY));
        if (trafficleft != null) ai.setTrafficLeft(trafficleft);
        account.setValid(true);
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    public void checkPremiumIP() throws Exception {
        /* we have to add ip to list, else we only get slow speed */
        br.getPage("http://czshare.com/profi/filtr.php");
        Form form = br.getForm(1);
        /* check if ip is already added */
        String ip = form.getVarsMap().get("ip");
        if (br.containsHTML("smaz=" + ip)) return;
        /* add current ip to list */
        br.submitForm(form);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        login(account);
        checkPremiumIP();
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRegex("id=\"profi_prava\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.czshare\\.com/\\d+/.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // More chunks are possible but often cause servererrors...
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_PREMIUM);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://www.czshare.com/pravidla.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("/error.php?co=4") || br.containsHTML("Omluvte, prosím, výpadek databáze\\. Na opravě pracujeme")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<div class=\"left\\-col\">[\t\n\r ]+<h1>(.*?)<span>\\&nbsp;</span></h1>").getMatch(0));
        if (filename == null) filename = Encoding.htmlDecode(br.getRegex("<title>(.*?) CZshare\\.com download</title>").getMatch(0));
        String filesize = br.getRegex("Velikost: (.*?)<br").getMatch(0);
        if (filename == null || filesize == null || filesize.equals("0 B")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
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
