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
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.raf.HTTPDownloader;
import jd.plugins.download.raf.OldRAFDownload;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.DebugMode;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.settings.GeneralSettings;

public class BrowserAdapter {

    @Deprecated
    public static final int      ERROR_REDIRECTED = -1;
    private static final boolean NEW_CORE         = DebugMode.TRUE_IN_IDE_ELSE_FALSE;

    private static DownloadInterface getDownloadInterface(Downloadable downloadable, Request request, boolean resumeEnabled, int pluginConnections) throws Exception {
        final DownloadInterface dl;
        if (NEW_CORE) {
            dl = new HTTPDownloader(downloadable, request);
        } else {
            if (false) {
                // was fine all the years, so better optimize handling in download system to better support edge cases with downloading
                // content-encoding connections
                request.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity", false));
            }
            dl = new jd.plugins.download.raf.OldRAFDownload(downloadable, request);
        }
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
        if (dl instanceof HTTPDownloader) {
            ((HTTPDownloader) dl).setChunkNum(setConnections);
            ((HTTPDownloader) dl).setMaxChunksNum(Math.abs(pluginConnections));
            ((HTTPDownloader) dl).setResume(resumeEnabled);
        } else if (dl instanceof OldRAFDownload) {
            ((OldRAFDownload) dl).setChunkNum(setConnections);
            ((OldRAFDownload) dl).setResume(resumeEnabled);
        }
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

    @Deprecated
    public static DownloadInterface openDownload(final Browser br, DownloadLink downloadLink, String link) throws Exception {
        return openDownload(br, downloadLink, br.createRequest(link), false, 1);
    }

    public static DownloadInterface openDownload(Browser br, Downloadable downloadable, Request request, boolean resume, int chunks) throws Exception {
        if (NEW_CORE) {
            final String originalUrl = br.getURL();
            int maxRedirects = 10;
            final DownloadInterface dl = getDownloadInterface(downloadable, request, resume, chunks);
            downloadable.setDownloadInterface(dl);
            while (maxRedirects-- > 0) {
                dl.setInitialRequest(request);
                final URLConnectionAdapter connection = dl.connect(br);
                if (connection.getRequest().getLocation() == null) {
                    return dl;
                } else {
                    connection.disconnect();
                    request = br.createRedirectFollowingRequest(request);
                    if (originalUrl != null) {
                        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, originalUrl);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Redirectloop");
        } else {
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
                        if (lastRedirectUrl != null && redirectUrl.equals(lastRedirectUrl)) {
                            // some providers don't like fast redirects, as they use this for preparing final file. lets add short wait
                            // based on
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

    public static DownloadInterface openDownload(final Browser br, final DownloadLink downloadLink, final Request request, final boolean resume, final int chunks) throws Exception {
        return openDownload(br, getDownloadable(downloadLink, br), request, resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata, boolean resume, int chunks) throws Exception {
        return openDownload(br, downloadLink, br.createPostRequest(url, postdata), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean resume, int chunks) throws Exception {
        return openDownload(br, downloadLink, br.createRequest(link), resume, chunks);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form, boolean resume, int chunks) throws Exception {
        return openDownload(br, downloadLink, br.createRequest(form), resume, chunks);
    }

}
