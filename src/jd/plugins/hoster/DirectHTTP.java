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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.HTACCESSController;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "DirectHTTP", "http links" }, urls = { "directhttp://.+", "https?viajd://[\\d\\w\\.:\\-@]*/.*\\.(otrkey|ac3|3gp|7zip|7z|aiff|aif|aifc|au|avi|bin|bz2|ccf|cue|deb|divx|dlc|dmg|doc|docx|dot|exe|ff|flv|gif|gz|iwd|iso|java|jpg|jpeg|m4v|mkv|mp2|mp3|mp4|mov|movie|mpe|mpeg|mpg|msi|msu|nfo|oga|ogg|ogv|pkg|png|pdf|ppt|pptx|pps|ppz|pot|psd|qt|rmvb|rar|r\\d+|\\d+|rpm|run|rsdf|rtf|sh|srt|snd|sfv|tar|tif|tiff|viv|vivo|wav|wmv|xla|xls|zip|z\\d+|ts|load|xpi|_[_a-z]{2}|djvu)" }, flags = { 0, 0 })
public class DirectHTTP extends PluginForHost {

    private String contentType;

    static public final String ENDINGS = "\\.(cbr|cbz|otrkey|ac3|3gp|7zip|7z|aiff|aif|aifc|au|avi|bin|bz2|ccf|cue|deb|divx|dlc|dmg|doc|docx|dot|exe|ff|flv|gif|gz|iwd|iso|java|jpg|jpeg|m4v|mkv|mp2|mp3|mp4|mov|movie|mpe|mpeg|mpg|msi|msu|nfo|oga|ogg|ogv|pkg|png|pdf|ppt|pptx|pps|ppz|pot|psd|qt|rmvb|rar|r\\d+|\\d+|rpm|run|rsdf|rtf|sh|srt|snd|sfv|tar|tif|tiff|viv|vivo|wav|wmv|xla|xls|zip|z\\d+|ts|load|xpi|_[_a-z]{2}|djvu)";

    public DirectHTTP(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

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

    private void BasicAuthfromURL(DownloadLink link) {
        String url = null;
        String basicauth = new Regex(link.getDownloadURL(), "http.*?/([^/]{1}.*?)@").getMatch(0);
        if (basicauth != null && basicauth.contains(":")) {
            /* https */
            url = new Regex(link.getDownloadURL(), "https.*?@(.+)").getMatch(0);
            if (url != null) {
                link.setUrlDownload("https://" + url);
            } else {
                /* http */
                url = new Regex(link.getDownloadURL(), "http.*?@(.+)").getMatch(0);
                if (url != null) {
                    link.setUrlDownload("http://" + url);
                } else {
                    logger.severe("Could not parse basicAuth from " + link.getDownloadURL());
                }
            }
            HTACCESSController.getInstance().add(link.getDownloadURL(), Encoding.Base64Encode(basicauth));
        }
    }

    private void setCustomHeaders(Browser br, DownloadLink downloadLink) {
        /* allow customized headers, eg useragent */
        ArrayList<String[]> custom = downloadLink.getGenericProperty("customHeader", new ArrayList<String[]>());
        if (custom != null && custom.size() > 0) {
            for (String[] header : custom) {
                br.getHeaders().put(header[0], header[1]);
            }
        }
        if (downloadLink.getStringProperty("referer", null) != null) br.getHeaders().put("Referer", downloadLink.getStringProperty("referer", null));
        if (downloadLink.getStringProperty("cookies", null) != null) br.getCookies(downloadLink.getDownloadURL()).add(Cookies.parseCookies(downloadLink.getStringProperty("cookies", null), Browser.getHost(downloadLink.getDownloadURL()), null));
    }

    private URLConnectionAdapter prepareConnection(Browser br, DownloadLink downloadLink) throws IOException {
        URLConnectionAdapter urlConnection = null;
        setCustomHeaders(br, downloadLink);
        if (downloadLink.getStringProperty("post", null) != null) {
            urlConnection = br.openPostConnection(downloadLink.getDownloadURL(), downloadLink.getStringProperty("post", null));
        } else {
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
        }
        return urlConnection;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        /* disable gzip, because current downloadsystem cannot handle it correct */
        br.getHeaders().put("Accept-Encoding", "");
        String basicauth = HTACCESSController.getInstance().get(downloadLink.getDownloadURL());
        if (basicauth == null) {
            basicauth = downloadLink.getStringProperty("pass", null);
            if (basicauth != null) basicauth = "Basic " + Encoding.Base64Encode(basicauth);
        }
        if (basicauth != null) {
            br.getHeaders().put("Authorization", basicauth);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = prepareConnection(br, downloadLink);
            if (urlConnection.getResponseCode() == 401 || urlConnection.getResponseCode() == 403) {
                if (basicauth != null) {
                    HTACCESSController.getInstance().remove(downloadLink.getDownloadURL());
                }
                urlConnection.disconnect();
                basicauth = getBasicAuth(downloadLink);
                if (basicauth == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
                br.getHeaders().put("Authorization", basicauth);
                urlConnection = prepareConnection(br, downloadLink);
                if (urlConnection.getResponseCode() == 401) {
                    urlConnection.disconnect();
                    HTACCESSController.getInstance().remove(downloadLink.getDownloadURL());
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
                } else {
                    HTACCESSController.getInstance().add(downloadLink.getDownloadURL(), basicauth);
                }
            }
            if (urlConnection.getResponseCode() == 404 || !urlConnection.isOK()) {
                urlConnection.disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(urlConnection));
            downloadLink.setDownloadSize(urlConnection.getLongContentLength());
            this.contentType = urlConnection.getContentType();
            urlConnection.disconnect();
            return AvailableStatus.TRUE;
        } catch (PluginException e2) {
            throw e2;
        } catch (Exception e) {
            JDLogger.exception(e);
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

    }

    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().startsWith("directhttp")) {
            link.setUrlDownload(link.getDownloadURL().replaceAll("^directhttp://", ""));
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("httpviajd://", "http://").replaceAll("httpsviajd://", "https://"));
        }
        BasicAuthfromURL(link);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.setDebug(true);
        boolean resume = true;
        int chunks = 0;

        if (downloadLink.getBooleanProperty("nochunkload", false) == true) resume = false;
        if (downloadLink.getBooleanProperty("nochunk", false) == true || resume == false) {
            chunks = 1;
        }
        setCustomHeaders(br, downloadLink);
        if (downloadLink.getStringProperty("post", null) != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), downloadLink.getStringProperty("post", null), resume, chunks);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), resume, chunks);
        }
        if (!dl.startDownload()) {
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaderparseerror", "Unexpected rangeheader format:"))) {
                if (downloadLink.getBooleanProperty("nochunk", false) == false) {
                    downloadLink.setProperty("nochunk", Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty("nochunkload", false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty("nochunkload", Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, HTACCESSController.getInstance(), JDL.L("plugins.http.htaccess", "List of all HTAccess passwords. Each line one password.")));
    }

    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("nochunkload", false);
        link.setProperty("nochunk", false);
    }

    public void resetPluginGlobals() {
    }

}
