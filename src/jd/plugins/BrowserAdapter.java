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

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.html.Form;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;

public class BrowserAdapter {
    public static final int ERROR_REDIRECTED = -1;

    private static DownloadInterface getDownloadInterface(Downloadable downloadable, Request request, boolean resumeEnabled, int pluginConnections) throws Exception {
        final jd.plugins.download.raf.OldRAFDownload dl = new jd.plugins.download.raf.OldRAFDownload(downloadable, request);
        final int customizedConnections = downloadable.getChunks();
        final int setConnections;
        if (pluginConnections > 0) {
            // pluginConnection has fixed value
            if (customizedConnections == 0) {
                // no customized, use pluginConnections
                setConnections = pluginConnections;
            } else {
                // use smaller of pluginConnections and customizedConnections
                setConnections = Math.min(pluginConnections, Math.abs(customizedConnections));
            }
        } else if (pluginConnections == 0) {
            // pluginConnection has no limits
            final int max = JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile();
            if (customizedConnections == 0) {
                // no limits for pluginConnections, use max
                setConnections = max;
            } else if (customizedConnections > 0) {
                // prefer customizedConnections over max
                setConnections = customizedConnections;
            } else {
                // use smaller of customizedConnections and max
                setConnections = Math.min(max, Math.abs(customizedConnections));
            }
        } else {
            // pluginConnections has limits
            final int max = JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile();
            if (customizedConnections == 0) {
                // use smaller of pluginsConnections and max
                setConnections = Math.min(max, Math.abs(pluginConnections));
            } else {
                // use smaller of pluginsConnections and customizedConnections
                setConnections = Math.min(customizedConnections, Math.abs(pluginConnections));
            }
        }
        dl.setChunkNum(setConnections);
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

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createRequest(link), false, 1, false);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata) throws Exception {
        return openDownload(br, downloadLink, url, postdata, false, 1);
    }

    /**
     *
     * @param br
     * @param downloadLink
     * @param url
     * @param postdata
     * @param resume
     *            true|false, if chunks over 1 it must be true!
     * @param chunks
     *            0 = unlimited, chunks must start with negative sign otherwise it forces value to be used instead of up to value.
     * @return
     * @throws Exception
     */
    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata, boolean resume, int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createPostRequest(url, postdata), resume, chunks, false);
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
    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean resume, int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createRequest(link), resume, chunks, false);
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
     * @param forceRedirectWait
     *            forces thread sleep between redirects (even if they are not the same, useful for (multi)hosters which wait via series
     *            redirects)
     * @return
     * @throws Exception
     */
    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean resume, int chunks, final boolean forceRedirectWait) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createRequest(link), resume, chunks, forceRedirectWait);
    }

    /**
     *
     * @param br
     * @param downloadLink
     * @param form
     * @param resume
     *            true|false, if chunks over 1 it must be true!
     * @param chunks
     *            0 = unlimited, chunks must start with negative sign otherwise it forces value to be used instead of up to value.
     * @return
     * @throws Exception
     */
    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form, boolean resume, int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), br.createRequest(form), resume, chunks, false);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form) throws Exception {
        return openDownload(br, downloadLink, form, false, 1);
    }

    /**
     *
     * @param br
     * @param downloadLink
     * @param request
     * @param resume
     * @param chunks
     * @return
     * @throws Exception
     */
    public static DownloadInterface openDownload(final Browser br, final DownloadLink downloadLink, final Request request, final boolean resume, final int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), request, resume, chunks, false);
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
    public static DownloadInterface openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int chunks) throws Exception {
        return openDownload(br, downloadable, request, resume, chunks, false);
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
     * @param forceRedirectWait
     *            forces thread sleep between redirects (even if they are not the same, useful for (multi)hosters which wait via series
     *            redirects)
     * @return
     * @throws Exception
     */
    public static DownloadInterface openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int chunks, boolean forceRedirectWait) throws Exception {
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadable, request, resume, chunks);
        downloadable.setDownloadInterface(dl);
        final PluginForHost plugin;
        final DownloadLink downloadLink;
        if (downloadable instanceof DownloadLinkDownloadable) {
            plugin = ((DownloadLinkDownloadable) downloadable).getPlugin();
            downloadLink = ((DownloadLinkDownloadable) downloadable).getDownloadLink();
        } else {
            plugin = null;
            downloadLink = null;
        }
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
                    if (lastRedirectUrl != null && (forceRedirectWait || redirectUrl.equals(lastRedirectUrl))) {
                        // some providers don't like fast redirects, as they use this for preparing final file. lets add short wait based on
                        // retry count
                        if (plugin != null && downloadLink != null) {
                            plugin.sleep(redirect_count * 250l, downloadLink);
                        } else {
                            Thread.sleep(redirect_count * 250l);
                        }
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
}
