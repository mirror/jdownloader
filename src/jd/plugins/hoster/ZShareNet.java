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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zshare.net" }, urls = { "http://[\\w\\.]*?zshare\\.net/(download|video|image|audio|flash)/.*" }, flags = { 2 })
public class ZShareNet extends PluginForHost {

    public ZShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.zshare.net/overview.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.zshare.net/TOS.html";
    }

    public boolean nopremium = false;

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage("http://www.zshare.net/myzshare/login.php");
        br.postPage("http://zshare.net/myzshare/process.php?loc=http://zshare.net/myzshare/login.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("unverified")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://zshare.net/myzshare/my-uploads.php");
        String mysession = br.getCookie("http://www.zshare.net", "mysession");
        if ((!br.containsHTML("Your premium account will expire in") && !br.containsHTML("Upgrade your account to Premium")) || mysession == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("Your premium account will expire in")) nopremium = true;
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
        String hostedFiles = br.getRegex("<strong>Uploads found:</strong>.*?(\\d+).*?</p>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        if (!nopremium) {
            String daysleft = br.getRegex("Your premium account will expire in.*?(\\d+).*?days").getMatch(0);
            if (daysleft != null) {
                ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(daysleft) * 24 * 60 * 60 * 1000));
            }
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Registered (Free) User");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String dllink = null;
        int maxchunks = 0;
        if (!nopremium) {
            dllink = br.getRegex("var link_enc\\=new Array\\(\\'(.*?)\\'\\)").getMatch(0);
        } else {
            // Handling for registered free accounts!
            maxchunks = 1;
            Form download = br.getForm(0);
            if (download != null) {
                // Formparameter setzen (zufällige Klickpositionen im Bild)
                download.put("imageField.x", (Math.random() * 160) + "");
                download.put("imageField.y", (Math.random() * 60) + "");
                download.put("imageField", null);
                // Form abschicken
                br.submitForm(download);
                String fnc = br.getRegex("var link_enc\\=new Array\\(\\'(.*?)\\'\\)").getMatch(0);
                fnc = fnc.replaceAll("\\'\\,\\'", "");
                dllink = fnc;
            } else {
                dllink = br.getRegex("<td bgcolor=\"#CCCCCC\">.*?<img src=\"(http://.*?.zshare.net/download/.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("<td bgcolor=\"#CCCCCC\">.*?<img src=\"(.*?)\"").getMatch(0);
                    if (!dllink.startsWith("/")) dllink = "/" + dllink;
                }
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (nopremium) sleep(20 * 1001l, downloadLink);
        dllink = dllink.replaceAll("\\'\\,\\'", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxchunks);
        // Möglicherweise serverfehler...
        if (!dl.getConnection().isContentDisposition() || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("/images/download.gif")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
            if (br.containsHTML("404 - Not Found") || br.getHttpConnection().getContentLength() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getHeaders().get("Referer") != null && br.getHeaders().get("Referer").contains("token")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (downloadLink.getDownloadURL().contains(".html")) {
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());
            br.getPage(br.getRedirectLocation().replaceFirst("zshare.net/(download|video|audio|flash|image)", "zshare.net/download"));
        } else {
            br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash|image)", "zshare.net/download"));
        }
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File Name:.*?<font color=\"#666666\".*?>(.*?)</font>").getMatch(0);
        String filesize = br.getRegex("File Size:.*?<font color=\"#666666\".*?>(.*?)</font>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("Image Size:.*?<font color=\"#666666\".*?>(.*?)</font>").getMatch(0);
        if (filename == null || filesize == null || filename.trim().equals("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "")));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Form abrufen
        Form download = br.getForm(0);
        String dlUrl = null;
        if (download != null) {
            // Formparameter setzen (zufällige Klickpositionen im Bild)
            download.put("imageField.x", (Math.random() * 160) + "");
            download.put("imageField.y", (Math.random() * 60) + "");
            download.put("imageField", null);
            // Form abschicken
            br.submitForm(download);
            String fnc = br.getRegex("var link_enc\\=new Array\\(\\'(.*?)\\'\\)").getMatch(0);
            fnc = fnc.replaceAll("\\'\\,\\'", "");
            dlUrl = fnc;
        } else {
            dlUrl = br.getRegex("<td bgcolor=\"#CCCCCC\">.*?<img src=\"(http://.*?.zshare.net/download/.*?)\"").getMatch(0);
            if (dlUrl == null) {
                dlUrl = br.getRegex("<td bgcolor=\"#CCCCCC\">.*?<img src=\"(.*?)\"").getMatch(0);
                if (!dlUrl.startsWith("/")) dlUrl = "/" + dlUrl;
            }
        }
        if (dlUrl == null) {
            logger.warning("The dlUrl couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* they now check waittime */
        sleep(50 * 1000l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 1);
        // Möglicherweise serverfehler...
        if (!dl.getConnection().isContentDisposition() || dl.getConnection().getContentType().contains("html")) {
            logger.warning("The download couldn't be started, something is wrong...");
            br.followConnection();
            if (br.containsHTML("/images/download.gif")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
            if (br.containsHTML("404 - Not Found") || br.getHttpConnection().getContentLength() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getHeaders().get("Referer") != null && br.getHeaders().get("Referer").contains("token")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            logger.warning("Unsupported errormessage on downloadstart!");
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
