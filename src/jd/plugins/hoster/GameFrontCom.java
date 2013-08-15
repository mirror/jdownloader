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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gamefront.com", "filefront.com" }, urls = { "http://(www\\.)?(filefront\\.com|gamefront\\.com/files)/\\d+", "blablablaUNUSED_REGEXfh65887iu4ren" }, flags = { 0, 0 })
public class GameFrontCom extends PluginForHost {

    public GameFrontCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        String fKey = new Regex(link.getDownloadURL(), "filefront\\.com/(\\d+)").getMatch(0);
        if (fKey != null) link.setUrlDownload("http://www.gamefront.com/files/" + fKey);
    }

    @Override
    public String getAGBLink() {
        return "http://www.gamefront.com/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean AVAILABLECHECKFAILED = true;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setReadTimeout(3 * 60 * 1000);

        String agent = null;
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);

        for (int i = 0; i <= 3; i++) {
            final URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.getResponseCode() == 502) continue;
            br.followConnection();
            AVAILABLECHECKFAILED = false;
            break;
        }
        if (AVAILABLECHECKFAILED) return AvailableStatus.UNCHECKABLE;
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("(>File not found, you will be redirected to|<title>Game Front</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("errno=ERROR_CONTENT_QUICKKEY_INVALID")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getRedirectLocation().matches(".*?gamefront\\.com/\\d+/.+")) {
                downloadLink.setUrlDownload(br.getRedirectLocation());
                br.getPage(downloadLink.getDownloadURL());
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String filename = br.getRegex("<dt>File Name:</dt>[\t\n\r ]+<dd>(<span title=\")?(.*?)(\"|</dd>)").getMatch(1);
        if (filename == null) {
            filename = br.getRegex("Lucida Grande\\',\\'Lucida Sans Unicode\\',Arial,sans-serif; font\\-size: 28px; font\\-weight: normal;\">(.*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\| Game Front</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("\">File size:</td>[\t\n\r ]+<td>(.*?)</td>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("File Size:<.*?<.*?>(.*?)<").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = filesize.trim();
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.") + "b"));
        if (br.containsHTML("he file you are looking for seems to be unavailable at the")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unavailable at the moment", 60 * 60 * 1000l);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (AVAILABLECHECKFAILED) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        String fileID = new Regex(downloadLink.getDownloadURL(), "gamefront\\.com/files/(\\d+)").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);

        // if (downloadLink.getBooleanProperty("specialstuff", false)) {
        // br.getHeaders().put("Accept", "*/*");
        // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // br.postPage("http://www.gamefront.com/files/service/request",
        // "token=iicJfwAa6vKSUZA%2BU0OqkjHqyTItn8RdRHUCAqeD%2FgtbGsw6vFV9piu7ordQeZSJxiOlJUxQIi5PIl1PpDHvoRATQa2VYXcT7CyftiNYFuE21taC4FkYYKa6i005wTgcgfCI2C2oKTc%2Fxtijzz4ya3SEXYMHpvlrlvLmY0gI2VgNbWBQXPLaKzb6hVkGDS1W");
        // }

        br.getPage("http://www.gamefront.com/files/service/thankyou?id=" + fileID);
        String finallink = br.getRegex("If it does not, <a href=\"(http://.*?)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex("http-equiv=\"refresh\" content=\"\\d+;url=(http://.*?)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex("(\"|')(http://media\\d+\\.gamefront\\.com/personal/\\d+/\\d+/[a-z0-9]+/.*?)(\"|')").getMatch(1);
        if (finallink == null) finallink = br.getRegex("downloadURL.*?(http://media\\d+\\.gamefront\\.com/.*?)'").getMatch(0);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String waittime = br.getRegex("var downloadCount = (\\d+);").getMatch(0);
        int wait = 5;
        if (waittime != null) wait = Integer.parseInt(waittime);
        this.sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(<title>404 - Not Found</title>|<h1>404 - Not Found</h1>)")) {
                // downloadLink.setProperty("specialstuff", true);
                // throw new PluginException(LinkStatus.ERROR_RETRY);
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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