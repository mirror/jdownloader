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
import jd.plugins.download.DownloadInterfaceFactory;
import jd.plugins.download.RAFDownload;

public class BrowserAdapter {

    public static final int ERROR_REDIRECTED = -1;

    private static DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request) throws Exception {
        Thread current = Thread.currentThread();
        if (current instanceof DownloadInterfaceFactory) { return ((DownloadInterfaceFactory) current).getDownloadInterface(downloadLink, request); }
        PluginForHost livePlugin = downloadLink.getLivePlugin();
        DownloadInterfaceFactory ret = null;
        if (livePlugin != null && (ret = livePlugin.getCustomizedDownloadFactory()) != null) return ret.getDownloadInterface(downloadLink, request);
        return RAFDownload.download(downloadLink, request);
    }

    private static DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request, boolean b, int i) throws Exception {
        Thread current = Thread.currentThread();
        if (current instanceof DownloadInterfaceFactory) { return ((DownloadInterfaceFactory) current).getDownloadInterface(downloadLink, request, b, i); }
        PluginForHost livePlugin = downloadLink.getLivePlugin();
        DownloadInterfaceFactory ret = null;
        if (livePlugin != null && (ret = livePlugin.getCustomizedDownloadFactory()) != null) return ret.getDownloadInterface(downloadLink, request, b, i);
        return RAFDownload.download(downloadLink, request, b, i);
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        Request r;
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadLink, r = br.createGetRequest(link));
        try {
            dl.connect(br);
        } catch (PluginException e) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable e2) {
            }
            if (e.getValue() == ERROR_REDIRECTED) {
                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    r = br.createGetRequestRedirectedRequest(r);
                    if (originalUrl != null) r.getHeaders().put("Referer", originalUrl);
                    dl = getDownloadInterface(downloadLink, r);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable e22) {
                        }
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }

            }
        }
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        Request r;
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadLink, r = br.createPostRequest(url, postdata));
        try {
            dl.connect(br);
        } catch (PluginException e) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable e2) {
            }
            if (e.getValue() == ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    r = br.createPostRequestfromRedirectedRequest(r, postdata);
                    if (originalUrl != null) r.getHeaders().put("Referer", originalUrl);
                    dl = getDownloadInterface(downloadLink, r);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable e22) {
                        }
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }
            }
        }
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata, boolean b, int c) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        Request r;
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadLink, r = br.createPostRequest(url, postdata), b, c);
        try {
            dl.connect(br);
        } catch (PluginException e) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable e2) {
            }
            if (e.getValue() == ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    r = br.createPostRequestfromRedirectedRequest(r, postdata);
                    if (originalUrl != null) r.getHeaders().put("Referer", originalUrl);
                    dl = getDownloadInterface(downloadLink, r, b, c);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable e22) {
                        }
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }
            }
        }
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        return dl;
    }

    /**
     * returns a DownloadInterface you need to call .startDownload() on to start the download
     * 
     * @param br
     *            a browser
     * @param downloadLink
     *            the untouched download link
     * @param link
     *            direct link to start the download
     * @param b
     *            is this download resumeable
     * @param c
     *            number of chunks, positive value to force this number of chunks, negative to set max chunks ("-5" sets max to 5 chunks)
     * @return DownloadInterface
     * @throws Exception
     */
    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean b, int c) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        Request r;
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadLink, r = br.createRequest(link), b, c);
        try {
            dl.connect(br);
        } catch (PluginException e) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable e2) {
            }
            if (e.getValue() == ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    r = br.createGetRequestRedirectedRequest(r);
                    if (originalUrl != null) r.getHeaders().put("Referer", originalUrl);
                    dl = getDownloadInterface(downloadLink, r, b, c);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable e22) {
                        }
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }

            }
        }
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form, boolean resume, int chunks) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        Request r;
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadLink, r = br.createRequest(form), resume, chunks);
        try {
            dl.connect(br);
        } catch (PluginException e) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable e2) {
            }
            if (e.getValue() == ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    r = br.createGetRequestRedirectedRequest(r);
                    if (originalUrl != null) r.getHeaders().put("Referer", originalUrl);
                    dl = getDownloadInterface(downloadLink, r, resume, chunks);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable e22) {
                        }
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }

            }
        }
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        Request r;
        String originalUrl = br.getURL();
        DownloadInterface dl = getDownloadInterface(downloadLink, r = br.createRequest(form));
        try {
            dl.connect(br);
        } catch (PluginException e) {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable e2) {
            }
            if (e.getValue() == ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    r = br.createGetRequestRedirectedRequest(r);
                    if (originalUrl != null) r.getHeaders().put("Referer", originalUrl);
                    dl = getDownloadInterface(downloadLink, r);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        try {
                            dl.getConnection().disconnect();
                        } catch (Throwable e22) {
                        }
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }
            }
        }
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        return dl;
    }
}
