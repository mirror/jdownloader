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

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.HTACCESSController;
import jd.controlling.ListController;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision", interfaceVersion = 2, names = { "http links", "JDUpdateLoader" }, urls = { "https?viajd://[\\d\\w\\.:\\-@]*/.*\\.(otrkey|ac3|3gp|7zip|7z|aiff|aif|aifc|au|avi|bin|bz2|ccf|cue|divx|dlc|doc|docx|dot|exe|flv|gif|gz|iso|java|jpg|jpeg|mkv|mp2|mp3|mp4|mov|movie|mpe|mpeg|mpg|msi|msu|nfo|png|pdf|ppt|pptx|pps|ppz|pot|qt|rmvb|rar|r\\d+|\\d+|rsdf|rtf|snd|sfv|tar|tif|tiff|viv|vivo|wav|wmv|xla|xls|zip|ts|load)", "https?viajd://[\\d\\w\\.:\\-@]*/.*\\.jdu" }, flags = { 0, HostPluginWrapper.ALWAYS_ENABLED })
public class HTTPAllgemein extends PluginForHost {

    private String contentType;
    static public final String ENDINGS = "\\.(otrkey|ac3|3gp|7zip|7z|aiff|aif|aifc|au|avi|bin|bz2|ccf|cue|divx|dlc|doc|docx|dot|exe|flv|gif|gz|iso|java|jpg|jpeg|mkv|mp2|mp3|mp4|mov|movie|mpe|mpeg|mpg|msi|msu|nfo|png|pdf|ppt|pptx|pps|ppz|pot|qt|rmvb|rar|r\\d+|\\d+|rsdf|rtf|snd|sfv|tar|tif|tiff|viv|vivo|wav|wmv|xla|xls|zip|ts|load)";

    public HTTPAllgemein(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    // @Override
    public String getAGBLink() {
        return "";
    }

    // @Override
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
        String url = link.getDownloadURL();
        String basicauth = new Regex(url, "http.*?/([^/]{1}.*?)@").getMatch(0);
        if (basicauth != null && basicauth.contains(":")) {
            url = new Regex(url, "https.*?@(.+)").getMatch(0);
            if (url != null) link.setUrlDownload("https://" + url);
            if (url == null) url = new Regex(url, "http.*?@(.+)").getMatch(0);
            if (url != null) link.setUrlDownload("http://" + url);
            HTACCESSController.getInstance().add(link.getDownloadURL(), Encoding.Base64Encode(basicauth));
        }
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {

        this.setBrowserExclusive();
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
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
            if (urlConnection.getResponseCode() == 401 || urlConnection.getResponseCode() == 403) {
                if (basicauth != null) {
                    HTACCESSController.getInstance().remove(downloadLink.getDownloadURL());
                }
                urlConnection.disconnect();
                basicauth = getBasicAuth(downloadLink);
                if (basicauth == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
                br.getHeaders().put("Authorization", basicauth);
                urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
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
            downloadLink.setFinalFileName(Plugin.getFileNameFormHeader(urlConnection));
            downloadLink.setBrowserUrl(downloadLink.getDownloadURL());
            downloadLink.setDownloadSize(urlConnection.getLongContentLength());
            this.contentType = urlConnection.getContentType();
            urlConnection.disconnect();
            return AvailableStatus.TRUE;
        } catch (PluginException e2) {
            throw e2;
        } catch (Exception e) {
        } finally {
            if (urlConnection != null && urlConnection.isConnected() == true) urlConnection.disconnect();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

    }

    // @Override
    /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("httpviajd://", "http://").replaceAll("httpsviajd://", "https://"));
        BasicAuthfromURL(link);
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.setDebug(true);
        boolean resume = true;
        int chunks = 0;

        if (downloadLink.getBooleanProperty("nochunkload", false) == true) resume = false;
        if (downloadLink.getBooleanProperty("nochunk", false) == true || resume == false) {
            chunks = 1;
        }
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL(), resume, chunks);

        if (!dl.startDownload()) {
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaderparseerror", "Unexpected rangeheader format:"))) {
                if (downloadLink.getBooleanProperty("nochunk", false) == false) {
                    downloadLink.setProperty("nochunk", new Boolean(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty("nochunkload", false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty("nochunkload", new Boolean(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, (ListController) HTACCESSController.getInstance(), JDL.L("plugins.http.htaccess", "List of all HTAccess passwords. Each line one password.")));
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("nochunkload", false);
        link.setProperty("nochunk", false);
        // link.setProperty("basicauth", null);
    }

    // @Override
    public void resetPluginGlobals() {
    }

}
