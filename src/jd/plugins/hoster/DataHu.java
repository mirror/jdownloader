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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data.hu" }, urls = { "http://[\\w\\.]*?data.hu/get/\\d+/.+" }, flags = { 2 })
public class DataHu extends PluginForHost {

    private static Object LOCK = new Object();

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://data.hu/premium.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace(".html", ""));
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
        br.getPage("http://data.hu/index.php?isl=1");
        if (!br.containsHTML("<td>Prémium:</td>")) {
            account.setValid(false);
            return ai;
        }
        String days = br.getRegex("<td><a href=\"/premium\\.php\">(.*?)<span").getMatch(0);
        if (days != null && !days.equals("0")) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(days, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        } else {
            logger.warning("Couldn't get the expire date, stopping premium!");
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        }
        String points = br.getRegex(Pattern.compile("title=\"Mi az a DataPont\\?\">(\\d+) pont</a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Long.parseLong(points));
        ai.setStatus("Premium account");
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://data.hu/adatvedelem.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 2;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        if (br.containsHTML("A let.*?shez v.*?rnod kell:")) {
            long wait = (Long.parseLong(br.getRegex(Pattern.compile("<div id=\"counter\" class=\"countdown\">([0-9]+)</div>")).getMatch(0)) * 1000);
            sleep(wait, downloadLink);
        }
        br.getPage(downloadLink.getDownloadURL());
        String link = br.getRegex(Pattern.compile("<div class=\"download_box_button\"><a href=\"(http://.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) link = br.getRegex(Pattern.compile("\"(http://ddl\\d+\\.data\\.hu/get/\\d+/\\d+/.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            // Wait a minute for respons 503 because JD tried to start too many
            // downloads in a short time
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.datahu.toomanysimultandownloads", "Too many simultan downloads, please wait some time!"), 60 * 1000l);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String link = br.getRegex("window\\.location\\.href=\\'(.*?)\\';").getMatch(0);
        if (link == null) link = br.getRegex("\"(http://ddlp\\.data\\.hu/get/[a-z0-9]+/\\d+/.*?)\"").getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(Account account) throws Exception {
        synchronized (LOCK) {
            br.setCustomCharset("utf-8");
            br.setFollowRedirects(true);
            this.setBrowserExclusive();
            br.forceDebug(true);
            br.getPage("http://data.hu/index.php?isl=1");
            final String loginID = br.getRegex("name=\"login_passfield\" value=\"(.*?)\"").getMatch(0);
            if (loginID == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            String postData = "act=dologin&login_passfield=" + loginID + "&target=%2Findex.php&t=&id=&data=&url_for_login=%2Findex.php%3Fisl%3D1&need_redirect=1&username=" + Encoding.urlEncode(account.getUser()) + "&" + loginID + "=" + Encoding.urlEncode(account.getPass()) + "&remember=on";
            br.postPage("http://data.hu/login.php", postData);
            if (br.getCookie("http://data.hu/", "datapremiumseccode") == null) {
                logger.warning("Cookie error!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"download_filename\">(.*?)</div>").getMatch(0);
        String filesize = br.getRegex(", fájlméret: ([0-9\\.]+ [A-Za-z]{1,5})").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}