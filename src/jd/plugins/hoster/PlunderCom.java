//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "plunder.com" }, urls = { "http://[\\w\\.]*?(youdownload\\.eu|binarybooty\\.com|mashupscene\\.com|plunder\\.com|files\\.youdownload\\.com)/((-download-[a-z0-9]+|.+\\-download\\-.+)\\.htm|(?!/)[0-9a-z]+)" }, flags = { 0 })
public class PlunderCom extends PluginForHost {

    /**
     * @author pspzockerscene
     */
    public PlunderCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.plunder.com/x/tos";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL() + "?showlink=1");
        String dllink = br.getRegex("/h2><a href=(\"|')?(http://[^<>\"']+)").getMatch(1);
        if (dllink == null) dllink = br.getRegex("(\"|')?(http://[a-z0-9]+\\.plunder\\.com/x/[^<>\"']+)").getMatch(1);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            String checklink = br.getURL();
            if (br.containsHTML("Too many downloads from this IP") || checklink.contains("/blocked")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads!", 10 * 60 * 1000l);
            if (br.containsHTML("You must log in to download more this session") || checklink.contains("/login/") || checklink.contains("/register/")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Register or perform a reconnect to download more!", 10 * 60 * 1001l);
            if (checklink.contains("/error")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        // Sometimes the links are outdated but they show the new links, the
        // problem is they they new and newer links could also be moved so this
        // is why we need the following part!
        for (int i = 0; i <= 5; i++) {
            String objectMoved = br.getRegex("<h2>Object moved to <a href=\"(.*?)\">here</a>").getMatch(0);
            if (objectMoved == null) objectMoved = br.getRegex("This document may be found <a HREF=\"(.*?)\"").getMatch(0);
            if (objectMoved != null) {
                objectMoved = Encoding.htmlDecode(objectMoved);
                if (!objectMoved.contains("http")) objectMoved = "http://www.plunder.com" + objectMoved;
                br.getPage(objectMoved);
                objectMoved = br.getRegex("<h2>Object moved to <a href=\"(.*?)\">here</a>").getMatch(0);
                if (objectMoved == null) objectMoved = br.getRegex("This document may be found <a HREF=\"(.*?)\"").getMatch(0);
                if (objectMoved != null) continue;
                downloadLink.setUrlDownload(br.getURL());
                break;
            } else {
                break;
            }
        }
        br.setFollowRedirects(true);
        if (br.getURL().contains("/search/?f=")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1>([^<>\"]*?) Download</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) download[\t\n\r ]+</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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