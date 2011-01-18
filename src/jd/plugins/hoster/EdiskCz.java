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
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "edisk.cz" }, urls = { "http://[\\w\\.]*?edisk\\.(cz|sk|eu)/stahni/[0-9]+/.+\\.html" }, flags = { 2 })
public class EdiskCz extends PluginForHost {

    public EdiskCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://www.edisk.cz/kontakt";
    }

    private static final String MAINPAGE = "http://www.edisk.cz/";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("edisk\\.(sk|eu)", "edisk.cz"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Tento soubor již neexistuje z následujích důvodů:|<li>soubor byl smazán majitelem</li>|<li>vypršela doba, po kterou může být soubor nahrán</li>|<li>odkaz je uvedený v nesprávném tvaru</li>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<span class=\"fl\" title=\"(.*?)\">").getMatch(0));
        if (filename == null) filename = br.getRegex("<title> \\&nbsp;\\&quot;(.*?)\\&quot; \\(").getMatch(0);
        String filesize = br.getRegex("<p>Velikost souboru: <strong>(.*?)</strong></p>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<title> \\&nbsp;\\&quot;.*?\\&quot; \\((.*?)\\) - stáhnout soubor\\&nbsp; </title>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("Stáhnout soubor:\\&nbsp;<span class=\"bold\">.*? \\((.*?)\\)</span>").getMatch(0);
            }
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set the final filename here because server gives us filename +
        // ".html" which is bad
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("/stahni/", "/stahni-pomalu/"));
        br.setFollowRedirects(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String postUrl = downloadLink.getDownloadURL().replace("/stahni/", "/x-download/");
        String postData = "action=" + new Regex(downloadLink.getDownloadURL(), "/stahni/(\\d+.*?\\.html)").getMatch(0);
        br.postPage(postUrl, postData);
        String dllink = br.toString().trim();
        if (!dllink.startsWith("http://") || !dllink.endsWith(".html") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.toString().trim(), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("/error/503") || br.containsHTML("<h3>Z této IP adresy již probíhá stahování</h3>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            String unknownErrormessage = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
            if (unknownErrormessage != null) {
                if (unknownErrormessage.equals("Maximální rychlost stahování")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                throw new PluginException(LinkStatus.ERROR_FATAL, unknownErrormessage);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.postPage("http://www.edisk.cz/prihlaseni", "email=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&rememberMe=on&set_auth=true");
        if (br.getCookie(MAINPAGE, "randStr") == null || br.getCookie(MAINPAGE, "email") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://www.edisk.cz/moje-soubory");
        String availabletraffic = br.getRegex("id=\"usercredit\">(\\d+)</span> MB</a>").getMatch(0);
        if (availabletraffic == null) availabletraffic = br.getRegex("<strong>Kredit: </strong>(\\d+)</span>").getMatch(0);
        if (availabletraffic != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic + "MB"));
        } else {
            account.setValid(false);
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        String fileID = new Regex(link.getDownloadURL(), "edisk\\.cz/stahni/(\\d+)/").getMatch(0);
        String premiumPage = br.getRegex("\"(x-premium/\\d+)\"").getMatch(0);
        if (fileID == null || premiumPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://www.edisk.cz/cz/" + premiumPage, "");
        String dllink = br.getRegex("class=\"wide\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("Pokud se tak nestane, <a href=\"(/stahni-.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(/stahni-rychle/\\d+/.*?\\.html)\"").getMatch(0);
            }
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.contains("edisk.cz")) dllink = "http://www.edisk.cz" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
