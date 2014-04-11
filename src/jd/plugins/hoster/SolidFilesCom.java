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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "solidfiles.com" }, urls = { "http://(www\\.)?solidfiles\\.com/d/[a-z0-9]+/?" }, flags = { 2 })
public class SolidFilesCom extends PluginForHost {

    public SolidFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.setStartIntervall(500l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.solidfiles.com/terms/";
    }

    public static final String  DECRYPTFOLDERS = "DECRYPTFOLDERS";
    private static final String NOCHUNKS       = "NOCHUNKS";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Offline links should also get nice filenames */
        link.setName(new Regex(link.getDownloadURL(), "solidfiles\\.com/d/([a-z0-9]+)").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/error/") || br.containsHTML("(>Not found<|>We couldn\\'t find the file you requested|Access to this file was disabled|The file you are trying to download has|>File not available)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>([^<>\"]*?) \\- Solidfiles</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<div id=\"download\\-title\">[\t\n\r ]+<h2>([^<>\"]*?)</h2>").getMatch(0);
        String filesize = br.getRegex("class=\"filesize\">\\(([^<>\"]*?)\\)</span>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("dt>File size<.*?dd>(.*?)</").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("<a id=\"download-button\"[^\r\n]+href=\"(https?://[^\"']+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(https?://\\w+\\.sfcdn\\.in/[^\"']+)").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) maxChunks = 1;

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 - use less connections and try again", 10 * 60 * 1000l);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(SolidFilesCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(SolidFilesCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(SolidFilesCom.NOCHUNKS, false) == false) {
                downloadLink.setProperty(SolidFilesCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ChoMikujPl.DECRYPTFOLDERS, JDL.L("plugins.hoster.solidfilescom.decryptfolders", "Decrypt subfolders in folders")).setDefaultValue(true));
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