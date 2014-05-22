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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "samepage.io" }, urls = { "http://samepagedecrypted\\.io/\\d+" }, flags = { 2 })
public class SamePageIo extends PluginForHost {

    public SamePageIo(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://samepage.io/cloud/legal/?tos";
    }

    private static final String DOWNLOAD_ZIP = "DOWNLOAD_ZIP";
    private String              id_1         = null;
    private String              id_2         = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        prepBR();
        // if (br.getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = link.getStringProperty("plain_name", null);
        final String filesize = link.getStringProperty("plain_size", null);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        id_1 = downloadLink.getStringProperty("plain_id_1");
        final String forward_link = downloadLink.getStringProperty("mainlink", null);
        if (forward_link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        id_2 = new Regex(forward_link, "samepage\\.io/app/#\\!/[a-z0-9]+/page\\-(\\d+)").getMatch(0);
        if (id_2 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        br.getPage("https://samepage.io/app/");
        final String main = br.getRegex("ClientSamepage\\.main\\(\\'(/client/[a-z0-9]+)\\'").getMatch(0);
        if (main == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.postPageRaw("https://samepage.io/api/app/jsonrpc", "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\":\"Bootstrap.bootstrap\",\"params\":{\"tenantId\":\"" + id_1 + "\",\"itemId\":\"" + id_2 + "\"}}");
        final String apiVersion = getJson("apiVersion", br.toString());
        final String token = br.getCookie("http://samepage.io", "TOKEN_WORKSPACE");
        if (token == null || apiVersion == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        br.cloneBrowser().getPage("https://samepage.io" + main + "/lib/internal/translations/de.js");
        br.getHeaders().put("Referer", "https://samepage.io/app/");
        br.getHeaders().put("X-Token", token);
        br.getHeaders().put("Content-Type", "application/json; charset=UTF-8");
        br.postPageRaw("https://samepage.io/" + id_1 + "/server/data?method=Items.get", "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"Items.get\",\"params\":{\"includeChildren\":-1,\"includeIamFollowing\":true,\"includeHasSubtree\":true,\"includeChain\":true,\"id\":\"" + id_2 + "\"},\"apiVersion\":\"" + apiVersion + "\"}");
        /* Item not found */
        if (br.containsHTML("\"code\":6002")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final String dllink = getdllink(downloadLink);
        boolean resume = true;
        int maxchunks = 0;
        if (isCompleteFolder(downloadLink)) {
            resume = false;
            maxchunks = 1;
        }

        br.setCookie("http://samepage.io/", "TENANT_WORKSPACE", "1");
        br.setCookie("http://samepage.io/", "TOKEN_WORKSPACE", token);
        br.getHeaders().put("Referer", "https://samepage.io/app/");

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getdllink(final DownloadLink dl) throws PluginException {
        final String fid = dl.getStringProperty("plain_fileid", null);
        String dllink;
        if (isCompleteFolder(dl)) {
            dllink = "https://samepage.io/" + id_1 + "/archive/" + fid + ".zip";
        } else {
            String name = Encoding.urlEncode(dl.getName());
            name = name.replace("+", "%20");
            dllink = "https://samepage.io/" + id_1 + "/file/" + fid + "/" + name;
        }
        return dllink;
    }

    private boolean isCompleteFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("complete_folder", false);
    }

    private void prepBR() {
        br.setFollowRedirects(false);
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.getHeaders().put("Accept-Language", "de,en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SamePageIo.DOWNLOAD_ZIP, JDL.L("plugins.hoster.SamePageIo.DownloadZip", "Download .zip file of all files in the folder?")).setDefaultValue(false));
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