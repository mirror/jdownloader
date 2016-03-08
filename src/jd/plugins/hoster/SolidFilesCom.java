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

import org.appwork.utils.formatter.SizeFormatter;

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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

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

    public static final String   DECRYPTFOLDERS    = "DECRYPTFOLDERS";
    private static final String  NOCHUNKS          = "NOCHUNKS";

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = -1;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Offline links should also get nice filenames */
        link.setName(new Regex(link.getDownloadURL(), "solidfiles\\.com/d/([a-z0-9]+)").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/error/") || br.containsHTML("(>404<|>Not found<|>We couldn\\'t find the file you requested|Access to this file was disabled|The file you are trying to download has|>File not available)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJson(br, "name");
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) (?:-|\\|) Solidfiles</title>").getMatch(0);
        }
        String filesize = PluginJSonUtils.getJson(br, "size");
        if (filesize == null) {
            filesize = br.getRegex("class=\"filesize\">\\(([^<>\"]*?)\\)</span>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("dt>File size<.*?dd>(.*?)</").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        if (br.containsHTML("We're currently processing this file and it's unfortunately not available yet")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is not available yet", 5 * 60 * 1000l);
        }
        String dllink = br.getRegex("class=\"direct\\-download regular\\-download\"[^\r\n]+href=\"(https?://[^\"']+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("href\\s*=(\"|'|)(https?://s\\d+\\.solidfilesusercontent\\.com/[^<>\"]+)\\1").getMatch(1);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        int maxChunks = FREE_MAXCHUNKS;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dllink = dllink.trim();
        final long downloadCurrentRaw = downloadLink.getDownloadCurrentRaw();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 - use less connections and try again", 10 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (this.dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                    // not stable compatible
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw e;
            }
            if (downloadLink.getBooleanProperty(NOCHUNKS, false) == false) {
                if (downloadLink.getDownloadCurrent() > downloadCurrentRaw + (1024 * 1024l)) {
                    throw e;
                } else {
                    /* disable multiple chunks => use only 1 chunk and retry */
                    downloadLink.setProperty(NOCHUNKS, Boolean.TRUE);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
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
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}