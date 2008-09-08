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

import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;

public class YouPornCom extends PluginForHost {
    private static final String CODER = "JD-Team";
    

    private RequestInfo requestInfo;

    @Override
    public String getAGBLink() {
        return "http://youporn.com/terms";
    }

    @Override
    public boolean getFileInformation(DownloadLink parameter) {
        try {
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(parameter.getDownloadURL()), null, null, true);
            HTTPConnection urlConnection = requestInfo.getConnection();
            parameter.setName(Plugin.getFileNameFormHeader(urlConnection));
            parameter.setDownloadSize(urlConnection.getContentLength());
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        parameter.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
        return false;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        LinkStatus linkStatus = link.getLinkStatus();
        if (!getFileInformation(link)) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(getHost() + " " + JDLocale.L("plugins.host.server.unavailable", "Serverfehler"));
            return;
        }
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, link, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert prüfen */
        return 1;
    }
    
    @Override
    public void reset() {
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return CODER;
    }



    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
