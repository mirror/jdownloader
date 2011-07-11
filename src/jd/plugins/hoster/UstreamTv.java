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
import java.util.Date;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 12299 $", interfaceVersion = 2, names = { "ustream.tv" }, urls = { "http://(www\\.)?ustream.tv/recorded/\\d+(/highlight/\\d+)?" }, flags = { 0 })
public class UstreamTv extends PluginForHost {

    public UstreamTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ustream.tv/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Setup Gateway
        final String url = "http://216.52.240.138/gateway.php";
        // Generate Parameter
        final Date rpin = new Date();
        final String pageUrl = downloadLink.getDownloadURL();
        final String videoId = new Regex(pageUrl, "recorded/(\\d+)").getMatch(0);
        final HashMap<String, String> parameter = new HashMap<String, String>();
        parameter.put("brandId", "1");
        parameter.put("videoId", videoId);
        parameter.put("rpin", "rpin.0." + rpin.getTime());
        parameter.put("autoplay", "");
        parameter.put("pageUrl", pageUrl);
        // ActionMessageFormat
        try {
            flex.messaging.io.amf.ASObject result;
            final flex.messaging.io.amf.client.AMFConnection amfConnection = new flex.messaging.io.amf.client.AMFConnection();
            try {
                amfConnection.connect(url);
                amfConnection.addHttpRequestHeader("Content-type", "application/x-amf");
                amfConnection.addHttpRequestHeader("Referer", "http://cdn1.ustream.tv/swf/4/viewer.rsl.465.swf?");
                amfConnection.addHttpRequestHeader("x-flash-version", "10,1,85,3");
                result = (flex.messaging.io.amf.ASObject) amfConnection.call("Viewer.getVideo", parameter);
            } catch (final Exception e) {
                JDLogger.exception(e);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            amfConnection.close();
            int chunk = 0;
            String dllink = "";
            // mp4,flv Selection
            if (result.containsKey("liveHttpUrl")) {
                dllink = result.get("liveHttpUrl").toString();
                chunk = 1;
            } else if (result.containsKey("flv")) {
                if (pageUrl.contains("highlight")) {
                    downloadLink.setName(downloadLink.getName().replace("mp4", "flv"));
                }
                dllink = result.get("flv").toString();
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temporary offline!");
            }
            dl = BrowserAdapter.openDownload(br, downloadLink, dllink, true, chunk);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Nightly Version of JD needed!");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We\\'re sorry, the page you requested cannot be found\\.")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("VideoTitle\">(.*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?),.*?</title>").getMatch(0);
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (downloadLink.getDownloadURL().contains("highlight")) {
            downloadLink.setName(filename.trim() + ".mp4");
        } else {
            downloadLink.setName(filename.trim() + ".flv");
        }
        return AvailableStatus.TRUE;
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
}
