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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livefile.org" }, urls = { "http://(www\\.)?livefile\\.org/get/[A-Za-z0-9]+" }, flags = { 0 })
public class LiveFileOrg extends PluginForHost {

    public LiveFileOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://livefile.org/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>The requested file is deleted or is not available for download|>Error 404 \\(File not found\\!\\))")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fInfo = br.getRegex("<title>([^<>\"/]*?) \\(([^<>\"/]*?)\\)</title>x");
        if (fInfo.getMatches().length == 0) fInfo = br.getRegex("<h1>([^<>\"/]*?) <span class=\"sin\">([^<>\"/]*?)</span>");
        if (fInfo.getMatches().length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String filename = fInfo.getMatch(0);
        String filesize = fInfo.getMatch(1);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "freelink");
        if (dllink == null) {
            if (br.containsHTML(">Free download not available now<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download not possible now!", 30 * 60 * 1000l);
            final String freeLink = downloadLink.getDownloadURL() + "/" + downloadLink.getName() + "/free";
            br.getPage(freeLink);
            if (br.getURL().contains("livefile.org/errors/onlyonefree.shtml") || br.containsHTML("(\\'Parallel downloading is canceled|>We found that you\\'ve download another file|Parallel downloading is not supported for free mode)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Only 1 simultan download allowed!", 10 * 60 * 1000l);
            final Regex keys = br.getRegex("FreeDlWait\\(\\'([A-Z0-9]+)\\',\\'([a-z0-9]+)\\',\\'(\\d+)\\'");
            if (keys.getMatches().length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            sleep(Integer.parseInt(keys.getMatch(2)) * 1001l, downloadLink);
            final Browser ajaxBR = br.cloneBrowser();
            ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            ajaxBR.getPage("http://livefile.org/captcha.php?f=" + keys.getMatch(0) + "&dl=" + keys.getMatch(1) + "&name=" + downloadLink.getName() + "&ok=true");
            String captchaLink = ajaxBR.getRegex("\"(/capic\\.php\\?bin=[a-z0-9]+)\"").getMatch(0);
            if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final String code = getCaptchaCode("http://livefile.org" + captchaLink, downloadLink);
            br.setFollowRedirects(false);
            br.postPage(freeLink, "checkcaptcha=1&cpid=" + code);
            if (br.containsHTML(">Captcha Error\\!")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("File not found\\!<br>|e>Server Error<")) {
                logger.info("Detected file not found after captcha and waittime!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        final String postdata = "file=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0) + "&keycode=" + account.getPass();
        // Chunkload = Temporary account block -> Bad idea ;)
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://livefile.org/valid.php", postdata, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections!", 10 * 60 * 1000l);
            br.followConnection();
            if (br.containsHTML("Key code not found\\!")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            logger.warning("The final dllink seems not to be a file...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        account.setValid(true);
        ai.setStatus("Status can only be checked while downloading!");
        return ai;
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