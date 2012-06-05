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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "icyfiles.com" }, urls = { "http://(www\\.)?icyfiles\\.com/[a-z0-9]+" }, flags = { 0 })
public class IcyFilesCom extends PluginForHost {

    public IcyFilesCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://icyfiles.com/impressum.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">The requested File cant be found<")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String filename = br.getRegex("id=\"file\">(.*?)</div>").getMatch(0);
        final String filesize = br.getRegex("<li>(\\d+) <span>Size/mb</span></li>").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize + "Mb"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        String waittime = br.getRegex("Sorry dude, you have downloaded too much\\. Please wait (\\d+) seconds").getMatch(0);
        int waitThis = waittime != null ? Integer.parseInt(waittime) : 30;
        if (waitThis > 30) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitThis * 1001l); }

        final String dllink = "http://icyfiles.com" + br.getRegex("id=\"downloadBtn\" rel=\"(.*?)\"").getMatch(0);
        if (dllink.contains("null")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        waittime = br.getRegex("class=\"counter\">(\\d+)</div>").getMatch(0);
        waitThis = waittime != null ? Integer.parseInt(waittime) : waitThis;
        sleep((waitThis + 2) * 1001l, downloadLink);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            waittime = br.getRegex("Sorry dude, you have downloaded too much\\. Please wait (\\d+) seconds").getMatch(0);
            if (waittime != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l); }
            if (br.containsHTML("(>All download tickets are in use|please try it again in a few seconds</div>|<head><title>404 Not Found</title>)")) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.icyfilescom.errors.nofreeslotsavailable", "No free download slots available"), 10 * 60 * 1000l); }
            if (br.containsHTML("Dont skip the countdown")) {
                logger.warning("Waittime isn't correct...");
            }
            if (downloadLink.getDownloadURL().equals(dl.getConnection().getURL().toString())) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}