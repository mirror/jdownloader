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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gamefront.online" }, urls = { "https://(filefront\\.com|gamefront\\.com/files|gamefront\\.online/files)/\\d+" })
public class GameFrontCom extends PluginForHost {
    public GameFrontCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(?:gamefront\\.com/files/)", "gamefront.online/files/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.gamefront.online/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean AVAILABLECHECKFAILED = true;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        final String agent = UserAgents.stringUserAgent();
        br.getHeaders().put("User-Agent", agent);
        for (int i = 0; i <= 3; i++) {
            final URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (con.getResponseCode() == 502) {
                continue;
            }
            br.followConnection();
            AVAILABLECHECKFAILED = false;
            break;
        }
        if (AVAILABLECHECKFAILED) {
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        if (br.containsHTML("(>File not found, you will be redirected to|<title>Game Front</title>)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("errno=ERROR_CONTENT_QUICKKEY_INVALID")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getRedirectLocation().matches(".*?gamefront\\.online/\\d+/.+")) {
                downloadLink.setUrlDownload(br.getRedirectLocation());
                br.getPage(downloadLink.getDownloadURL());
            } else if (br.getRedirectLocation().endsWith("/files/") || br.getRedirectLocation().endsWith("/files2/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String filename = br.getRegex("<dt>File Name:</dt>\\s*(?:<!--.*?-->)?\\s*<dd>(.*?)</dd>").getMatch(0);
        String filesize = br.getRegex("<dt>File size:</dt>\\s*<dd>(.*?)</dd>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("File Size:<.*?<.*?>(.*?)<").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            filesize = filesize.trim();
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.") + "b"));
        }
        if (br.containsHTML("he file you are looking for seems to be unavailable at the")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unavailable at the moment", 60 * 60 * 1000l);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (AVAILABLECHECKFAILED) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        }
        String fileID = new Regex(downloadLink.getDownloadURL(), "gamefront\\.online/files/(\\d+)").getMatch(0);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        // if (downloadLink.getBooleanProperty("specialstuff", false)) {
        // br.getHeaders().put("Accept", "*/*");
        // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // br.postPage("http://www.gamefront.com/files/service/request",
        // "token=iicJfwAa6vKSUZA%2BU0OqkjHqyTItn8RdRHUCAqeD%2FgtbGsw6vFV9piu7ordQeZSJxiOlJUxQIi5PIl1PpDHvoRATQa2VYXcT7CyftiNYFuE21taC4FkYYKa6i005wTgcgfCI2C2oKTc%2Fxtijzz4ya3SEXYMHpvlrlvLmY0gI2VgNbWBQXPLaKzb6hVkGDS1W");
        // }
        br.getPage("/files/service/thankyou?id=" + fileID);
        String finallink = br.getRegex("If it does not, <a href=\"(http://.*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("downloadURL\\s*=\\s*'(http.*?)'").getMatch(0);
        }
        if (finallink == null) {
            finallink = br.getRegex("http-equiv=\"refresh\" content=\"\\d+;url=(http://.*?)\"").getMatch(0);
        }
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String waittime = br.getRegex("var downloadCount\\s*=\\s*(\\d+);").getMatch(0);
        int wait = 5;
        if (waittime != null) {
            wait = Integer.parseInt(waittime);
        }
        this.sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            br.followConnection();
            if (br.containsHTML("(<title>404 - Not Found</title>|<h1>404 - Not Found</h1>)")) {
                // downloadLink.setProperty("specialstuff", true);
                // throw new PluginException(LinkStatus.ERROR_RETRY);
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            }
        }
        dl.startDownload();
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