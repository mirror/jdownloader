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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import flex.messaging.io.amf.ASObject;
import flex.messaging.io.amf.AmfTrace;
import flex.messaging.io.amf.client.AMFConnection;

@HostPlugin(revision = "$Revision: 12299 $", interfaceVersion = 2, names = { "viddler.com" }, urls = { "http://(www\\.)?viddler.com/explore/\\w+/videos/\\d+" }, flags = { PluginWrapper.DEBUG_ONLY })
public class VddlrCm extends PluginForHost {
    // Note this plugin does still not work because of an AMF bug (?!)
    public VddlrCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.viddler.com/terms-of-use/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        this.br.setCookiesExclusive(true);
        this.br.setDebug(true);
        // Setup Gateway
        final String url = "http://www.viddler.com/amfgateway.action";
        // Generate Parameter
        final String pageUrl = downloadLink.getDownloadURL();
        final HashMap<String, String> parameter = new HashMap<String, String>();
        parameter.put("1ba2eda3", "ad15eebc");
        // ActionMessageFormat
        ASObject result;
        final AMFConnection amfConnection = new AMFConnection();
        final AmfTrace trace = new AmfTrace();
        try {
            amfConnection.connect(url);
            amfConnection.setAmfTrace(trace);
            amfConnection.addHttpRequestHeader("Content-type", "application/x-amf");
            amfConnection.addHttpRequestHeader("Referer", "http://cdn-static.viddler.com/flash/player750.swf");
            result = (ASObject) amfConnection.call("viddlerGateway.getVideoInfo", parameter);
        } catch (final Exception e) {
            amfConnection.getAmfTrace();
            final int[] test = { 0, 3, 0, 0, 0, 1, 0, 27, 118, 105, 100, 100, 108, 101, 114, 71, 97, 116, 101, 119, 97, 121, 46, 103, 101, 116, 86, 105, 100, 101, 111, 73, 110, 102, 111, 0, 2, 47, 48, -1, -1, -1, -1, 17, 9, 3, 1, 9, 1, 17, 49, 98, 97, 50, 101, 100, 97, 51, 6, 17, 102, 102, 102, 102, 102, 102, 102, 102, 1, 0 };
            for (final int bla : test) {
                System.out.print(Integer.toHexString(bla) + " ");
            }
            System.out.println(trace.toString());
            JDLogger.exception(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        amfConnection.close();
        int chunk = 0;
        String dllink = new String();
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
        this.dl = BrowserAdapter.openDownload(this.br, downloadLink, dllink, true, chunk);
        if (this.dl.getConnection().getContentType().contains("html")) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.br.getPage(downloadLink.getDownloadURL());
        if (this.br.containsHTML("Video not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = this.br.getRegex("title=\"(.*?)\"").getMatch(0, 3);
        if (filename == null) {
            filename = this.br.getRegex("<title>(.*?),.*?</title>").getMatch(0);
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
