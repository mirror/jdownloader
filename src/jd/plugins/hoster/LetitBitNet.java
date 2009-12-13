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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.net" }, urls = { "http://[\\w\\.]*?letitbit\\.net/download/[0-9a-zA-z/.-]+" }, flags = { 2 })
public class LetitBitNet extends PluginForHost {

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.setStartIntervall(4000l);
        enablePremium("http://letitbit.net/");
    }

    @Override
    public String getAGBLink() {
        return "http://letitbit.net/page/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        br.postPage(downloadLink.getDownloadURL(), "en.x=10&en.y=8&vote_cr=en");
        String filename = br.getRegex("<span>File::</span>(.*?)</h1>").getMatch(0);
        String size = br.getRegex("<span>File size::</span>(.*?)</h1>").getMatch(0);
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(size));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        Form form = br.getForm(3);
        form.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        /* we have to wait little because server too buggy */
        sleep(5000, downloadLink);
        String url = br.getRegex("(http://[^/]*?/download.*?/.*?)(\"|')").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 2 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String url = null;
        Form dl1 = br.getFormbyProperty("id", "dvifree");
        String captchaurl = null;
        if (dl1 == null) {
            // first trying to bypass block using webproxy:
            br.setFollowRedirects(true);
            String randomain = String.valueOf((int) (Math.random() * 9 + 1));
            br.getPage("http://www.gur" + randomain + ".info/index.php");
            br.postPage("http://www.gur" + randomain + ".info/index.php", "q=" + downloadLink.getDownloadURL() + "&hl[include_form]=0&hl[remove_scripts]=0&hl[accept_cookies]=1&hl[show_images]=1&hl[show_referer]=0&hl[strip_meta]=0&hl[strip_title]=0&hl[session_cookies]=0");
            captchaurl = br.getRegex(Pattern.compile("<div\\sclass=\"cont\\sc2[^>]*>\\s+<br /><br />\\s+<img src=\"(.*?)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            // formaction = forms[3].action;
            if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.letitbitnet.errors.countryblock", "Letitbit forbidden downloading this file in your country"));
        } else {
            String id = dl1.getVarsMap().get("uid");
            captchaurl = "http://letitbit.net/cap.php?jpg=" + id + ".jpg";
        }
        Form down = br.getFormbyProperty("id", "dvifree");
        URLConnectionAdapter con = br.openGetConnection(captchaurl);
        File file = this.getLocalCaptchaFile();
        Browser.download(file, con);
        con.disconnect();
        down.setMethod(Form.MethodType.POST);
        down.put("frameset", "Download+file");
        String id2 = null;
        if (dl1 != null) id2 = dl1.getVarsMap().get("uid");
        // first trying to bypass captcha
        down.put("cap", "2f2411");
        down.put("uid2", "c0862b659695");
        down.put("fix", "1");
        br.getPage(downloadLink.getDownloadURL());
        down.setAction("http://letitbit.net/download3.php");
        br.submitForm(down);
        // if we cannot bypass, ask user for entering captcha code
        if (!br.containsHTML("<frame")) {
            String code = getCaptchaCode(file, downloadLink);
            down.put("cap", code);
            down.put("uid2", id2);
            down.setAction("http://letitbit.net/download3.php");
            br.submitForm(down);
        }
        if (!br.containsHTML("<frame")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        url = br.getRegex("<frame src=\"http://letitbit.net/tmpl/tmpl_frame_top.php\\?link=(.*?)\" name=\"topFrame\" scrolling=\"No\" noresize=\"noresize\" id=\"topFrame\" title=\"topFrame\" />").getMatch(0);
        if (url == null) {
            /* if we have to wait, lets wait 60+5 buffer secs */
            sleep(65 * 1000l, downloadLink);
            String nextpage = br.getRegex("(http://s\\d+.letitbit.net/tmpl/tmpl_frame_top.*?)\"").getMatch(0);
            br.getPage(nextpage);
            /* letitbit and vipfile share same hosting server ;) */
            url = br.getRegex("(http://[^/]*?/download.*?/.*?)(\"|')").getMatch(0);
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* we have to wait little because server too buggy */
        sleep(2000, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        con = dl.getConnection();
        if (con.getResponseCode() == 404) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, null, 5 * 60 * 1001);
        }
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
