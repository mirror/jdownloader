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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onedrive.live.com" }, urls = { "http://onedrivedecrypted\\.live\\.com/\\d+" })
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
    private static final String DOWNLOAD_ZIP            = "DOWNLOAD_ZIP_2";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        JDUtilities.getPluginForDecrypt("onedrive.live.com");
        jd.plugins.decrypter.OneDriveLiveCom.prepBrAPI(br);
        final String cid = link.getStringProperty("plain_cid", null);
        final String id = link.getStringProperty("plain_id", null);
        final String authkey = link.getStringProperty("plain_authkey", null);
        final String original_link = link.getStringProperty("original_link", null);
        String additional_data = "&ps=" + MAX_ENTRIES_PER_REQUEST;
        if (authkey != null) {
            additional_data += "&authkey=" + Encoding.urlEncode(authkey);
        }
        if (isCompleteFolder(link)) {
            /* Case is not yet present */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            jd.plugins.decrypter.OneDriveLiveCom.accessItems_API(br, original_link, cid, id, additional_data);
            if (br.getRequest().getHttpConnection().getResponseCode() == 500) {
                link.getLinkStatus().setStatusText("Server error 500");
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("\"code\":154")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String filename = link.getStringProperty("plain_name", null);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getBooleanProperty("account_only", false)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 30 * 60 * 1000l);
        }
        final String dllink = getDownloadURL(br, downloadLink);
        boolean resume = true;
        int maxchunks = 0;
        if (isCompleteFolder(downloadLink)) {
            // resume = false;
            // maxchunks = 1;
            /* Only registered users can download all files of folders as .zip file */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* This header is especially important for smaller files! See DirectHTTP Host Plugin. */
        br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            try {
                br.followConnection();
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDownloadURL(final Browser br, final DownloadLink dl) throws Exception {
        if (isCompleteFolder(dl)) {
            return null;
        } else {
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final Object error = entries.get("error");
            if (error != null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error.toString());
            }
            final List<Object> ressourcelist = (ArrayList) entries.get("items");
            final String itemId = dl.getStringProperty("plain_item_id", null);
            if (itemId != null) {
                for (Object ressource : ressourcelist) {
                    final String ret = findDownloadURL((Map<String, Object>) ressource, itemId);
                    if (ret != null) {
                        dl.setProperty("plain_download_url", ret);
                        return ret;
                    }
                }
            }
            return dl.getStringProperty("plain_download_url", null);
        }
    }

    private String findDownloadURL(Map<String, Object> item, final String id) throws Exception {
        if (StringUtils.equals(id, (String) item.get("id"))) {
            final Map<String, Object> urls = (Map<String, Object>) item.get("urls");
            final String downloadURL = urls != null ? (String) urls.get("download") : null;
            if (downloadURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                return downloadURL;
            }
        }
        final Map<String, Object> folder = (Map<String, Object>) item.get("folder");
        if (folder != null) {
            final List<Map<String, Object>> children = (List<Map<String, Object>>) folder.get("children");
            for (Map<String, Object> child : children) {
                final String ret = findDownloadURL(child, id);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    private boolean isCompleteFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("complete_folder", false);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), OneDriveLiveCom.DOWNLOAD_ZIP, JDL.L("plugins.hoster.OneDriveLiveCom.DownloadZip", "Download .zip file of all files in the folder (not yet possible)?")).setDefaultValue(false).setEnabled(false));
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