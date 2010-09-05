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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.decrypter.TbCm.DestinationFormat;
import jd.utils.JDMediaConvert;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "clipfish.de" }, urls = { "http://[\\w\\.]*?pg\\d+\\.clipfish\\.de/media/.+?\\.flv" }, flags = { 0 })
public class ClipfishDe extends PluginForHost {
    private static final String AGB_LINK = "http://www.clipfish.de/agb/";

    public ClipfishDe(final PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return ClipfishDe.AGB_LINK;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        final LinkStatus linkStatus = downloadLink.getLinkStatus();

        URLConnectionAdapter urlConnection;
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL());
        urlConnection = this.dl.connect();
        if (urlConnection.getLongContentLength() == 0) {
            this.br.followConnection();
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
            return;
        }

        if (this.dl.startDownload()) {
            if (downloadLink.getProperty("convertto") != null) {
                final DestinationFormat convertTo = DestinationFormat.valueOf(downloadLink.getProperty("convertto").toString());
                final DestinationFormat inType = DestinationFormat.VIDEOFLV;

                if (!JDMediaConvert.ConvertFile(downloadLink, inType, convertTo)) {
                    Plugin.logger.severe("Video-Convert failed!");
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) {
        /*
         * warum sollte ein video das der decrypter sagte es sei online, offline
         * sein ;)
         * 
         * coa: hm.. weil er vieleicht so nem anderen zeitpunk eingefügt worden
         * ist als er dann geladen wird?
         */
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
