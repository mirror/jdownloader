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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u.115.com" }, urls = { "http://[\\w\\.]*?u\\.115\\.com/file/[a-z0-9]+" }, flags = { 2 })
public class U115Com extends PluginForHost {

    private static String u115servers = "u115servers";

    /** The list of servers displayed in the plugin configuration pane */
    // private static final String[] U115_SERVERS = new String[] { "cnc", "tel",
    // "bak" };
    private static final String[] U115_SERVERS;

    static {
        U115_SERVERS = new String[] { "cnc", "tel", "bak" };
    }

    public U115Com(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://u.115.com/tos.html";
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), u115servers, U115_SERVERS, JDL.L("plugins.host.U115Com.servers", "Use this server: ")).setDefaultValue(0));
    }

    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(u115servers, -1)) {
        case 0:
            logger.fine("The server " + U115_SERVERS[0] + " is configured");
            return 0;
        case 1:
            logger.fine("The server " + U115_SERVERS[1] + " is configured");
            return 1;
        case 2:
            logger.fine("The server " + U115_SERVERS[2] + " is configured");
            return 2;
        default:
            logger.fine("No server is configured, returning default server [" + U115_SERVERS[2] + "]");
            return 2;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        br.setCustomCharset("utf-8");
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (!br.containsHTML("class=\"alert-box\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("file_name = '(.*?)';").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)- 115网络U盘").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"nowrap file-name [a-z]+\">(.*?)</h2>").getMatch(0);
            }
        }
        String filesize = br.getRegex("文件大小：(.*?)\\\\r\\\\n").getMatch(0);
        if (filesize == null) filesize = br.getRegex("文件大小：(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace(",", "");
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        String sh1 = br.getRegex("nsha1：([A-Z0-9]+)\\\\r\\\\n").getMatch(0);
        if (sh1 == null) sh1 = br.getRegex("colspan=\"4\">SHA1：(A-Z0-9]+)").getMatch(0);
        if (sh1 != null) link.setSha1Hash(sh1);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        int maxchunks = 0;
        int chosenServer = getConfiguredServer();
        if (chosenServer == 0)
            maxchunks = -2;
        else if (chosenServer == 1) maxchunks = -3;
        requestFileInformation(link);
        String dllink = findLink();
        if (dllink == null) {
            maxchunks = -2;
            dllink = findLink2();
        }
        if (dllink == null) {
            logger.warning("dllink is null, seems like the regexes are defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.urlDecode(dllink, true);
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(help_down.html|onclick=\"WaittingManager|action=submit_feedback|\"report_box\"|UploadErrorMsg)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String secondServerregexPart = "\\.[a-z0-9.]+\\.com(:\\d+)?/.*?\\?key=.*?\\&key1=.*?(\\&file=.*?)?\\&key2=[a-z0-9]+)";

    public String findLink() throws Exception {
        int chosenServer = getConfiguredServer();
        String serverext = U115_SERVERS[chosenServer];
        String finalRegex = "(http://\\d+\\." + serverext + secondServerregexPart;
        String dllink = br.getRegex(finalRegex).getMatch(0);
        if (dllink == null) logger.info("Dllink for chosen server " + U115_SERVERS[chosenServer] + " couldn't be found");
        return dllink;
    }

    public String findLink2() throws Exception {
        String dllink = null;
        for (int i = 0; i <= U115_SERVERS.length - 1; i++) {
            String serverext = U115_SERVERS[i];
            String finalRegex = "(http://\\d+\\." + serverext + secondServerregexPart;
            dllink = br.getRegex(finalRegex).getMatch(0);
            if (dllink != null) break;
        }
        return dllink;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        int maxdls = -1;
        int chosenServer = getConfiguredServer();
        if (chosenServer == 0)
            maxdls = 1;
        else if (chosenServer == 1) maxdls = 2;
        return maxdls;
    }

}
