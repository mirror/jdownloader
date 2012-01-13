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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "min.us", "minus.com" }, urls = { "http://(www\\.)?min\\.us/[A-Za-z0-9]+", "http://(www\\.)?minus\\.com/[A-Za-z0-9]+" }, flags = { 0 })
public class MinUs extends PluginForHost {

    public MinUs(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("min\\.us", "minus.com"));
    }

    @Override
    public String getAGBLink() {
        return "http://minus.com/pages/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        // Get internal filename
        final String partA = br.getRegex("\"secure_prefix\":\\s?\"(.*?)\"").getMatch(0);
        final String partB = br.getRegex("\"id\":\\s?\"(.*?)\"").getMatch(0);
        if (partA == null || partB == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://i.minus.com" + partA + "/d" + partB, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (downloadLink.getFinalFileName() == null) {
            downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setDebug(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h2>Not found\\.</h2>|<p>Our records indicate that the gallery/image you are referencing has been deleted or does not exist|The page you requested does not exist)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("\\{\"name\":\\s?\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"description\" content=\".*?, (.*?)\"").getMatch(0);
        }
        final String filesize = br.getRegex("\"filesize\": \"(.*?)\"").getMatch(0);
        if (filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        // Set the final filename here because servers send other/internal or
        // wrong names
        if (filename != null) {
            link.setFinalFileName(filename.trim());
        } else {
            link.setName(new Regex(link.getDownloadURL(), "minus\\.com/(.+)").getMatch(0));
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}