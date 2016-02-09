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

package jd.plugins;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.Downloadable;
import jd.plugins.download.raf.HTTPDownloader;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.settings.GeneralSettings;

public class BrowserAdapter {

    private static DownloadInterface getDownloadInterface(Downloadable downloadable, Request request, boolean resumeEnabled, int chunksCount) throws Exception {
        HTTPDownloader dl = new HTTPDownloader(downloadable, request);
        int chunks = downloadable.getChunks();
        if (chunksCount == 0) {
            dl.setChunkNum(chunks <= 0 ? JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile() : chunks);
        } else {
            dl.setChunkNum(chunksCount < 0 ? Math.min(chunksCount * -1, chunks <= 0 ? JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile() : chunks) : chunksCount);
        }
        dl.setMaxChunksNum(Math.abs(chunksCount));
        dl.setResume(resumeEnabled);
        return dl;
    }

    public static Downloadable getDownloadable(DownloadLink downloadLink, Browser br) {
        final SingleDownloadController controller = downloadLink.getDownloadLinkController();
        if (controller != null) {
            final PluginForHost plugin = controller.getProcessingPlugin();
            if (plugin != null) {
                return plugin.newDownloadable(downloadLink, br);
            }
        }
        return null;
    }

    public static DownloadInterface openDownload(final Browser br, DownloadLink downloadLink, String link) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createRequest(link), false, 1);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata) throws Exception {
        return openDownload(br, downloadLink, url, postdata, false, 1);
    }

    public static DownloadInterface openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int chunks) throws Exception {
        final String originalUrl = br.getURL();
        int maxRedirects = 10;
        DownloadInterface dl = getDownloadInterface(downloadable, request, resume, chunks);
        downloadable.setDownloadInterface(dl);
        while (maxRedirects-- > 0) {
            dl.setInitialRequest(request);
            URLConnectionAdapter connection = dl.connect(br);
            if (connection.getRequest().getLocation() == null) {
                return dl;
            }
            connection.disconnect();
            request = br.createRedirectFollowingRequest(request);
            if (originalUrl != null) {
                request.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, originalUrl);
            } else if (dl.getConnection().getResponseCode() == 403 && dl.getConnection().getHeaderField("Server") != null && dl.getConnection().getHeaderField("Server").matches("^Zscaler/.+")) {
                // Zscaler, corporate firewall/antivirus ? http://www.zscaler.com/ jdlog://2660609980341
                // ----------------Response------------------------
                // HTTP/1.1 403 Forbidden
                // Content-Type: text/html
                // Server: Zscaler/5.0
                // Cache-Control: no-cache
                // Content-length: 10135
                // ------------------------------------------------

                // <title>Threat download blocked</title>
                // ..
                // <span><font color="black" size=6><p>For security reasons your request was blocked.<p>If you feel you've reached this page
                // in error, contact Helpdesk at the email address below</td>
                throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked by Zscaler");

            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Redirectloop");
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata, boolean resume, int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createPostRequest(url, postdata), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean resume, int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createRequest(link), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form, boolean resume, int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createRequest(form), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form) throws Exception {
        return openDownload(br, downloadLink, form, false, 1);
    }

}
