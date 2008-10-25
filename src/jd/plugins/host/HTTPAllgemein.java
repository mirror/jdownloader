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

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.http.HTTPConnection;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class HTTPAllgemein extends PluginForHost {

    private String contentType;

    public HTTPAllgemein(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    public String getFileInformationString(DownloadLink parameter) {
        return "(" + contentType + ")" + parameter.getName();
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException {
        String linkurl;
        downloadLink.setUrlDownload(linkurl = downloadLink.getDownloadURL().replaceAll("httpviajd://", "http://"));

        if (linkurl != null) {
            br.setFollowRedirects(true);

            HTTPConnection urlConnection;
            try {
                urlConnection = br.openGetConnection(linkurl);
                if (!urlConnection.isOK()) return false;
                downloadLink.setName(Plugin.getFileNameFormHeader(urlConnection));
                downloadLink.setBrowserUrl(linkurl);
                downloadLink.setDownloadSize(urlConnection.getContentLength());
                urlConnection.disconnect();
                this.contentType = urlConnection.getContentType();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL());
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}