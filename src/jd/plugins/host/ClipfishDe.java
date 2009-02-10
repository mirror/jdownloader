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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDMediaConvert;

public class ClipfishDe extends PluginForHost {
    public ClipfishDe(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    private static final String AGB_LINK = "http://www.clipfish.de/agb/";

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            URLConnectionAdapter urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
            if (!urlConnection.isOK()) return false;
            downloadLink.setDownloadSize(urlConnection.getContentLength());
            return true;
        } catch (IOException e) {
            logger.severe(e.getMessage());
            downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return false;
        }
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        br.openGetConnection(downloadLink.getDownloadURL());
        URLConnectionAdapter urlConnection;
        dl = new RAFDownload(this, downloadLink, br.createGetRequest(downloadLink.getDownloadURL()));
        dl.setChunkNum(1);
        dl.setResume(false);
        urlConnection = dl.connect();
        if (urlConnection.getContentLength() == 0) {
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

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
