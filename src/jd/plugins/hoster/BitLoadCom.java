//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitload.com", "mystream.to" }, urls = { "http://(www\\.)?(bitload\\.com/f/\\d+/[a-z0-9]+|mystream\\.to/file-\\d+-[a-z0-9]+)", "http://blablarfdghrtthgrt56z3ef27893bv" }, flags = { 0, 0 })
public class BitLoadCom extends PluginForHost {

    public BitLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.bitload.com/imprint";
    }

    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().contains("mystream.to/")) {
            Regex mystreamIDs = new Regex(link.getDownloadURL(), "mystream\\.to/file-(\\d+)-([a-z0-9]+)");
            link.setUrlDownload("http://www.bitload.com/f/" + mystreamIDs.getMatch(0) + "/" + mystreamIDs.getMatch(1));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.bitload.com", "locale", "de");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex nameAndSize = br.getRegex("Ihre Datei <strong>(.*?) \\((\\d+,\\d+ .*?)\\)</strong> wird angefordert");
        String filename = nameAndSize.getMatch(0);
        if (filename == null) filename = br.getRegex("Sie möchten <strong>(.*?)</strong> schauen <br/>").getMatch(0);
        String filesize = nameAndSize.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        // Streamlinks show no filesize
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL() + "?c=free");
        String dllink = br.getRegex("bis die Datei bereitgestellt wird!</div>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://ms\\d+\\.mystream\\.to/file-\\d+/[A-Za-z0-9]+/.*?)\"").getMatch(0);
            if (dllink == null) {
                // For Streamlinks
                dllink = br.getRegex("var url = \\'(http://.*?)\\'").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\\'(http://ms\\d+\\.mystream\\.to/file-\\d+/[A-Za-z0-9]+/.*?)\\'").getMatch(0);
                }
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}