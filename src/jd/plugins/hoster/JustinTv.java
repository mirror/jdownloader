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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "justin.tv" }, urls = { "http://.+(justin|twitch)decrypted\\.tv/archives/[^<>\"]*?\\.flv" }, flags = { 2 })
public class JustinTv extends PluginForHost {

    public JustinTv(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("(justin|twitch)decrypted\\.tv", "justin.tv"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.justin.tv/user/terms_of_service";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String NOCHUNKS       = "NOCHUNKS";
    private static final String CUSTOMDATE     = "CUSTOMDATE";
    private static final String CUSTOMFILENAME = "CUSTOMFILENAME";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, ParseException {
        this.setBrowserExclusive();
        URLConnectionAdapter con = null;
        final Browser br2 = br.cloneBrowser();
        String videoName = null;
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        try {
            con = br2.openGetConnection(downloadLink.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                videoName = correctFilename(downloadLink.getName());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        final SubConfiguration cfg = this.getPluginConfig();
        String formattedFilename = cfg.getStringProperty(CUSTOMFILENAME);
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = "*channel*_*date*_*filename*";

        final String date = downloadLink.getStringProperty("originaldate", null);
        final String channelName = downloadLink.getStringProperty("channel", null);

        String formattedDate = null;
        if (date != null) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOMDATE);
            final String[] dateStuff = date.split("T");
            // 2013-04-21T13:44:43Z
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss");
            Date dateStr = formatter.parse(dateStuff[0] + ":" + dateStuff[1]);
            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            formatter = new SimpleDateFormat(userDefinedDateFormat);
            formattedDate = formatter.format(theDate);
        }

        formattedFilename = formattedFilename.replace("*videoname*", videoName);
        if (channelName != null) formattedFilename = formattedFilename.replace("*channelname*", channelName);
        if (formattedDate != null) formattedFilename = formattedFilename.replace("*date*", formattedDate);
        downloadLink.setFinalFileName(formattedFilename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(JustinTv.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">416 Requested Range Not Satisfiable<")) {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(JustinTv.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(JustinTv.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            /* unknown error, we disable multiple chunks */
            if (downloadLink.getBooleanProperty(JustinTv.NOCHUNKS, false) == false) {
                downloadLink.setProperty(JustinTv.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private String correctFilename(final String oldFilename) {
        String newFilename = oldFilename.replace("#", "");
        return newFilename;
    }

    @Override
    public String getDescription() {
        return "JDownloader's twitch.tv Plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOMDATE, JDL.L("plugins.hoster.justintv.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy_hh-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channel*_*date*_*filename*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOMFILENAME, JDL.L("plugins.hoster.justintv.customfilename", "Define how the filenames should look:")).setDefaultValue("*channelname*_*date*_*videoname*"));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}