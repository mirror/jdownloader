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
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

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

    private String getBasicAuth(DownloadLink link) {
        String username = null;
        String password = null;
        try {
            username = getUserInput("Username(BasicAuth)", link);
            password = getUserInput("Password(BasicAuth)", link);
        } catch (Exception e) {
            return null;
        }
        return "Basic " + Encoding.Base64Encode(username + ":" + password);
    }

    private String removeBasicAuthfromURL(DownloadLink link) {
        String url = link.getDownloadURL();
        String basicauth = new Regex(url, "http.*?/([^/]{1}.*?)@").getMatch(0);
        if (basicauth != null && basicauth.contains(":")) {
            url = new Regex(url, "http.*?@(.+)").getMatch(0);
            if (url != null) link.setUrlDownload("http://" + url);
            return "Basic " + Encoding.Base64Encode(basicauth);
        }
        return null;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("httpviajd://", "http://"));
        String basicauth = removeBasicAuthfromURL(downloadLink);
        if (basicauth == null) basicauth = (String) downloadLink.getProperty("basicauth", null);
        if (basicauth != null) {
            br.getHeaders().put("Authorization", basicauth);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
            if (urlConnection.getResponseCode() == 401) {
                if (basicauth != null) {
                    downloadLink.setProperty("basicauth", null);
                }
                urlConnection.disconnect();
                basicauth = getBasicAuth(downloadLink);
                if (basicauth == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "BasicAuth is needed!");
                br.getHeaders().put("Authorization", basicauth);
                urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
                if (urlConnection.getResponseCode() == 401) {
                    urlConnection.disconnect();
                    downloadLink.setProperty("basicauth", null);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "BasicAuth is needed!");
                } else {
                    downloadLink.setProperty("basicauth", basicauth);
                }
            }
            if (urlConnection.getResponseCode() == 404 || !urlConnection.isOK()) {
                urlConnection.disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setFinalFileName(Plugin.getFileNameFormHeader(urlConnection));
            downloadLink.setBrowserUrl(downloadLink.getDownloadURL());
            downloadLink.setDownloadSize(urlConnection.getContentLength());
            downloadLink.setDupecheckAllowed(true);
            this.contentType = urlConnection.getContentType();
            urlConnection.disconnect();
            return true;
        } catch (IOException e) {
            if (urlConnection != null && urlConnection.isConnected() == true) urlConnection.disconnect();
            e.printStackTrace();
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
        boolean resume = true;
        int chunks = 0;

        if (downloadLink.getBooleanProperty("nochunkload", false) == true) resume = false;
        if (downloadLink.getBooleanProperty("nochunk", false) == true || resume == false) {
            chunks = 1;
        }
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL(), resume, chunks);

        if (!dl.startDownload()) {
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().contains("rangeheader")) {
                if (downloadLink.getBooleanProperty("nochunk", false) == false) {
                    downloadLink.setProperty("nochunk", new Boolean(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().contains("chunkload")) {
                if (downloadLink.getBooleanProperty("nochunkload", false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty("nochunkload", new Boolean(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void reset_downloadlink(DownloadLink link) {
        link.setProperty("nochunkload", false);
        link.setProperty("nochunk", false);
        link.setProperty("basicauth", null);
    }

    @Override
    public void resetPluginGlobals() {
    }

}