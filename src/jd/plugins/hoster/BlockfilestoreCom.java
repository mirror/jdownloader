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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.websocket.WebSocketClient;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.net.websocket.ReadWebSocketFrame;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BlockfilestoreCom extends PluginForHost {
    public BlockfilestoreCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://www.blockfilestore.com/terms";
    }

    private static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.BlockfilestoreComFolder.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/api/download\\?id=([a-f0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Content-Type", "text/plain;charset=UTF-8");
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.postPage("https://www." + this.getHost() + "/bfshub/negotiate", "");
        final Map<String, Object> entries = JSonStorage.restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final String connectionId = entries.get("connectionId").toString();
        // br.getHeaders().put("Origin", "https://www.blockfilestore.com");
        // br.getHeaders().put("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits");
        // br.getHeaders().put("Sec-WebSocket-Key", "");
        // br.getHeaders().put("Sec-WebSocket-Version", "13");
        final WebSocketClient wsc = new WebSocketClient(br, new URL("https://www." + this.getHost() + "/bfshub?id=" + connectionId));
        String uidh = null;
        try {
            wsc.connect();
            ReadWebSocketFrame frame = wsc.readNextFrame();// sid
            frame = wsc.readNextFrame();// session
            frame = wsc.readNextFrame();// subscription
            frame = wsc.readNextFrame();// subscription
            final String response = new String(frame.getPayload(), "UTF-8");
            uidh = new Regex(response, "arguments\":\\[\"([a-f0-9\\-]+)\"\\]").getMatch(0);
            // if (WebSocketFrameHeader.OP_CODE.UTF8_TEXT.equals(frame.getOpCode()) && frame.isFin())
        } finally {
            wsc.close();
        }
        br.getHeaders().put("uidh", uidh);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}