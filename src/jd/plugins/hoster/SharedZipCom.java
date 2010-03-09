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
import java.util.Locale;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision: 10707 $", interfaceVersion = 2, names = { "sharedzip.com" }, urls = { "http://[\\w\\.]*?(sharedzip)\\.com/.*?/.*" }, flags = { 0 })
public class SharedZipCom extends PluginForHost {

    public SharedZipCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.sharedzip.com/free132.html");
    }

    public String getAGBLink() {
        return "http://www.sharedzip.com/tos.html";
    }

    public void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();

        br.setFollowRedirects(true);
        br.postPage("http://www.sharedzip.com/", "op=login&redirect=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&x=15&y=13");

    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        // direct download or not?

        if (br.getRedirectLocation() != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 1);
        } else {
            br.loadConnection(null);
            br.forceDebug(true);
            Form download = br.getFormBySubmitvalue("Datei+herunterladen");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download, true, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        try {
            this.setBrowserExclusive();

            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("Die von Ihnen angeforderte Datei konnte nicht gefunden werden")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            }
            String fileName = br.getRegex("Sie haben angefordert <font color=\"red\">http://www.sharedzip.com/(.*?)/(.*?)</font> \\((.*?)\\)</font>").getMatch(1);
            long fileSize = Regex.getSize(br.getRegex("Sie haben angefordert <font color=\"red\">http://www.sharedzip.com/(.*?)/(.*?)</font> \\((.*?)\\)</font>").getMatch(2));
            if (fileName == null || fileSize <= 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            }
            downloadLink.setName(fileName);
            downloadLink.setDownloadSize(fileSize);

            return AvailableStatus.TRUE;
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.forceDebug(true);
        login(account);
        br.getPage("http://www.sharedzip.com/?op=my_account");

        String validUntil = br.getRegex("Ihr Premiumkonto ist g.ltig bis:</TD><TD><b>(.*?)</b><").getMatch(0);
        ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd MMMM yyyy", Locale.GERMANY));
        if (validUntil == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        return ai;

        // ai.setTrafficMax(Regex.getSize(dat[1]));
        // ai.setTrafficLeft((long) (ai.getTrafficMax() * ((100.0 -
        // Float.parseFloat(dat[0])) / 100.0)));

    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        Form form = br.getFormBySubmitvalue("Kostenloser+Download");
        br.forceDebug(true);
        br.submitForm(form);
        String ipblocktime = br.getRegex("You have to wait (.*?) till next download").getMatch(0);
        if (ipblocktime != null) {
            long waittime = Regex.getMilliSeconds(ipblocktime);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        }
        Form download = br.getFormBySubmitvalue("Datei+herunterladen");
        int wait = 1000 * Integer.parseInt(br.getRegex("<span id=\"countdown_str\">Warten <span id=\".*\">(\\d+?)</span> </span>").getMatch(0));
        this.sleep(wait, downloadLink);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download, false, 1);

        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
