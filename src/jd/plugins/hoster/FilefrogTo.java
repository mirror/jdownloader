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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefrog.to" }, urls = { "http://[\\w\\.]*?filefrog\\.to/download/\\d+/[a-zA-Z0-9]+" }, flags = { 2 })
public class FilefrogTo extends PluginForHost {

    public FilefrogTo(PluginWrapper wrapper) {
        super(wrapper);
        /*
         * server error. without this, paralell downloads result in 0 byte
         * text/html files.
         */
        this.setStartIntervall(1000);
        enablePremium("http://www.filefrog.to/premium");
    }

    public String getAGBLink() {
        return "http://www.filefrog.to/terms";
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.fetchSession();
        br.setDebug(true);
        br.getPage("http://www.filefrog.to/api/info/uid/" + account.getUser() + "/pwd/" + Encoding.urlEncode(account.getPass()));

        HashMap<String, String> dat = new HashMap<String, String>();
        String[][] data = br.getRegex("\"(.*?)\":\"(.*?)\"").getMatches();
        for (String[] entry : data) {
            dat.put(entry[0], entry[1]);
        }
        if (dat.get("traffic") == null) {
            account.setValid(false);
            return ai;
        }
        ai.setTrafficLeft(Regex.getSize(dat.get("traffic")));
        ai.setValidUntil(Regex.getMilliSeconds(dat.get("premium_until"), "yyyy-MM-dd", null));
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);

        br.forceDebug(true);

        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Sorry your Premium traffic is exhausted")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);

        if (br.getRedirectLocation() == null) {
            // indirect download
            Form form = br.getForms()[0];
            // workaround a base. Not required in browser versions above Rev
            // 11067
            form.setAction(downloadLink.getDownloadURL());
            br.submitForm(form);
        }
        dl = BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Sorry your Premium traffic is exhausted")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            // server error. for some files it returns 0 byte text files.
            if (con.getContentLength() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60000);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        br.getPage("http://www.filefrog.to/user/login");
        Form form = br.getForms()[0];
        form.getInputField("pwd").setValue(account.getPass());
        form.getInputField("uid").setValue(account.getUser());
        br.submitForm(form);
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.fetchSession();
        String[] ids = new Regex(downloadLink.getDownloadURL(), "filefrog\\.to/download/(\\d{1,})/(.*)").getRow(0);
        br.getPage("http://www.filefrog.to/api/status/" + ids[0] + "/" + ids[1]);
        String[] data = br.getRegex("(.*?);(.*?);(\\d+)").getRow(0);
        if (data[0].equalsIgnoreCase("offline")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (data[0].equalsIgnoreCase("only premium")) {
            downloadLink.getLinkStatus().setErrorMessage("Download only with premium account");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }

        downloadLink.setName(data[1]);
        downloadLink.setDownloadSize(Long.parseLong(data[2]));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.forceDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("traffic-exhausted")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60000);
        Form form = br.getForms()[0];
        // workaround a base. Not required in browser versions above Rev 11067
        form.setAction(downloadLink.getDownloadURL());
        // not required
        // this.sleep(30000, downloadLink);
        br.submitForm(form);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("traffic-exhausted")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60000);
        dl = BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation());

        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            if (br.getURL().contains("traffic-exhausted")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60000);
            // server error. for some files it returns 0 byte text files.
            if (con.getContentLength() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60000);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void fetchSession() throws IOException {
        br.getPage("http://www.filefrog.to/index/locale/set/en_US");
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
