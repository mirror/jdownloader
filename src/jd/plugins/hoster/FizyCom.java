//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fizy.com" }, urls = { "rtmphttp://((www|[a-z]+)\\.)?fizy\\.com/(#?s/)?[a-z0-9]{2,}" }, flags = { 32 })
public class FizyCom extends PluginForHost {

    private String clipUrl              = null;

    private String clipNetConnectionUrl = "rtmp://fizy.mncdn.net/fizy/";

    private String INCLUDING_YT         = "INCLUDING_YT";

    public FizyCom(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("rtmphttp", "http"));
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    @Override
    public String getAGBLink() {
        return "http://fizy.com/about";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        final String dllink = downloadLink.getDownloadURL();
        final String sid = dllink.substring(dllink.lastIndexOf("/") + 1);
        if (sid == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        br.postPage("http://fizy.com/fizy::getSong", "SID=" + sid);
        final String filename = br.getRegex("title\":\"(.*?)\"").getMatch(0).trim();
        String ext = br.getRegex("type\":\"(.*?)\"").getMatch(0);
        clipUrl = br.getRegex("source\":\"(.*?)\"").getMatch(0);
        if (filename == null || clipUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        ext = ext == null ? "mp3" : ext;
        downloadLink.setFinalFileName(decodeUnicode(filename) + "." + ext);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dllink = clipNetConnectionUrl + clipUrl;

        if (dllink.startsWith("rtmp")) {
            if (isStableEnviroment()) { throw new PluginException(LinkStatus.ERROR_FATAL, "JD2 BETA needed!"); }

            final String swfUrl = "http://apiplayer1.fizy.org/lib/player/fizyPlay1410a.swf";
            final String app = "fizy/";
            dl = new jd.plugins.hoster.RTMPDownload(this, downloadLink, dllink);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((jd.plugins.hoster.RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(clipUrl);
            rtmp.setApp(app);
            rtmp.setSwfVfy(swfUrl);
            rtmp.setUrl(clipNetConnectionUrl);
            rtmp.setPageUrl("http://fizy.com/");
            rtmp.setTimeOut(1);

            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private boolean isStableEnviroment() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(prev);
        if (rev < 10000) { return true; }
        return false;
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), INCLUDING_YT, JDL.L("plugins.hoster.fizycom.includingyt", "Including youtube links?")).setDefaultValue(false));
    }
}