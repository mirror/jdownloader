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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vip-file.com" }, urls = { "http://[\\w\\.]*?vip-file\\.com/download/[\\w\\.]+/(.*?)\\.html" }, flags = { 2 })
public class Vipfilecom extends PluginForHost {

    public Vipfilecom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        enablePremium("http://vip-file.com/tmpl/premium_en.php");
    }

    @Override
    public String getAGBLink() {
        return "http://vip-file.com/tmpl/terms.php";
    }

    public static String freelinkregex = "\"(http://vip-file.com/download([0-9]+)/.*?)\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        String downloadURL = downloadLink.getDownloadURL();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadURL);
        if (br.containsHTML("This file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileSize = br.getRegex("name=\"sssize\" value=\"(.*?)\"").getMatch(0);
        if (fileSize == null) fileSize = br.getRegex("<p>Size of file: <span>(.*?)</span>").getMatch(0);
        String fileName = br.getRegex("<input type=\"hidden\" name=\"realname\" value=\"(.*?)\" />").getMatch(0);
        if (fileSize == null || fileName == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Regex.getSize(fileSize));
        downloadLink.setName(fileName);
        String link = Encoding.htmlDecode(br.getRegex(Pattern.compile(freelinkregex, Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink", "No free download link for this file"));
            return AvailableStatus.TRUE;
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* DownloadLink holen, 2x der Location folgen */
        /* we have to wait little because server too buggy */
        sleep(2000, downloadLink);
        String link = Encoding.htmlDecode(br.getRegex(Pattern.compile(freelinkregex, Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink", "No free download link for this file"));
        br.setDebug(true);
        /* SpeedHack */
        br.setFollowRedirects(false);
        br.getPage(link);
        link = br.getRedirectLocation();
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!link.contains("vip-file.com")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink", "No free download link for this file"));
        // link = link.replaceAll("file.com.*?/", "file.com:8080/");
        br.setFollowRedirects(true);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1).startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        // Try to find the remaining traffic
        String trafficLeft = br.getRegex("You can download another: (.*?) with current password").getMatch(0);
        if (trafficLeft != null) {
            AccountInfo ai = account.getAccountInfo();
            ai.setTrafficLeft(trafficLeft);
            ai.setStatus("Premium User");
        }
        String url = Encoding.htmlDecode(br.getRegex(Pattern.compile("Ваша ссылка для скачивания:<br><a href='(http://.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(1));
        if (url == null) url = br.getRegex("(http://[0-9]+\\.[0-9]+\\.[0-9]+\\..*?/downloadp[0-9]+/.*?)'>").getMatch(0);
        if (url == null && br.containsHTML("Wrong password")) {
            logger.info("Downloadpassword seems to be wrong, disabeling account now!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* we have to wait little because server too buggy */
        sleep(5000, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 2 * 1000l);
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
