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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ustream.tv" }, urls = { "http://(www\\.)?ustream\\.tv/recorded/\\d+(/highlight/\\d+)?" }, flags = { 0 })
public class UstreamTv extends PluginForHost {

    private String  DLLINK       = null;

    private boolean NOTFORSTABLE = false;

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

    private String beautifierString(final Browser amf) {
        final StringBuffer sb = new StringBuffer();
        for (final byte element : amf.toString().getBytes()) {
            if (element < 127) {
                if (element > 31) {
                    sb.append((char) element);
                } else {
                    sb.append("#");
                }
            }
        }
        if (sb == null || sb.length() == 0) { return null; }
        return sb.toString().replaceAll("#+", "#");
    }

    private byte[] createAMFRequest(String url, String vid) {
        if (vid == null) return null;
        String rpin = "rpin" + String.valueOf(Math.random() * Math.random()).substring(1);
        String data = "0A000000010300077061676555726C0200";
        data += getHexLength(url) + JDHexUtils.getHexString(url);
        data += "00086175746F706C617901010007766964656F49640200";
        data += getHexLength(vid) + JDHexUtils.getHexString(vid);
        data += "00066C6F63616C65020005656E5F555300047270696E0200";
        data += getHexLength(rpin) + JDHexUtils.getHexString(rpin);
        data += "00076272616E64496402000131000009";
        return JDHexUtils.getByteArray("000000000001000F5669657765722E676574566964656F00022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We\\'re sorry, the page you requested cannot be found\\.")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String pageUrl = downloadLink.getDownloadURL();
        final String videoId = new Regex(pageUrl, "recorded/(\\d+)").getMatch(0);

        Browser amf = new Browser();
        getAMFRequest(amf, createAMFRequest(pageUrl, videoId));
        String result = beautifierString(amf);
        HashMap<String, String> values = new HashMap<String, String>();
        for (String[] s : new Regex(result == null ? "" : result, "#(title|flv|liveHttpUrl|smoothStreamingUrl)#.([^<>#]+)").getMatches()) {
            values.put(s[0], s[1]);
        }

        String ext = ".mp4";
        String filename = values.get("title");
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"(.*?), Recorded on ").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?),.*?</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?), Recorded on ").getMatch(0);
                }
            }
        }

        DLLINK = values.get("liveHttpUrl");
        if (DLLINK == null) {
            DLLINK = values.get("smoothStreamingUrl");
            if (DLLINK == null) {
                DLLINK = values.get("flv");
                ext = ".flv";
            }
        }
        if (NOTFORSTABLE) throw new PluginException(LinkStatus.ERROR_FATAL, "JDownloader2 is needed!");
        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        downloadLink.setName(filename.trim() + ext);

        // In case the link redirects to the finallink
        amf.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = amf.openGetConnection(DLLINK);
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

    private void getAMFRequest(final Browser amf, final byte[] b) {
        amf.getHeaders().put("Content-Type", "application/x-amf");
        try {
            PostRequest request = (PostRequest) amf.createPostRequest("http://rgw.ustream.tv/gateway.php", (String) null);
            request.setPostBytes(b);
            amf.openRequestConnection(request);
            amf.loadConnection(null);
        } catch (Throwable e) {
            /* does not exist in 09581 */
            NOTFORSTABLE = true;
        }
    }

}