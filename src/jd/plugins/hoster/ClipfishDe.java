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
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDMediaConvert;

@HostPlugin(revision="$Revision", interfaceVersion=2, names = { "clipfish.de"}, urls ={ "http://[\\w\\.]*?pg\\d+\\.clipfish\\.de/media/.+?\\.flv"}, flags = {0})
public class ClipfishDe extends PluginForHost {
    public ClipfishDe(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    private static final String AGB_LINK = "http://www.clipfish.de/agb/";

    // @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) {
        /*
         * warum sollte ein video das der decrypter sagte es sei online, offline
         * sein ;)
         * 
         * coa: hm.. weil er vieleicht so nem anderen zeitpunk eingefügt worden
         * ist als er dann geladen wird?
         */
        return AvailableStatus.TRUE;
    }

    // @Override
    /* /* public String getVersion() {

        return getVersion("$Revision$");
    } */

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        URLConnectionAdapter urlConnection;
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL());
        urlConnection = dl.connect();
        if (urlConnection.getLongContentLength() == 0) {
            urlConnection.disconnect();
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }

        if (dl.startDownload()) {
            if (downloadLink.getProperty("convertto") != null) {
                ConversionMode convertTo = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
                ConversionMode inType = ConversionMode.VIDEOFLV;

                if (!JDMediaConvert.ConvertFile(downloadLink, inType, convertTo)) {
                    logger.severe("Video-Convert failed!");
                }
            }
        }
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}
