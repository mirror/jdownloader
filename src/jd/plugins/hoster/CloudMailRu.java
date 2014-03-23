//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloud.mail.ru" }, urls = { "http://clouddecrypted\\.mail\\.ru/\\d+|https?://[a-z0-9]+\\.datacloudmail\\.ru/weblink/(view|get)/a13a79fc6e6f/[^<>\"/]+/[^<>\"/]+" }, flags = { 2 })
public class CloudMailRu extends PluginForHost {

    public CloudMailRu(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String TYPE_FROM_DECRYPTER = "http://clouddecrypted\\.mail\\.ru/\\d+";
    private static final String TYPE_HOTLINK        = "https?://[a-z0-9]+\\.datacloudmail\\.ru/weblink/(view|get)/[a-z0-9]+/[^<>\"/]+/[^<>\"/]+";
    private static final String NOCHUNKS            = "NOCHUNKS";
    private static final String DOWNLOAD_ZIP        = "DOWNLOAD_ZIP";

    @Override
    public String getAGBLink() {
        return "https://my.pcloud.com/#page=policies&tab=terms-of-service";
    }

    private static final String BUILD = jd.plugins.decrypter.CloudMailRuDecrypter.BUILD;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        prepBR();
        if (link.getDownloadURL().matches(TYPE_HOTLINK)) {
            URLConnectionAdapter con = null;
            final String dlink = getdllink(link);
            try {
                con = br.openGetConnection(dlink);
                if (!con.getContentType().contains("html")) {
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)).trim());
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* Check if main-folder still exists */
            br.getPage("https://cloud.mail.ru/api/v1/folder/recursive?storage=public&id=" + getID(link) + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&api=1&htmlencoded=false&build=" + BUILD);
            if (br.containsHTML("\"status\":400")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            final String filename = link.getStringProperty("plain_name", null);
            final String filesize = link.getStringProperty("plain_size", null);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(filename);
            link.setDownloadSize(Long.parseLong(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = getdllink(downloadLink);
        boolean resume = true;
        int maxchunks = 0;
        if (isCompleteFolder(downloadLink)) {
            resume = false;
            maxchunks = 1;
        }
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) maxchunks = 1;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("plain_directlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(CloudMailRu.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(CloudMailRu.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info("ERROR_DOWNLOAD_INCOMPLETE --> Handling it");
                if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
                    downloadLink.setProperty(NOCHUNKS, Boolean.valueOf(false));
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
                }
                downloadLink.setProperty(NOCHUNKS, Boolean.valueOf(true));
                downloadLink.setChunksProgress(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "ERROR_DOWNLOAD_INCOMPLETE");
            }
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(CloudMailRu.NOCHUNKS, false) == false) {
                downloadLink.setProperty(CloudMailRu.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private String getdllink(final DownloadLink dl) throws PluginException, IOException {
        String dllink = checkDirectLink(dl, "plain_directlink");
        if (dllink == null) {
            if (dl.getDownloadURL().matches(TYPE_HOTLINK)) {
                dllink = dl.getDownloadURL();
            } else if (isCompleteFolder(dl)) {
                final String request_id = dl.getStringProperty("plain_request_id", null);
                if (request_id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.postPage("https://cloud.mail.ru/api/v1/zip", "name=%D0%9D%D0%BE%D0%B2%D0%B0%D1%8F+%D0%BF%D0%B0%D0%BF%D0%BA%D0%B0&ids=%5B%22" + request_id + "%22%5D&storage=public&cp866=false&api=1&htmlencoded=false&build=" + BUILD);
                dllink = getJson("body", br.toString());
            } else {
                /* FAIL case */
                // dllink = checkDirectLink(dl, "plain_directlink");
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return dllink;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        return result;
    }

    private String getID(final DownloadLink dl) {
        return dl.getStringProperty("plain_request_id", null);
    }

    private boolean isCompleteFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("complete_folder", false);
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CloudMailRu.DOWNLOAD_ZIP, JDL.L("plugins.hoster.CloudMailRu.DownloadZip", "Download .zip file of all files in the folder?")).setDefaultValue(true));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}