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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https://(www\\.)?soundclouddecrypted\\.com/[a-z\\-_0-9]+/[a-z\\-_0-9]+" }, flags = { 0 })
public class SoundcloudCom extends PluginForHost {

    private String url;

    public SoundcloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public final static String CLIENTID = "b45b1aa10f1ac2941910a7f0d10f8e28";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("soundclouddecrypted", "soundcloud"));
    }

    @Override
    public String getAGBLink() {
        return "http://soundcloud.com/terms-of-use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        url = parameter.getStringProperty("directlink");
        if (url != null) {
            checkDirectLink(parameter, url);
            if (url != null) return AvailableStatus.TRUE;
        }
        br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(parameter.getDownloadURL()) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + CLIENTID);
        if (br.containsHTML("\"404 \\- Not Found\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        AvailableStatus status = checkStatus(parameter, this.br.toString());
        if (status.equals(AvailableStatus.FALSE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        checkDirectLink(parameter, url);
        return AvailableStatus.TRUE;
    }

    public AvailableStatus checkStatus(final DownloadLink parameter, final String source) {
        String filename = getXML("title", source);
        if (filename == null) {
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.pluginBroken", "The host plugin is broken!"));
            return AvailableStatus.FALSE;
        }
        final String filesize = getXML("original-content-size", source);
        if (filesize != null) parameter.setDownloadSize(Long.parseLong(filesize));
        final String description = getXML("description", source);
        if (description != null) {
            try {
                parameter.setComment(description);
            } catch (Throwable e) {
            }
        }
        String username = getXML("username", source);
        filename = Encoding.htmlDecode(filename.trim());
        String type = getXML("original-format", source);
        if (type == null) type = "mp3";
        username = username.trim();
        if (username != null && !filename.contains(username)) filename += " - " + username;
        filename += "." + type;
        url = getXML("download-url", source);
        if (url != null) {
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.downloadavailable", "Original file is downloadable"));
        } else {
            url = getXML("stream-url", source);
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.previewavailable", "Preview (Stream) is downloadable"));
        }
        if (url == null) {
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.pluginBroken", "The host plugin is broken!"));
            return AvailableStatus.FALSE;
        }
        parameter.setFinalFileName(filename);
        parameter.setProperty("directlink", url + "?client_id=" + CLIENTID);
        return AvailableStatus.TRUE;
    }

    private void checkDirectLink(final DownloadLink downloadLink, final String property) {
        URLConnectionAdapter con = null;
        try {
            Browser br2 = br.cloneBrowser();
            con = br2.openGetConnection(url);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getResponseCode() == 401) {
                downloadLink.setProperty(property, Property.NULL);
                url = null;
                return;
            }
            downloadLink.setDownloadSize(con.getLongContentLength());

        } catch (Exception e) {
            downloadLink.setProperty(property, Property.NULL);
            url = null;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String getJson(final String parameter) {
        return br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
    }

    public String getXML(final String parameter, final String source) {
        return new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>\"]*?)</" + parameter + ">").getMatch(1);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}