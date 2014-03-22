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
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "onedrive.live.com" }, urls = { "http://onedrivedecrypted\\.live\\.com/\\d+" }, flags = { 2 })
public class OneDriveLiveCom extends PluginForHost {

    public OneDriveLiveCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://windows.microsoft.com/de-de/windows-live/microsoft-services-agreement";
    }

    /* Use less than in the decrypter to not to waste traffic & time */
    private static final int    MAX_ENTRIES_PER_REQUEST = 50;
    private static final String DOWNLOAD_ZIP            = "DOWNLOAD_ZIP";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        prepBR();
        final String cid = link.getStringProperty("plain_cid", null);
        final String id = link.getStringProperty("plain_id", null);
        final String authkey = link.getStringProperty("plain_authkey", null);
        String additional_data = "&ps=" + MAX_ENTRIES_PER_REQUEST;
        if (authkey != null) additional_data += "&authkey=" + Encoding.urlEncode(authkey);
        if (isCompleteFolder(link)) {
            /* Case is not yet present */
        } else {
            try {
                accessItems_API(this.br, cid, id, additional_data);
            } catch (final BrowserException e) {
                if (br.getRequest().getHttpConnection().getResponseCode() == 500) {
                    link.getLinkStatus().setStatusText("Server error 500");
                    return AvailableStatus.UNCHECKABLE;
                }
                throw e;
            }
            if (br.containsHTML("\"code\":154")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_name", null);
        final String filesize = link.getStringProperty("plain_size", null);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.getRequest().getHttpConnection().getResponseCode() == 500) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 30 * 60 * 1000l);
        final String dllink = getdllink(downloadLink);
        boolean resume = true;
        int maxchunks = 0;
        if (isCompleteFolder(downloadLink)) {
            // resume = false;
            // maxchunks = 1;
            /* Only registered users can download all files of folders as .zip file */
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");

        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void accessItems_API(final Browser br, final String cid, final String id, final String additional) throws IOException {
        jd.plugins.decrypter.OneDriveLiveCom.accessItems_API(br, cid, id, additional);
    }

    private String getdllink(final DownloadLink dl) throws PluginException {
        String dllink = null;
        if (isCompleteFolder(dl)) {
        } else {
            dllink = dl.getStringProperty("plain_download_url", null);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    private boolean isCompleteFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("complete_folder", false);
    }

    private void prepBR() {
        jd.plugins.decrypter.OneDriveLiveCom.prepBrAPI(this.br);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), OneDriveLiveCom.DOWNLOAD_ZIP, JDL.L("plugins.hoster.OneDriveLiveCom.DownloadZip", "Download .zip file of all files in the folder (not yet possible)?")).setDefaultValue(true).setEnabled(false));
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