//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.

//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDMediaConvert;

public class SpiegelDe extends PluginForHost {

    private static final String AGB_LINK = "http://www.spiegel.de/agb";
    private static final Pattern PATTERN_SUPPORTED_FOTOSTRECKE = Pattern.compile("http://www.spiegel.de/img/.+?(\\.\\w+)");
    private static final Pattern PATTERN_SUPPORTED_VIDEO = Pattern.compile("http://video\\.spiegel\\.de/flash/.+?\\.flv|http://video\\.promobil2spiegel\\.netbiscuits\\.com/.+?\\.(3gp|mp4)");

    public SpiegelDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        HTTPConnection urlConnection;
        try {
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
        } catch (IOException e) {
            logger.severe(e.getMessage());
            downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return false;
        }
        if (!urlConnection.isOK()) {
            urlConnection.disconnect();
            return false;
        }
        downloadLink.setDownloadSize(urlConnection.getContentLength());
        urlConnection.disconnect();
        return true;
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision: 3337 $");
    }

    @Override
    public void reset() {
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        dl = new RAFDownload(this, downloadLink, br.createGetRequest(downloadLink.getDownloadURL()));
        dl.setChunkNum(1);
        dl.setResume(false);
        HTTPConnection urlConnection = dl.connect();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        if (new Regex(downloadLink.getDownloadURL(), PATTERN_SUPPORTED_FOTOSTRECKE).matches()) {
            dl.startDownload();
        } else if (new Regex(downloadLink.getDownloadURL(), PATTERN_SUPPORTED_VIDEO).matches()) {
            if (dl.startDownload()) {
                if (downloadLink.getProperty("convertto") != null) {
                    ConversionMode convertTo = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
                    ConversionMode inType;
                    if (convertTo == ConversionMode.IPHONE || convertTo == ConversionMode.PODCAST || convertTo == ConversionMode.VIDEOMP4 || convertTo == ConversionMode.VIDEO3GP) {
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
        return 20;
    }

}
