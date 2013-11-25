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

import jd.http.Browser;
import jd.http.Request;
import jd.parser.html.Form;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.raf.OldRAFDownload;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.downloadcore.v15.Downloadable;
import org.jdownloader.settings.GeneralSettings;

public class BrowserAdapter {

    public static final int ERROR_REDIRECTED = -1;

    private static DownloadInterface getDownloadInterface(Downloadable downloadable, Request request, boolean resumeEnabled, int chunksCount) throws Exception {
        OldRAFDownload dl = new OldRAFDownload(downloadable, request);
        if (chunksCount == 0) {
            dl.setChunkNum(JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile());
        } else {
            dl.setChunkNum(chunksCount < 0 ? Math.min(chunksCount * -1, JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile()) : chunksCount);
        }
        dl.setResume(resumeEnabled);
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link) throws Exception {
        return openDownload(br, new DownloadLinkDownloadable(downloadLink), br.createRequest(link), false, 1);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata) throws Exception {
        return openDownload(br, downloadLink, url, postdata, false, 1);
    }

    public static DownloadInterface openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int chunks) throws Exception {
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadable, request, resume, chunks);
        downloadable.setDownloadInterface(dl);
        try {
            dl.connect(br);
        } catch (PluginException handle) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            if (handle.getValue() == ERROR_REDIRECTED) {
                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    request = br.createRedirectFollowingRequest(request);
                    if (originalUrl != null) request.getHeaders().put("Referer", originalUrl);
                    dl = getDownloadInterface(downloadable, request, resume, chunks);
                    downloadable.setDownloadInterface(dl);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException handle2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable ignore) {
                        }
                        if (handle2.getValue() == ERROR_REDIRECTED) {
                            continue;
                        } else {
                            throw handle2;
                        }
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }
            } else {
                throw handle;
            }
        } catch (Exception forward) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            throw forward;
        }
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata, boolean resume, int chunks) throws Exception {
        return openDownload(br, new DownloadLinkDownloadable(downloadLink), br.createPostRequest(url, postdata), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean resume, int chunks) throws Exception {
        return openDownload(br, new DownloadLinkDownloadable(downloadLink), br.createRequest(link), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form, boolean resume, int chunks) throws Exception {
        return openDownload(br, new DownloadLinkDownloadable(downloadLink), br.createRequest(form), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form) throws Exception {
        return openDownload(br, downloadLink, form, false, 1);
    }

}
