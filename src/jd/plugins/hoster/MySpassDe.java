//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.text.DecimalFormat;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "myspass.de", "tvtotal.prosieben.de" }, urls = { "https?://(?:www\\.)?myspassdecrypted\\.de/.+\\d+/?$", "http://tvtotal\\.prosieben\\.de/videos/.*?/\\d+/" })
public class MySpassDe extends PluginForHost {
    public MySpassDe(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return "http://www.myspass.de/myspass/kontakt/";
    }

    /*
     * Example final url (18.05.2015):
     * http://x3583brainc11021.s.o.l.lb.core-cdn.net/secdl/78de6150fffffffffff1f136aff77d61/55593149/11021brainpool/ondemand
     * /3583brainpool/163840/myspass2009/14/660/10680/18471/18471_61.mp4
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)/?$").getMatch(0);
        downloadLink.setLinkID(fid);
        // br.getPage("http://www.myspass.de/myspass/includes/apps/video/getvideometadataxml.php?id=" + fid + "&0." +
        // System.currentTimeMillis());
        /* 2018-12-29: New */
        br.getPage("https://www.myspass.de/includes/apps/video/getvideometadataxml.php?id=" + fid);
        if (br.containsHTML("<url_flv><\\!\\[CDATA\\[\\]\\]></url_flv>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Build our filename */
        /* Links added via decrypter can have this set to FALSE as it is not needed for all filenames e.g. stock car crash challenge. */
        final boolean needs_series_filename = downloadLink.getBooleanProperty("needs_series_filename", true);
        final DecimalFormat df = new DecimalFormat("00");
        String filename = getXML("format") + " - ";
        if (needs_series_filename) { // Sometimes episode = 9/Best Of, need regex to get only the integer
            filename += "S" + df.format(Integer.parseInt(getXML("season"))) + "E" + getXML("episode") + " - ";
        }
        filename += getXML("title");
        dllink = getXML("url_flv");
        filename = filename.trim();
        final String ext;
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            ext = ".mp4";
        }
        filename = Encoding.htmlDecode(filename);
        if (dllink != null) {
            downloadLink.setFinalFileName(filename + ext);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            downloadLink.setName(filename + ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2017-02-04: Without the Range Header we'll be limited to ~100 KB/s */
        downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
        br.getHeaders().put("Range", "bytes=" + 0 + "-");
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getXML(final String parameter) {
        return br.getRegex("<" + parameter + "><\\!\\[CDATA\\[(.*?)\\]\\]></" + parameter + ">").getMatch(0);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "FAST_LINKCHECK", "Enable fast linkcheck?\r\nFilesize will only be visible on downloadstart!").setDefaultValue(true));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
