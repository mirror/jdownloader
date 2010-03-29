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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.JDHash;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "qshare.com" }, urls = { "http://[\\w\\.]*?qshare\\.com/get/[0-9]{1,20}/.*" }, flags = { 2 })
public class QshareCom extends PluginForHost {
    public QshareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://s1.qshare.com/index.php?sysm=sys_page&sysf=site&site=buy");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(false);

        String id = new Regex(downloadLink.getDownloadURL(), "get/(\\d+)/").getMatch(0);
        br.getPage("http://qshare.com/api/file_info.php?id=" + id);
        if (!br.containsHTML("#")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        HashMap<String, String> infoMap = new HashMap<String, String>();
        String[] infos = br.toString().split("#");
        for (String info : infos) {
            infoMap.put(info.split(":")[0], info.split(":")[1]);
        }
        downloadLink.setName(infoMap.get("NAME").trim());
        downloadLink.setDownloadSize(Long.parseLong(infoMap.get("SIZE").trim()));
        downloadLink.setMD5Hash(infoMap.get("MD5").trim());

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setDebug(false);
        String error = br.getRegex("<SPAN STYLE=\"font\\-size:13px;color:#BB0000;font\\-weight:bold\">(.*?)</SPAN>").getMatch(0);
        if (error != null) throw new PluginException(LinkStatus.ERROR_FATAL, Encoding.UTF8Encode(error));

        String url = br.getRegex(Pattern.compile("<SCRIPT TYPE=\"text/javascript\">.*?function free\\(\\).*?window.location = \"(.*?)\";", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(url);

        if (br.getRegex("(Du hast die maximal zulässige Anzahl|You have exceeded the maximum allowed)").matches()) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        if (br.containsHTML("There are currently too many free downloads")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "There are currently too many free downloads", 10 * 60 * 1000l);
        String link = br.getRegex("writeToPage\\('<A HREF=\"(.*?)\"").getMatch(0);
        if (link == null) {
            String wait = br.getRegex("Dein Freivolumen wird in <b>([\\d]*?) Minuten").getMatch(0);
            if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Ticket Time
        int tt = 45;
        String ttt = br.getRegex("count_down\\((\\d+)\\)").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        sleep(tt * 1001l, downloadLink);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
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
        String url = br.getRegex("A HREF=\"(.*?)\">").getMatch(0);
        br.getPage(url);

        if (br.getRedirectLocation() != null) {
            logger.info("QSHARE.COM: Direct Download is activ");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 0);
        } else {
            logger.warning("QSHARE.COM: Indirect Download is activ (is much slower... you should active direct downloading in the configs(qshare configs)");
            // Keine errors gefunden, deshalb folgendes Regex evtl falsch
            String error = br.getRegex("<SPAN STYLE=\"font\\-size:13px;color:#BB0000;font\\-weight:bold\">(.*?)</SPAN>").getMatch(0);
            if (error != null) {
                logger.severe(error);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String[] links = br.getRegex("class=\"button\" href=\"(.*?)\"><span>").getColumn(0);
            if (links.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(links[1]);
            url = br.getRegex("(http://\\w{1,5}.qshare.com/\\w{1,10}/\\w{1,50}/\\w{1,50}/\\w{1,50}/\\w{1,50}/" + account.getUser() + "/" + account.getPass() + "/.*?)\"").getMatch(0);
            if (links.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);

        }

        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            if (dl.getConnection().getContentType().contains("html")) {
                logger.severe("QSHARE.COM: Server error. The file does not exist");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        dl.startDownload();

    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.qshare.com/index.php?sysm=user_portal&sysf=login&new_lang=1");
        // passt invalid html code an. es fehlt der form-close tag
        if (br.getRequest().getHtmlCode().toLowerCase().contains("<form") && !br.getRequest().getHtmlCode().toLowerCase().contains("</form")) {
            br.getRequest().setHtmlCode(br.getRequest().getHtmlCode() + "</form>");
        }
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("username", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        form.remove("cookie");
        br.submitForm(form);

        String premiumError = br.getRegex("[Following error occured|Folgender Fehler ist aufgetreten]: (.*?)[\\.|<]").getMatch(0);
        if (premiumError != null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        HashMap<String, Long> apiMap = new HashMap<String, Long>();
        String[] accInfos = br.getPage("http://qshare.com/api/account_info.php?user=" + account.getUser() + "&pass=" + JDHash.getMD5(account.getPass())).split("#");
        for (String accInfo : accInfos) {
            if (!accInfo.split(":")[0].trim().equalsIgnoreCase("USER")) {
                apiMap.put(accInfo.split(":")[0].trim(), Long.parseLong(accInfo.split(":")[1].trim()));
            }
        }

        if (apiMap.get("ACTIVE") == 0) {
            account.setValid(false);
            ai.setStatus("Invalid account");
            return ai;
        } else
            account.setValid(true);

        if (apiMap.get("FLAT") == 1) {
            if (apiMap.get("FLAT_END") != 0)
                ai.setValidUntil(apiMap.get("FLAT_END") * 1000);
            else
                ai.setStatus("Flatrate account");
        } else {
            ai.setTrafficLeft(apiMap.get("TRAFFIC_REMAIN"));
            ai.setTrafficMax(apiMap.get("TRAFFICMAX"));
            ai.setValidUntil(apiMap.get("VOLUME_EXIRE") * 1000);
            ai.setStatus("Volume account");
        }
        ai.setCreateTime(apiMap.get("CREATETIME") * 1000);
        ai.setFilesNum(apiMap.get("FILESNUM"));
        ai.setUsedSpace(apiMap.get("FILESIZE"));
        ai.setPremiumPoints(apiMap.get("PREMIUMPOINTS"));
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://s1.qshare.com/index.php?sysm=sys_page&sysf=site&site=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
