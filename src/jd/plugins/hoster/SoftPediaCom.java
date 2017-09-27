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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser.BrowserException;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "softpedia.com" }, urls = { "http://(www\\.|drivers\\.)?softpedia\\.com/(get/.+/.*?\\.shtml|progDownload/.*?\\-download\\-\\d+\\.(s)?html)" })
public class SoftPediaCom extends PluginForHost {
    private static final String SOFTPEDIASERVERS  = "allservers";
    private static final String SERVER0           = "SP Mirror (US)";
    private static final String SERVER1           = "SP Mirror (RO)";
    private static final String SERVER2           = "Softpedia Mirror (US)";
    private static final String SERVER3           = "Softpedia Mirror (RO)";
    /** The list of server values displayed to the user */
    private final String[]      servers           = new String[] { SERVER0, SERVER1, SERVER2, SERVER3 };
    private static final int    FREE_MAXCHUNKS    = 1;
    private static final int    FREE_MAXDOWNLOADS = -1;

    public SoftPediaCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        String fileID = new Regex(link.getDownloadURL(), "softpedia\\.com/progDownload/(.*?)-Download-\\d+\\.html").getMatch(0);
        if (fileID != null) {
            link.setUrlDownload("http://www.softpedia.com/get/Programming/" + fileID + ".shtml");
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.softpedia.com/user/terms.shtml";
    }

    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(SOFTPEDIASERVERS, -1)) {
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
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Happens when they block your IP */
        if (br.containsHTML("No htmlCode read")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        int server = getConfiguredServer();
        final String fileID = br.getRegex("var spjs_prog_id=(\\d+);").getMatch(0);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String xajaRoot = br.getRegex("var sp_xaja_root=\"([^\"]+)").getMatch(0);
        br.postPage(xajaRoot + "_xaja/dlinfo.php", new UrlQuery().append("t", "15", false).append("id", fileID, false));
        String mirrorPage = br.getRegex("'(" + xajaRoot + "dyn\\-postdownload\\.php/[^<>\"]*?)'").getMatch(0);
        if (mirrorPage == null) {
            /* Unmaintained code */
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
        }
        if (mirrorPage == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(mirrorPage);
        // They have many mirrors, we just pick a random one here because all
        // downloadlinks look pretty much the same
        String dllink = br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+; url=(https?://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("automatically in a few seconds\\.\\.\\. If it doesn\\'t, please <a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://download.*?\\.softpedia\\.com/dl/[a-z0-9]+/[a-z0-9]+/\\d+/.*?)\"").getMatch(0);
            }
        }
        if (dllink == null) {
            /* Some links simply don't work */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        if (new Regex(link.getDownloadURL(), "index\\d*\\.shtml").matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            br.getPage(link.getDownloadURL());
        } catch (final BrowserException e) {
            if (br.getHttpConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw e;
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("No htmlCode read")) {
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("FILENAME:.+?title=\"([^\"]+)").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("google_ad_section_start \\-\\-><h1>(.*?)<br/></h1><").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("style=\"padding\\-top: 15px;\">Softpedia guarantees that <b>(.*?)</b> is <b").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex(">yahooBuzzArticleHeadline = \"(.*?)\";").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<title>([^<>\"]*?)Free Download</title>").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("title=\"Click here to download ([^<>\"]*?)\"").getMatch(0);
                        }
                    }
                }
            }
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) Free Download \\- Softpedia</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?) Download\"").getMatch(0);
        }
        // For fre trail programms
        if (filename == null) {
            filename = br.getRegex("<title>Download ([^<>\"]*?)\\- Softpedia</title>").getMatch(0);
        }
        String filesize = br.getRegex("([0-9\\.]+ (MB|KB))").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SOFTPEDIASERVERS, servers, JDL.L("plugins.host.SoftPediaCom.servers", "Use this server:")).setDefaultValue(0));
    }
}