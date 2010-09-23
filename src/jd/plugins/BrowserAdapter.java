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
import jd.parser.html.Form;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.RAFDownload;

public class BrowserAdapter {
    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        DownloadInterface dl = RAFDownload.download(downloadLink, br.createGetRequest(link));
        try {
            dl.connect(br);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, br.createGetRequestRedirectedRequest(dl.getRequest()));
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }

            }
        }
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        DownloadInterface dl = RAFDownload.download(downloadLink, br.createPostRequest(url, postdata));
        try {
            dl.connect(br);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, br.createPostRequestfromRedirectedRequest(dl.getRequest(), postdata));
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }
            }
        }
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String url, String postdata, boolean b, int c) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        DownloadInterface dl = RAFDownload.download(downloadLink, br.createPostRequest(url, postdata), b, c);
        try {
            dl.connect(br);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, br.createPostRequestfromRedirectedRequest(dl.getRequest(), postdata), b, c);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }
            }
        }
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, String link, boolean b, int c) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        DownloadInterface dl = RAFDownload.download(downloadLink, br.createRequest(link), b, c);
        try {
            dl.connect(br);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, br.createGetRequestRedirectedRequest(dl.getRequest()), b, c);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }

            }
        }
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form, boolean resume, int chunks) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        DownloadInterface dl = RAFDownload.download(downloadLink, br.createRequest(form), resume, chunks);
        try {
            dl.connect(br);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, br.createGetRequestRedirectedRequest(dl.getRequest()), resume, chunks);
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }

            }
        }
        return dl;
    }

    public static DownloadInterface openDownload(Browser br, DownloadLink downloadLink, Form form) throws Exception {
        if (downloadLink.getLivePlugin() == null) return null;
        DownloadInterface dl = RAFDownload.download(downloadLink, br.createRequest(form));
        try {
            dl.connect(br);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, br.createGetRequestRedirectedRequest(dl.getRequest()));
                    try {
                        dl.connect(br);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }
            }
        }
        return dl;
    }
}
