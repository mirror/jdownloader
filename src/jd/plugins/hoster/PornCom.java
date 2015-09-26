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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "porn.com" }, urls = { "http://(www\\.)?porn\\.com/videos/[^<>\"/]+\\d+(\\.html)?" }, flags = { 0 })
public class PornCom extends antiDDoSForHost {

    private String DLLINK = null;
    private String vq     = null;

    public PornCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.porn.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL().replace("/embed/", "/"));
        if (br.containsHTML("(id=\"error\"><h2>404|No such video|<title>PORN\\.COM</title>)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        get_dllink(this.br);
        /* A little trick to download videos that are usually only available for registered users WITHOUT account :) */
        if (DLLINK == null) {
            final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)(?:\\.html)?$").getMatch(0);
            final Browser brc = br.cloneBrowser();
            /* This way we can access links which are usually only accessible for registered users */
            brc.getPage("http://www.porn.com/videos/embed/" + fid + ".html");
            get_dllink(brc);
        }
        if (DLLINK == null && br.containsHTML(">Sorry, this video is only available to members")) {
            downloadLink.setName(filename + ".mp4");
            downloadLink.getLinkStatus().setStatusText("Only available for registered users");
            return AvailableStatus.TRUE;
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK).replace("\\", "");
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".mp4";
        }
        if (vq == null) {
            downloadLink.setFinalFileName(filename + ext);
        } else {
            downloadLink.setFinalFileName(filename + "." + vq + ext);
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private void get_dllink(final Browser brc) {
        final SubConfiguration cfg = SubConfiguration.getConfig("porn.com");
        boolean q240 = cfg.getBooleanProperty("240p", false);
        boolean q360 = cfg.getBooleanProperty("360p", false);
        boolean q480 = cfg.getBooleanProperty("480p", false);
        boolean q720 = cfg.getBooleanProperty("720p", false);
        DLLINK = brc.getRegex("240p\",url:\"(http:.*?)\"").getMatch(0); // Default
        if (q240) {
            DLLINK = brc.getRegex("240p\",url:\"(http:.*?)\"").getMatch(0);
            vq = "240p";
        }
        if (q360) {
            DLLINK = brc.getRegex("360p\",url:\"(http:.*?)\"").getMatch(0);
            vq = "360p";
        }
        if (q480) {
            DLLINK = brc.getRegex("480p\",url:\"(http:.*?)\"").getMatch(0);
            vq = "480p";
        }
        if (q720) {
            DLLINK = brc.getRegex("720p\",url:\"(http:.*?)\"").getMatch(0);
            vq = "720p";
        }
        if (DLLINK != null) {
            return;
        }
        // json
        final String a = getJsonArray("streams");
        final String[] array = getJsonResultsFromArray(a);
        if (array != null) {
            int highestQual = 0;
            String bestUrl = null;
            for (final String aa : array) {
                final String quality = getJson(aa, "name");
                final String q = quality != null ? new Regex(quality, "\\d+").getMatch(-1) : null;
                final int qual = q != null ? Integer.parseInt(q) : 0;
                if (qual > highestQual) {
                    highestQual = qual;
                    final String url = getJson(aa, "url");
                    if (url != null) {
                        bestUrl = url;
                    }
                }
            }
            DLLINK = bestUrl;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK == null && br.containsHTML(">Sorry, this video is only available to members")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only available for registered users");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public String getDescription() {
        return "Only highest quality video available that you choose will be chosen.";
    }

    private void setConfigElements() {
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_BEST", JDL.L("plugins.hoster.PornCom.checkbest", "Only grab the best available resolution")).setDefaultValue(false);
        // getConfig().addEntry(hq);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "240p", JDL.L("plugins.hoster.PornCom.check360p", "Choose 240p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "360p", JDL.L("plugins.hoster.PornCom.check360p", "Choose 360p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "480p", JDL.L("plugins.hoster.PornCom.check480p", "Choose 480p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "720p", JDL.L("plugins.hoster.PornCom.check720p", "Choose 720p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
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