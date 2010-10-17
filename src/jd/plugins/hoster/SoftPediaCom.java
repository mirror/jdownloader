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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "softpedia.com" }, urls = { "http://[\\w\\.]*?softpedia\\.com/get/.+/.*?\\.shtml" }, flags = { 2 })
public class SoftPediaCom extends PluginForHost {

    public SoftPediaCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.softpedia.com/user/terms.shtml";
    }

    private static final String   moddbservers = "moddbservers";
    private static final String   SERVER0      = "SP Mirror (US)";
    private static final String   SERVER1      = "SP Mirror (RO)";
    private static final String   SERVER2      = "Softpedia Mirror (US)";
    private static final String   SERVER3      = "Softpedia Mirror (RO)";

    /** The list of server values displayed to the user */
    private static final String[] servers;

    static {
        servers = new String[] { SERVER0, SERVER1, SERVER2, SERVER3 };
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), moddbservers, servers, JDL.L("plugins.host.SoftPediaCom.servers", "Use this server:")).setDefaultValue(0));
    }

    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(moddbservers, -1)) {
        case 0:
            logger.fine("The server " + SERVER0 + " is configured");
            return 0;
        case 1:
            logger.fine("The server " + SERVER1 + " is configured");
            return 1;
        case 2:
            logger.fine("The server " + SERVER2 + " is configured");
            return 2;
        case 3:
            logger.fine("The server " + SERVER3 + " is configured");
            return 3;
        default:
            logger.fine("No server is configured, returning default server (" + SERVER0 + ")");
            return 0;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404 - page not found</h2>|404error\\.gif\"></td>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("google_ad_section_start --><h1>(.*?)<br/></h1><").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("style=\"padding-top: 15px;\">Softpedia guarantees that <b>(.*?)</b> is <b").getMatch(0);
            if (filename == null) filename = br.getRegex(">yahooBuzzArticleHeadline = \"(.*?)\";").getMatch(0);
        }
        String filesize = br.getRegex("([0-9\\.]+ (MB|KB))").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        int server = getConfiguredServer();
        String nextPage = br.getRegex("<div align=\"center\">[\t\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        if (nextPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String fileID = new Regex(nextPage, "-(\\d+)\\.html$").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(nextPage);
        String mirrorPage = null;
        if (server == 0) {
            mirrorPage = br.getRegex("\"(http://(www\\.)?softpedia\\.com/dyn-postdownload\\.php\\?p=" + fileID + "\\&t=0\\&i=1)\"").getMatch(0);
        } else if (server == 1) {
            mirrorPage = br.getRegex("\"(http://(www\\.)?softpedia\\.com/dyn-postdownload\\.php\\?p=" + fileID + "\\&t=0\\&i=2)\"").getMatch(0);
        } else if (server == 2) {
            mirrorPage = br.getRegex("\"(http://(www\\.)?softpedia\\.com/dyn-postdownload\\.php\\?p=" + fileID + "\\&t=4\\&i=1)\"").getMatch(0);
        } else if (server == 3) {
            mirrorPage = br.getRegex("\"(http://(www\\.)?softpedia\\.com/dyn-postdownload\\.php\\?p=" + fileID + "\\&t=3\\&i=1)\"").getMatch(0);
        }
        if (mirrorPage == null) {
            logger.warning("Failed to find the downloadlink for the chosen mirror, trying to find ANY mirror...");
            mirrorPage = br.getRegex("\"(http://(www\\.)?softpedia\\.com/dyn-postdownload\\.php\\?p=" + fileID + "\\&t=\\d\\&i=\\d)\"").getMatch(0);
        }
        if (mirrorPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(mirrorPage);
        // They have many mirrors, we just pick a random one here because all
        // downloadlinks look pretty much the same
        String dllink = br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+; url=(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("automatically in a few seconds\\.\\.\\. If it doesn\\'t, please <a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://download.*?\\.softpedia\\.com/dl/[a-z0-9]+/[a-z0-9]+/\\d+/.*?)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}