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

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class HotFileCom extends PluginForHost {

    public HotFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hotfile.com/premium.html");
    }

    // @Override
    public String getAGBLink() {
        return "http://hotfile.com/terms-of-service.html";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://hotfile.com", "lang", "en");
        br.setFollowRedirects(true);
        br.getPage("http://hotfile.com/");
        br.postPage("http://hotfile.com/login.php", "returnto=%2F&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        Form form = br.getForm(0);
        if (form != null && form.containsHTML("<td>Username:")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://hotfile.com/", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("<b>Premium Membership</b>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        br.setFollowRedirects(false);
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
        String validUntil = br.getRegex("<td>Until.*?</td><td>(.*?2009-06-08 22:28:16)</td>").getMatch(0);
        if (validUntil == null) {
            ai.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyy-dd-MM HH:mm:ss", null));
            ai.setValid(true);
        }
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String finalUrl = null;
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            finalUrl = br.getRedirectLocation();
        } else {
            finalUrl = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?hotfile.*?)\">Click here to download</a></h3>").getMatch(0);
        }
        if (finalUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = br.openDownload(downloadLink, finalUrl, true, 0);
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex("Downloading(.*?)\\([^\\s]*\\)</h2>").getMatch(0);
        String filesize = br.getRegex("Downloading.*?\\(([^\\s]*)\\)</h2>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("JavaScript>starthtimer")) {
            String waittime = br.getRegex("starthtimer\\(\\).*?timerend=.*?\\+(\\d+);").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waittime.trim()));
        }
        String waittime = br.getRegex("starttimer\\(\\).*?timerend=.*?\\+(\\d+);").getMatch(0);
        if (waittime == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        this.sleep(Long.parseLong(waittime.trim()), link);
        Form form = br.getForm(1);
        br.submitForm(form);
        String dl_url = br.getRegex("Downloading.*?<a href=\"(.*?/get/.*?)\">").getMatch(0);
        if (dl_url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = br.openDownload(link, dl_url, true, 1);
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    // @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}
