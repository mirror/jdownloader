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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.TbCm;
import jd.plugins.decrypter.TbCm.DestinationFormat;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spiegel.de" }, urls = { "http://video\\.spiegel\\.de/flash/.+?\\.flv|http://video\\.promobil2spiegel\\.netbiscuits\\.com/.+?\\.(3gp|mp4)|http://www.spiegel.de/img/.+?(\\.\\w+)" }, flags = { 0 })
public class SpiegelDe extends PluginForHost {

    private static final String AGB_LINK = "http://www.spiegel.de/agb";
    private static final Pattern PATTERN_SUPPORTED_FOTOSTRECKE = Pattern.compile("http://www.spiegel.de/img/.+?(\\.\\w+)");
    private static final Pattern PATTERN_SUPPORTED_VIDEO = Pattern.compile("http://video\\.spiegel\\.de/flash/.+?\\.flv|http://video\\.promobil2spiegel\\.netbiscuits\\.com/.+?\\.(3gp|mp4)");

    public SpiegelDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return SpiegelDe.AGB_LINK;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), false, 1);

        if (new Regex(downloadLink.getDownloadURL(), SpiegelDe.PATTERN_SUPPORTED_FOTOSTRECKE).matches()) {
            this.dl.startDownload();
        } else if (new Regex(downloadLink.getDownloadURL(), SpiegelDe.PATTERN_SUPPORTED_VIDEO).matches()) {
            if (this.dl.startDownload()) {
                if (downloadLink.getProperty("convertto") != null) {
                    final DestinationFormat convertTo = DestinationFormat.valueOf(downloadLink.getProperty("convertto").toString());
                    DestinationFormat inType;
                    if (convertTo == DestinationFormat.VIDEOIPHONE || convertTo == DestinationFormat.VIDEOMP4 || convertTo == DestinationFormat.VIDEO3GP) {
                        inType = convertTo;
                    } else {
                        inType = DestinationFormat.VIDEOFLV;
                    }
                    /* to load the TbCm plugin */
                    JDUtilities.getPluginForDecrypt("youtube.com");
                    if (!TbCm.ConvertFile(downloadLink, inType, convertTo)) {
                        logger.severe("Video-Convert failed!");
                    }

                }
            }
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException {
        URLConnectionAdapter urlConnection;
        try {
            urlConnection = this.br.openGetConnection(downloadLink.getDownloadURL());
        } catch (final IOException e) {
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

    public void resetDownloadlink(final DownloadLink link) {
    }

}
