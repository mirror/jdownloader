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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDMediaConvert;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spiegel.de" }, urls = { "http://video\\.spiegel\\.de/flash/.+?\\.flv|http://video\\.promobil2spiegel\\.netbiscuits\\.com/.+?\\.(3gp|mp4)|http://www.spiegel.de/img/.+?(\\.\\w+)" }, flags = { 0 })
public class SpiegelDe extends PluginForHost {

    private static final String AGB_LINK = "http://www.spiegel.de/agb";
    private static final Pattern PATTERN_SUPPORTED_FOTOSTRECKE = Pattern.compile("http://www.spiegel.de/img/.+?(\\.\\w+)");
    private static final Pattern PATTERN_SUPPORTED_VIDEO = Pattern.compile("http://video\\.spiegel\\.de/flash/.+?\\.flv|http://video\\.promobil2spiegel\\.netbiscuits\\.com/.+?\\.(3gp|mp4)");

    public SpiegelDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return AGB_LINK;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        URLConnectionAdapter urlConnection;
        try {
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
        } catch (IOException e) {
            logger.severe(e.getMessage());
            downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!urlConnection.isOK()) {
            urlConnection.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setDownloadSize(urlConnection.getLongContentLength());
        urlConnection.disconnect();
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), false, 1);

        if (new Regex(downloadLink.getDownloadURL(), PATTERN_SUPPORTED_FOTOSTRECKE).matches()) {
            dl.startDownload();
        } else if (new Regex(downloadLink.getDownloadURL(), PATTERN_SUPPORTED_VIDEO).matches()) {
            if (dl.startDownload()) {
                if (downloadLink.getProperty("convertto") != null) {
                    ConversionMode convertTo = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
                    ConversionMode inType;
                    if (convertTo == ConversionMode.VIDEOIPHONE || convertTo == ConversionMode.VIDEOPODCAST || convertTo == ConversionMode.VIDEOMP4 || convertTo == ConversionMode.VIDEO3GP) {
                        inType = convertTo;
                    } else {
                        inType = ConversionMode.VIDEOFLV;
                    }
                    if (!JDMediaConvert.ConvertFile(downloadLink, inType, convertTo)) {
                        logger.severe("Video-Convert failed!");
                    }

                }
            }
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
