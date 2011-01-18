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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "przeslij.net" }, urls = { "http://www[\\d]?\\.przeslij\\.net/download\\.php\\?file=(.*)" }, flags = { 0 })
public class Przeslijnet extends PluginForHost {

    public Przeslijnet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www2.przeslij.net/#";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.getPage(downloadLink.getDownloadURL());
            if (!br.containsHTML("Invalid download link")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("<font color=#000000>(.*?)</font>").getMatch(0)));
                downloadLink.setDownloadSize(SizeFormatter.getSize(br.getRegex("File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*?)</td>").getMatch(0)));
                return AvailableStatus.TRUE;
            }
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        String dllink = Encoding.htmlDecode(br.getRegex("onClick=\"window\\.location=\\\\\'(.*?)\\\\\'").getMatch(0));
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* Datei herunterladen */
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink).startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
