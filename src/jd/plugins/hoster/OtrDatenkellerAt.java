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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "otr.datenkeller.at" }, urls = { "http://[\\w\\.]*?otr\\.datenkeller\\.at/\\?(file|getFile)=.+" }, flags = { 0 })
public class OtrDatenkellerAt extends PluginForHost {

    public OtrDatenkellerAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://otr.datenkeller.at";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("getFile", "file"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (!br.containsHTML("id=\"reqFile\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = new Regex(link.getDownloadURL(), "otr\\.datenkeller\\.at/\\?file=(.+)").getMatch(0);
        String filesize = br.getRegex("Gr.{1,5}e: </td><td align='center'>(.*?)<td").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace("i", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dlPage = downloadLink.getDownloadURL().replace("?file=", "?getFile=");
        br.getPage(dlPage);
        String dllink = null;
        if (br.containsHTML("klicken um den Download zu starten")) {
            dllink = getDllink();
        } else {
            downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
            for (int i = 0; i <= 25; i++) {
                sleep(28 * 1000l, downloadLink);
                br.getPage(dlPage);
                if (br.containsHTML("klicken um den Download zu starten")) {
                    br.getPage(dlPage);
                    dllink = getDllink();
                    break;
                }
                if (i > 24) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Didn't get a ticket");
                logger.info("Didn't get a ticket on try " + i + ". Retrying...");
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -6);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String getDllink() throws Exception, PluginException {
        Regex allMatches = br.getRegex("onclick=\"startCount\\([0-9]{1}, [0-9]{1}, '(.*?)', '(.*?)', '(.*?)'\\)");
        String firstPart = allMatches.getMatch(1);
        String secondPart = allMatches.getMatch(0);
        String thirdPart = allMatches.getMatch(2);
        if (firstPart == null || secondPart == null || thirdPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + firstPart + "/" + secondPart + "/" + thirdPart;
        return dllink;
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