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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filetrip.net" }, urls = { "https?://(www\\.)?filetrip.net/dl\\?[A-Za-z0-9]+" }) 
public class FileTripNet extends PluginForHost {

    public FileTripNet(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://filetrip.net/document.php?id=1";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        // Offline links should also have nice filenames
        link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">You might want to check that URL<|>Sorry, this file has been temporarily disabled") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fileInfo = br.getRegex(">([^<>\"]*?)</a><span style=\\'color: #888; font\\-size: 22px; padding\\-left: 20px;\\'>([^<>\"]*?)</span>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Form[] allforms = br.getForms();
        final Form dlform = allforms[allforms.length - 1];
        if (dlform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxchunks = -2;
        if (this.getPluginConfig().getBooleanProperty(ENABLE_UNLIMITED_CHUNKS, defaultENABLE_UNLIMITED_CHUNKS)) {
            maxchunks = 0;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform, true, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">Error 404")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public String getDescription() {
        return "JDownloader's FileTrip plugin helps downloading files from filetrip.net.";
    }

    public static final String  ENABLE_UNLIMITED_CHUNKS        = "ENABLE_UNLIMITED_CHUNKS";
    public static final boolean defaultENABLE_UNLIMITED_CHUNKS = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_UNLIMITED_CHUNKS, JDL.L("plugins.hoster.FileTripNet.enableUnlimitedChunks", "Enable unlimited chunks?\r\nWARNING: This may cause download failures / loops!")).setDefaultValue(defaultENABLE_UNLIMITED_CHUNKS));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}