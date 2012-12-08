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

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tele5.de" }, urls = { "decrypted://(www\\.)?tele5.de/\\w+" }, flags = { 0 })
public class TeleFiveDe extends PluginForHost {

    private String  DLLINK       = null;

    private boolean NOTFORSTABLE = false;

    public TeleFiveDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("decrypted://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.tele5.de/nutzungsbedingungen";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private byte[] createAMFRequest(String query) {
        if (query == null) return null;
        String data = "0A000000010200";
        data += getHexLength(query) + JDHexUtils.getHexString(query);
        return JDHexUtils.getByteArray("000300000001001674656C65352E676574436F6E74656E74506C6179657200022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();

        } else {
            br.setFollowRedirects(true);
            if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (DLLINK.startsWith("mms")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (mms://) not supported!"); }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        String[] streamValue = DLLINK.split("@");
        rtmp.setUrl(streamValue[0]);
        rtmp.setPlayPath(streamValue[1]);
        rtmp.setSwfVfy("http://www.tele5.de/flashmedia/Tele5_Mediaplayer.swf");
        rtmp.setResume(true);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We\\'re sorry, the page you requested cannot be found\\.")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String pageUrl = downloadLink.getDownloadURL();
        final String query = new Regex(pageUrl, "/(\\w+)$").getMatch(0);

        Browser amf = new Browser();
        getAMFRequest(amf, createAMFRequest(query));
        if (NOTFORSTABLE) throw new PluginException(LinkStatus.ERROR_FATAL, "JDownloader2 is needed!");

        HashMap<String, String> values = new HashMap<String, String>();
        values = AMFParser(amf);
        if (values == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        String ext = null;
        String fileName = values.get("subline");
        if (fileName == null) {
            fileName = br.getRegex("<meta name=\"title\" content=\"(.*?), Recorded on ").getMatch(0);
            if (fileName == null) {
                fileName = br.getRegex("<title>(.*?),.*?</title>").getMatch(0);
                if (fileName == null) {
                    fileName = br.getRegex("<meta property=\"og:title\" content=\"(.*?), Recorded on ").getMatch(0);
                }
            }
        }
        fileName = fileName.replaceAll("\\|", "_").replaceAll("\\s_\\s", "_");
        try {
            fileName = new String(fileName.getBytes("ISO-8859-1"), "UTF-8");
        } catch (Throwable e) {
        }

        String media = values.get("filename");
        if (media != null) {
            DLLINK = values.get("path");
            if (DLLINK != null) DLLINK += "@" + media;
            ext = media.substring(media.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".mp4";
        }
        if (fileName == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        downloadLink.setName(Encoding.htmlDecode(fileName.trim()) + ext);
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

    private HashMap<String, String> AMFParser(final Browser amf) {
        /* Parsing key/value pairs from binary amf0 response message to HashMap */
        String t = amf.toString();
        /* workaround for browser in stable version */
        t = t.replaceAll("\r\n", "\n");
        char[] content = null;
        try {
            content = t.toCharArray();
        } catch (Throwable e) {
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < content.length; i++) {
            if (content[i] != 3) continue;// Object 0x03
            i = i + 2;
            for (int j = i; j < content.length; j++) {
                int keyLen = content[j];
                if (keyLen == 0 || keyLen + j >= content.length) {
                    i = content.length;
                    break;
                }
                String key = "";
                int k;
                for (k = 1; k <= keyLen; k++) {
                    key = key + content[k + j];
                }
                String value = "";
                int v = j + k;
                int vv = 0;
                if (content[v] == 2) {// String 0x02
                    v = v + 2;
                    int valueLen = content[v];
                    if (valueLen == 0) value = null;
                    for (vv = 1; vv <= valueLen; vv++) {
                        value = value + content[v + vv];
                    }
                } else if (content[v] == 0) {// Number 0x00
                    String r;
                    for (vv = 1; vv <= 8; vv++) {
                        r = Integer.toHexString(content[v + vv]);
                        r = r.length() % 2 > 0 ? "0" + r : r;
                        value = value + r;
                    }
                    /*
                     * Encoded as 64-bit double precision floating point number IEEE 754 standard
                     */
                    value = value != null ? String.valueOf((int) Double.longBitsToDouble(new BigInteger(value, 16).longValue())) : value;
                } else {
                    continue;
                }
                j = v + vv;
                result.put(key, value);
            }
        }
        return result;
    }

    private void getAMFRequest(final Browser amf, final byte[] b) {
        amf.getHeaders().put("Content-Type", "application/x-amf");
        try {
            PostRequest request = (PostRequest) amf.createPostRequest("http://www.tele5.de/gateway/gateway.php", (String) null);
            request.setPostBytes(b);
            amf.openRequestConnection(request);
            amf.loadConnection(null);
        } catch (Throwable e) {
            /* does not exist in 09581 */
            NOTFORSTABLE = true;
        }
    }

}