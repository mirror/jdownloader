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

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.settings.GeneralSettings;

import jd.http.Browser;
import jd.http.Request;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.Downloadable;
import jd.plugins.download.raf.OldRAFDownload;

/**
 * heavily modified download interface by raztoki
 *
 * @author raztoki
 *
 */
public class BrowserDownloadInterface {
    public static final int ERROR_REDIRECTED = -1;

    protected DownloadInterface getDownloadInterface(Downloadable downloadable, Request request, boolean resumeEnabled, int chunksCount) throws Exception {
        final OldRAFDownload dl = new OldRAFDownload(downloadable, request);
        final int chunks = downloadable.getChunks();
        if (chunksCount == 0) {
            dl.setChunkNum(chunks <= 0 ? JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile() : chunks);
        } else {
            dl.setChunkNum(chunksCount < 0 ? Math.min(chunksCount * -1, chunks <= 0 ? JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile() : chunks) : chunksCount);
        }
        dl.setResume(resumeEnabled);
        return dl;
    }

    /**
     *
     * @param br
     * @param downloadable
     * @param request
     * @param resume
     *            true|false, if chunks over 1 it must be true!
     * @param chunks
     *            0 = unlimited, chunks must start with negative sign otherwise it forces value to be used instead of up to value.
     * @return
     * @throws Exception
     */
    protected DownloadInterface openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int chunks) throws Exception {
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
                final int redirect_max = 10;
                int redirect_count = 0;
                String lastRedirectUrl = null;
                while (redirect_count++ < redirect_max) {
                    request = br.createRedirectFollowingRequest(request);
                    final String redirectUrl = request.getUrl();
                    if (lastRedirectUrl != null && redirectUrl.equals(lastRedirectUrl)) {
                        /*
                         * some providers don't like fast redirects, as they use this for preparing final file. lets add short wait based on
                         * retry count
                         */
                        Thread.sleep(redirect_count * 250l);
                    }
                    if (originalUrl != null) {
                        request.getHeaders().put("Referer", originalUrl);
                    }
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
                            lastRedirectUrl = redirectUrl;
                            continue;
                        } else {
                            throw handle2;
                        }
                    }
                }
                if (redirect_count++ >= redirect_max) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop");
                }
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

    /**
     *
     * @param br
     * @param downloadLink
     * @param link
     * @param resume
     *            true|false, if chunks over 1 it must be true!
     * @param chunks
     *            0 = unlimited, chunks must start with negative sign otherwise it forces value to be used instead of up to value.
     * @return
     * @throws Exception
     */
    public DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean resume, int chunks) throws Exception {
        return openDownload(br, BrowserAdapter.getDownloadable(downloadLink, br), br.createRequest(link), resume, chunks);
    }
}
