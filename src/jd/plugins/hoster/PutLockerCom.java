//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.http.Browser;
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

/** Works exactly like sockshare.com */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "putlocker.com" }, urls = { "http://(www\\.)?putlocker\\.com/(file|embed)/[A-Z0-9]+" }, flags = { 0 })
public class PutLockerCom extends PluginForHost {

    private static final String MAINPAGE = "http://www.putlocker.com";

    public PutLockerCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.putlocker.com/gopro.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/embed/", "/file/"));
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, br);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        String validUntil = br.getRegex("Expiring </td>.*?>(.*?)<").getMatch(0);
        if (validUntil != null) {
            validUntil = validUntil.replaceFirst(" at ", " ");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "MMMM dd, yyyy HH:mm", null));
            ai.setStatus("Premium okay");
            ai.setUnlimitedTraffic();
            account.setValid(true);
        } else {
            account.setValid(false);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.putlocker.com/page.php?terms";
    }

    private String getDllink(DownloadLink downloadLink) throws IOException {
        String dllink = br.getRegex("<a href=\"/gopro\\.php\">Tired of ads and waiting\\? Go Pro\\!</a>[\t\n\rn ]+</div>[\t\n\rn ]+<a href=\"(/.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/get_file\\.php\\?download=[A-Z0-9]+\\&key=[a-z0-9]+)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(/get_file\\.php\\?download=[A-Z0-9]+\\&key=[a-z0-9]+&original=1)\"").getMatch(0);
            if (dllink == null) {
                // Handling for streamlinks
                dllink = br.getRegex("playlist: \\'(/get_file\\.php\\?stream=[A-Za-z0-9=]+)\\'").getMatch(0);
                if (dllink != null) {
                    dllink = MAINPAGE + dllink;
                    downloadLink.setProperty("videolink", dllink);
                    br.getPage(dllink);
                    dllink = br.getRegex("media:content url=\"(http://.*?)\"").getMatch(0);
                    if (dllink == null) dllink = br.getRegex("\"(http://media\\-b\\d+\\.putlocker\\.com/download/\\d+/.*?)\"").getMatch(0);
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String hash = br.getRegex("<input type=\"hidden\" value=\"([a-z0-9]+)\" name=\"hash\">").getMatch(0);
        if (hash == null) {
            logger.warning("hash is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Can still be skipped */
        // String waittime =
        // br.getRegex("var countdownNum = (\\d+);").getMatch(0);
        // int wait = 5;
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        br.postPage(br.getURL(), "hash=" + Encoding.urlEncode(hash) + "&confirm=Continue+as+Free+User");
        if (br.containsHTML("(>This content server has been temporarily disabled for upgrades|Try again soon\\. You can still download it below\\.<)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server temporarily disabled!", 2 * 60 * 60 * 1000l);
        String dllink = getDllink(downloadLink);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("http")) dllink = MAINPAGE + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // My experience was that such files just don't work, i wasn't able
            // to download a link with this error in 3 days!
            if (br.getURL().equals("http://www.putlocker.com/")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.putlockercom.servererrorfilebroken", "Server error - file offline?"));
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, br);
        br.getPage(link.getDownloadURL());
        String dlURL = getDllink(link);
        if (dlURL == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account acc, Browser br) throws PluginException, IOException {
        br.setFollowRedirects(true);
        br.getPage("http://www.putlocker.com/authenticate.php?login");
        br.postPage("http://www.putlocker.com/authenticate.php?login", "user=" + Encoding.urlEncode(acc.getUser()) + "&pass=" + Encoding.urlEncode(acc.getPass()) + "&login_submit=Login");
        if (br.getCookie("http://www.putlocker.com", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://www.putlocker.com/profile.php?pro");
        String proActive = br.getRegex("Pro  Status<.*?>.*?(Active)").getMatch(0);
        if (proActive == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This file doesn\\'t exist, or has been removed \\.<") || br.getURL().contains("?404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("site-content.*?<h1>(.*?)<strong").getMatch(0);
        if (filename == null) filename = br.getRegex("site-content.*?<h1>(.*?)<strong").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) |").getMatch(0);
        String filesize = br.getRegex("site-content.*?<h1>.*?<strong>\\((.*?)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // User sometimes adds random stuff to filenames when downloading so we
        // better set the final name here
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}