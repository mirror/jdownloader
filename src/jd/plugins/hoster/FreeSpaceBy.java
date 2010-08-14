//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freespace.by" }, urls = { "http://[\\w\\.]*?freespace\\.by/download/[a-z0-9]+" }, flags = { 2 })
public class FreeSpaceBy extends PluginForHost {

    public FreeSpaceBy(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://freespace.by/info/rules";
    }

    private static final String MAINPAGE = "http://freespace.by/";
    private static final String COUNTRYBLOCKED = "Услуги FreeSpace доступны только для белорусских сетей\\.";
    private static final String COUNTRYBLOCKEDTEXT = "This hoster is now available in your country!";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(COUNTRYBLOCKED)) {
            link.getLinkStatus().setStatusText(COUNTRYBLOCKEDTEXT);
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML(">Файл не найден<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Имя файла:</td> <td>(.*?)</td>").getMatch(0);
        if (filename == null) filename = br.getRegex("<textarea  class=\"links\" onclick=\"this\\.focus\\(\\);this.select\\(\\)\" readonly=\"readonly\"><a href=\"http://freespace\\.by/download/[a-z0-9]+\">(.*?)</a></textarea><br>").getMatch(0);
        String filesize = br.getRegex("<tr><td>Размер:</td> <td>(.*?)</td></tr>").getMatch(0);
        if (filesize != null) filesize = br.getRegex("<td>Размер:</td><td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize.replace(",", ".").replace(" ", "")));
        String sh1 = br.getRegex("SHA1</a>:</td> <td>(.*?)</td>").getMatch(0);
        if (sh1 != null) link.setSha1Hash(sh1.trim());
        String md5 = br.getRegex("MD5</a>:</td> <td>(.*?)</td>").getMatch(0);
        if (md5 != null) link.setMD5Hash(sh1.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, "This hoster doesn't allow any free downloads.");
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML(COUNTRYBLOCKED)) {
            downloadLink.getLinkStatus().setStatusText(COUNTRYBLOCKEDTEXT);
            throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKEDTEXT);
        }
        br.setFollowRedirects(false);
        String dllink = null;
        String shortWaittime = br.getRegex("id=\"remaining_time\">(\\d+)</span>").getMatch(0);
        if (shortWaittime != null) {
            logger.info("Fount waittime, waiting...");
            sleep(Integer.parseInt(shortWaittime) * 1001l, downloadLink);
            String postPage = "http://freespace.by/api.php?format=ajax&lang=ru&PHPSESSID=" + br.getCookie(MAINPAGE, "PHPSESSID") + "&JsHttpRequest=" + System.currentTimeMillis() + "-xml";
            String post = "action[0]=Download.getFreeDownloadLink&download_token[0]=" + new Regex(downloadLink.getDownloadURL(), "freespace\\.by/download/([a-z0-9]+)").getMatch(0);
            br.postPage(postPage, post);
            dllink = br.getRegex("response\":\"(http:.*?)\"").getMatch(0);
        } else {
            logger.info("Fount no waittime, trying to start the download...");
            dllink = br.getRegex("id=\"free_download_link_wrapper\">[\n\t\r ]+<a href=\"http://.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+/get/.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("dllink couldn't be found, plugin seems to be broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage(MAINPAGE);
        String post = "action[0]=AuthExt.logon&username[0]=" + Encoding.urlEncode(account.getUser()) + "&password[0]=" + Encoding.urlEncode(account.getPass()) + "&remember[0]=1";
        String postPage = "http://freespace.by/api.php?format=ajax&lang=ru&PHPSESSID=" + br.getCookie(MAINPAGE, "PHPSESSID") + "&JsHttpRequest=" + System.currentTimeMillis() + "-xml";
        br.postPage(postPage, post);
        if (br.getCookie(MAINPAGE, "lms_auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ai.setUnlimitedTraffic();
        ai.setStatus("Account is OK");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}