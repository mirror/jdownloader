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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.net" }, urls = { "http://[\\w\\.]*?letitbit\\.net/download/[0-9a-zA-z/.-]+" }, flags = { 2 })
public class LetitBitNet extends PluginForHost {

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.setStartIntervall(4000l);
        enablePremium("http://letitbit.net/page/premium.php");
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
        String filesize = br.getRegex("<span>Size of file::</span>(.*?)</h1>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<span>File size::</span>(.*?)</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        Form premiumform = null;
        Form[] allforms = br.getForms();
        if (allforms == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (Form singleform : allforms) {
            if (singleform.containsHTML("pass") && singleform.containsHTML("uid5") && singleform.containsHTML("uid") && singleform.containsHTML("name") && singleform.containsHTML("pin") && singleform.containsHTML("realuid") && singleform.containsHTML("realname") && singleform.containsHTML("host") && singleform.containsHTML("ssserver") && singleform.containsHTML("sssize") && singleform.containsHTML("optiondir")) {
                premiumform = singleform;
                break;
            }
        }
        if (premiumform == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        premiumform.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(premiumform);
        /* because there can be another link to a downlodmanager first */
        String url = br.getRegex("(http://[^/]*?/download.*?/.*?)(\"|').*?(http://[^/]*?/download.*?/.*?)(\"|')").getMatch(2);
        if (url == null) url = br.getRegex("(http://[^/]*?/download.*?/.*?)(\"|')").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        /* we have to wait little because server too buggy */
        sleep(5000, downloadLink);
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
        Form down = null;
        Form[] allforms = br.getForms();
        if (allforms == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (Form singleform : allforms) {
            if (singleform.containsHTML("md5crypt")) {
                down = singleform;
                break;
            }
        }
        String captchaurl = null;
        if (down == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String captchaId = down.getVarsMap().get("uid");
        if (captchaId == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaurl = "http://letitbit.net/cap.php?jpg=" + captchaId + ".jpg";
        URLConnectionAdapter con = br.openGetConnection(captchaurl);
        File file = this.getLocalCaptchaFile();
        Browser.download(file, con);
        con.disconnect();
        down.setMethod(Form.MethodType.POST);
        String code = getCaptchaCode(file, downloadLink);
        down.put("cap", code);
        down.put("uid2", captchaId);
        down.setAction("http://letitbit.net/download3.php");
        br.submitForm(down);
        if (!br.containsHTML("<frame")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        url = br.getRegex("<frame src=\"http://letitbit.net/tmpl/tmpl_frame_top.php\\?link=(.*?)\" name=\"topFrame\" scrolling=\"No\" noresize=\"noresize\" id=\"topFrame\" title=\"topFrame\" />").getMatch(0);
        if (url == null || url.equals("")) {
            String nextpage = "http://letitbit.net/tmpl/tmpl_frame_top.php";
            br.getPage(nextpage);
            // Ticket Time
            int waitThat = 60;
            String time = br.getRegex("id=\"errt\">(\\d+)</span>").getMatch(0);
            if (time != null) {
                logger.info("Waittime found, waittime is " + time + " seconds.");
                waitThat = Integer.parseInt(time);
            }
            sleep((waitThat + 5) * 1001, downloadLink);
            br.getPage(nextpage);
            /* letitbit and vipfile share same hosting server ;) */
            /* because there can be another link to a downlodmanager first */
            url = br.getRegex("(http://[^/]*?/download.*?/.*?)(\"|').*?(http://[^/]*?/download.*?/.*?)(\"|')").getMatch(2);
            if (url == null) {
                url = br.getRegex("(http://[^/]*?/download.*?/.*?)(\"|')").getMatch(0);
                if (url == null) url = br.getRegex("\"(http://[0-9]{2,3}\\.[0-9]{2,3}\\.[0-9]{2,3}\\.[0-9]{2,3}/download\\d+/.*?)\"").getMatch(0);
            }
        }
        if (url == null || url.equals("")) {
            logger.warning("url couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
        if (con.getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>Error</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
