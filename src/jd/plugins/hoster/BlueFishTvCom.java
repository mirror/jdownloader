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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bluefishtv.com" }, urls = { "bluefishtv.comrtmpe?://(s\\d\\.)?csl\\.delvenetworks.com/.+" }, flags = { 2 })
public class BlueFishTvCom extends PluginForHost {

    private static final String ALLOW_HD     = "ALLOW_HD";
    private static final String ALLOW_HIGH   = "ALLOW_HIGH";
    private static final String ALLOW_LOW    = "ALLOW_LOW";
    private static final String ALLOW_LOWEST = "ALLOW_LOWEST";
    private static final String ALLOW_MEDIUM = "ALLOW_MEDIUM";
    private String              DLLINK       = null;

    public BlueFishTvCom(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("bluefishtv.comrtmp", "rtmp"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("rtmp://csl.delvenetworks.com", "rtmp://s1.csl.delvenetworks.com"));
    }

    private String convertRtmpUrlToHttpUrl(String url) throws Exception {
        if (!url.matches("rtmp.?://(.*?)/[0-9a-f]+/[0-9a-f]+-[0-9a-f]+/[\\w\\-]+\\.\\w+")) { return null; }
        final String ext = url.substring(url.lastIndexOf(".") + 1);
        url = url.substring(url.indexOf("//") + 2);

        if (ext.matches("(mp3|mp4)")) {
            if (url.indexOf("/" + ext + ":") != -1) {
                url = url.replace(url.substring(url.indexOf("/") + 1, url.indexOf(":") + 1), "");
            }
        }

        url = "http://" + url.replaceAll("\\.csl\\.", ".cpl.");
        url = convertToMediaVaultUrl(url);
        if (url == null) { return null; }
        return url;
    }

    private String convertToMediaVaultUrl(String url) {
        final Browser getTime = br.cloneBrowser();
        String time = null;
        try {
            getTime.getPage("http://assets.delvenetworks.com/time.php");
            time = getTime.getRegex("(\\d+)").getMatch(0);
        } catch (final Throwable e) {
        }
        if (time == null) { return null; }
        final int e = (int) Math.floor(Double.parseDouble(time) + 1500);
        url = url + "?e=" + e;
        final String h = JDHash.getMD5(Encoding.Base64Decode("Z0RuU1lzQ0pTUkpOaVdIUGh6dkhGU0RqTFBoMTRtUWc=") + url);
        url = url + "&h=" + h;
        return url;
    }

    @Override
    public String getAGBLink() {
        return "http://www.bluefishtv.com/Terms_of_Use";
    }

    @Override
    public String getDescription() {
        return "JDownloader's BlueFishVOD Plugin helps downloading videoclips from bluefishtv.com. Bluefishtv.com provides different video qualities.";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        DLLINK = convertRtmpUrlToHttpUrl(downloadLink.getDownloadURL());
        if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "Download not possible"); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepareBrowser(final Browser pp) {
        pp.getHeaders().put("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        pp.getHeaders().put("Accept", "*/*");
        pp.getHeaders().put("Accept-Charset", null);
        pp.getHeaders().put("Accept-Language", "de-DE");
        pp.getHeaders().put("Cache-Control", null);
        pp.getHeaders().put("Pragma", null);
        pp.getHeaders().put("Referer", "http://s.delvenetworks.com/deployments/player/player-3.37.5.3.swf?ldr=ldr");
        pp.getHeaders().put("x-flash-version", "10,3,183,7");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (getPluginConfig().getBooleanProperty("COMPLETE_SEASON")) { return AvailableStatus.TRUE; }
        DLLINK = convertRtmpUrlToHttpUrl(downloadLink.getDownloadURL());
        if (DLLINK == null) {
            downloadLink.getLinkStatus().setStatusText("Download not possible");
            return AvailableStatus.TRUE;
        }
        prepareBrowser(br);
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDL.L("plugins.hoster.bluefishtvcom.configlabel", "Select Media Quality:")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HD, "HD @ 1200Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HIGH, "HIGH @ 900Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MEDIUM, "MEDIUM @ 600Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_LOW, "LOW @ 450Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_LOWEST, "LOWEST @ 224Kbps").setDefaultValue(true));
    }

}