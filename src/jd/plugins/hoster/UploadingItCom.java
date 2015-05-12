//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadingit.com" }, urls = { "http://(www\\.)?uploadingit\\.com/((d/|get/)[A-Z0-9]{16}|file/(view/)?[a-zA-Z0-9]{16}/.{1})" }, flags = { 0 })
public class UploadingItCom extends PluginForHost {

    private static String ua = RandomUserAgent.generate();

    public UploadingItCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uploadingit.com/help/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // Waittime is skippable
        // sleep(12 * 1001l, downloadLink);;
        String postUrl = br.getRegex("<form action=\"(file/download/.*?)\"").getMatch(0);
        if (postUrl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        postUrl = "http://uploadingit.com/" + postUrl;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, postUrl, "a=download", true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        // br.getPage(link.getDownloadURL());
        dllink = downloadLink.getDownloadURL();
        URLConnectionAdapter con = null;
        // In case the link are directlinks! current cloudflare implementation will actually open them!
        br.setFollowRedirects(true);
        try {
            con = getConnection(br, downloadLink);
            if (!con.getContentType().contains("html")) {
                // is file
                downloadLink.setFinalFileName(getFileNameFromHeader(con));
                downloadLink.setDownloadSize(con.getLongContentLength());
                return AvailableStatus.TRUE;
            } else {
                // is html
                con = br.openGetConnection(dllink);
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML("(<title>Invalid Download - Uploadingit</title>|\">Sorry, but according to our database the download link you have entered is not valid\\.)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(">Oh Snap! File Not Found!<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"download_filename\">(.*?)</div>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<td style=\"width:150px\">(.*?)</td>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Visit Uploadingit\\.com for free file hosting\\.\\&quot;\\&gt;Download (.*?) from Uploadingit\\.com\\&lt;/a\\&gt;\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"downloadTitle\">(.*?)<").getMatch(0);
                }
                if (filename == null) {
                    filename = br.getRegex("<title>(Downloading: )?(.*?) - Uploadingit</title>").getMatch(1);
                }
            }
        }
        String filesize = br.getRegex(" class=\"download_filesize\">\\((.*?)\\)</div>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"downloadSize\">\\((.*?)\\)<").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private final boolean isNewJD() {
        return System.getProperty("jd.revision.jdownloaderrevision") != null ? true : false;
    }

    private boolean preferHeadRequest = true && isNewJD();
    private String  dllink            = null;

    private URLConnectionAdapter getConnection(final Browser br, final DownloadLink downloadLink) throws IOException {
        URLConnectionAdapter urlConnection = null;
        boolean rangeHeader = false;
        try {
            if (downloadLink.getProperty("streamMod") != null) {
                rangeHeader = true;
                br.getHeaders().put("Range", "bytes=" + 0 + "-");
            }
            if (downloadLink.getStringProperty("post", null) != null) {
                urlConnection = br.openPostConnection(dllink, downloadLink.getStringProperty("post", null));
            } else {
                try {
                    if (!preferHeadRequest || "GET".equals(downloadLink.getStringProperty("requestType", null))) {
                        urlConnection = br.openGetConnection(dllink);
                    } else if (preferHeadRequest || "HEAD".equals(downloadLink.getStringProperty("requestType", null))) {
                        urlConnection = br.openHeadConnection(dllink);
                        if (urlConnection.getResponseCode() == 404 && StringUtils.contains(urlConnection.getHeaderField("Cache-Control"), "must-revalidate") && urlConnection.getHeaderField("Via") != null) {
                            urlConnection.disconnect();
                            urlConnection = br.openGetConnection(dllink);
                        } else if (urlConnection.getResponseCode() != 404 && urlConnection.getResponseCode() >= 300) {
                            // no head support?
                            urlConnection.disconnect();
                            urlConnection = br.openGetConnection(dllink);
                        } else if (urlConnection.getContentType().contains("html")) {
                            urlConnection.disconnect();
                            urlConnection = br.openGetConnection(dllink);
                        }
                    } else {
                        urlConnection = br.openGetConnection(dllink);
                    }
                } catch (final IOException e) {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (preferHeadRequest || "HEAD".equals(downloadLink.getStringProperty("requestType", null))) {
                        /* some servers do not allow head requests */
                        urlConnection = br.openGetConnection(dllink);
                        downloadLink.setProperty("requestType", "GET");
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            if (rangeHeader) {
                br.getHeaders().remove("Range");
            }
        }
        return urlConnection;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}