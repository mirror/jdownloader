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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ifile.it" }, urls = { "http://[\\w\\.]*?ifile\\.it/[\\w]+/?" }, flags = { 2 })
public class IFileIt extends PluginForHost {

    public IFileIt(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.ifile.it/signup");
    }

    public String getAGBLink() {
        return "http://ifile.it/tos";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("https://secure.ifile.it/signin");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.put("usernameFld", Encoding.urlEncode(account.getUser()));
        form.put("passwordFld", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.setFollowRedirects(false);
        if (!br.containsHTML("you have successfully signed in")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        // This isn't important but with it the login looks more like JD is a
        // real user^^
        String human = br.getRegex("refresh\".*?url=(.*?)\"").getMatch(0);
        if (human != null) br.getPage(human);
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
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String downlink = br.getRegex("var url =.*?\"(.*?)\"").getMatch(0);
        String esn = br.getRegex("var.*?esn =.*?(\\d+);<").getMatch(0);
        if (downlink == null || esn == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String finaldownlink = "http://ifile.it/" + downlink + esn;
        br.getPage(finaldownlink);
        if (!br.containsHTML("status\":\"ok\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage("http://ifile.it/dl");
        if (br.containsHTML("download:captcha")) {
            Browser br2 = br.cloneBrowser();
            for (int i = 0; i <= 5; i++) {
                String code = getCaptchaCode("http://ifile.it/download:captcha?0." + Math.random(), downloadLink);
                String captchaget = "http://ifile.it/" + downlink.replace("&type=na", "") + "&type=simple&esn=" + esn + "&9c16d=" + code;
                br2.getPage(captchaget);
                if (br2.containsHTML("\"retry\":\"retry\"")) continue;
                br.getPage("http://ifile.it/dl");
                break;
            }
            if (br2.containsHTML("\"retry\":\"retry\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("req_btn\".*?href=\"(http://.*?\\.ifile\\.it/.*?)\">").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -3);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 18;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("font-size: [0-9]+%; color: gray;\">(.*?)\\&nbsp;").getMatch(0);
        if (filename == null) filename = br.getRegex("id=\"descriptive_link\" value=\"http://ifile.it/.*?/(.*?)\"").getMatch(0);
        String filesize = br.getRegex(".*?(([0-9]+|[0-9]+\\.[0-9]+) (MB|KB|B|GB)).*?").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim().replaceAll("(\r|\n|\t)", ""));
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (br.containsHTML("signup for a free account in order to download this file")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via account");
        } else {
            throw new PluginException(LinkStatus.ERROR_FATAL, "No free handling implemented yep, please contact the support");
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("directLink", null);
    }
}
