//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDMediaConvert;

public class Youtube extends PluginForHost {
    static private final String CODER = "JD-Team";
    static private final String HOST = "youtube.com";
    static private final String AGB = "http://youtube.com/t/terms";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?youtube\\.com/get_video\\?video_id=.+&t=.+(&fmt=\\d+)?", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;

    public Youtube() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return AGB;
    }

    @Override
    public String getCoder() {
        return CODER;
    }
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadLink.getDownloadURL()), null, null, true);
            if (requestInfo.getResponseCode() == 200) { return true; }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return 50;
    }

    @Override
   */ public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(HOST + " " + JDLocale.L("plugins.host.server.unavailable", "Serverfehler"));
            return;
        }
        HTTPConnection urlConnection = requestInfo.getConnection();
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        if (dl.startDownload()) {
            if (downloadLink.getProperty("convertto") != null) {
                ConversionMode convertto = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
                ConversionMode InType = ConversionMode.VIDEOFLV;
                if (convertto.equals(ConversionMode.VIDEOMP4) || convertto.equals(ConversionMode.VIDEO3GP)) {
                    InType = convertto;
                }

                if (!JDMediaConvert.ConvertFile(downloadLink, InType, convertto)) {
                    logger.severe("Video-Convert failed!");
                }
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
