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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "about.com" }, urls = { "http://(www\\.)?video\\.about\\.com/\\w+/[\\w\\-]+\\.htm" }, flags = { 32 })
public class AboutCom extends PluginForHost {

    private String  DLLINK       = null;

    private boolean NOTFORSTABLE = false;

    public AboutCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.about.com/gi/pages/uagree.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
        String[] tmpRtmpUrl = DLLINK.split("@");
        rtmp.setUrl(tmpRtmpUrl[0] + tmpRtmpUrl[1]);
        rtmp.setApp(tmpRtmpUrl[1] + tmpRtmpUrl[3] + tmpRtmpUrl[4]);
        rtmp.setPlayPath(tmpRtmpUrl[2] + tmpRtmpUrl[3] + tmpRtmpUrl[4]);
        // rtmp.setConn("B:0");
        // rtmp.setConn("S:" + tmpRtmpUrl[2] + tmpRtmpUrl[3]);
        rtmp.setSwfVfy("http://admin.brightcove.com/viewer/us20121102.1044/federatedVideo/BrightcovePlayer.swf");
        rtmp.setResume(true);
        rtmp.setRealTime();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        String dlink = link.getDownloadURL();
        br.getPage(dlink);
        if (br.containsHTML("404 Document Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("<meta itemprop=\"name\" content=\"([^\"]+)\"").getMatch(0);
        String playerId = br.getRegex("\\.playerID = \"(\\d+)\"").getMatch(0);
        String videoId = br.getRegex("\\.videoId = ([^;]+)").getMatch(0);
        videoId = br.getRegex("var " + videoId + "=\"([^\"]+)").getMatch(0);
        if (filename == null || playerId == null || videoId == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        if (NOTFORSTABLE) throw new PluginException(LinkStatus.ERROR_FATAL, "JDownloader2 is needed!");

        /* AMF-Request */
        Browser amf = br.cloneBrowser();
        amf.getHeaders().put("Content-Type", "application/x-amf");
        getAMFRequest(amf, createAMFMessage(dlink, videoId), playerId);

        /* successfully request? */
        final int rC = amf.getHttpConnection().getResponseCode();
        if (rC != 200) {
            logger.warning("File not found or amf request failure! Link: " + dlink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        byte[] amfRes = amf.getRequest().getResponseBytes();
        String result = beautifierString(amfRes);

        /* decoding amf3 double values */
        Map<String, String> decodedAMF = new HashMap<String, String>();
        String[] keys = { "pubId", "videoId", "fileSize" };
        int t = 0;
        double d = 0;
        for (int i = 0; i < amfRes.length; i++) {
            if (amfRes[i] != 5) continue; // 0x05 double marker + 8byte data
            if (amfRes[i + 9] > 13) continue;
            byte[] amfDouble = new byte[8];
            System.arraycopy(amfRes, i + 1, amfDouble, 0, 8);
            /* Encoded as 64-bit double precision floating point number IEEE 754 standard */
            d = Double.longBitsToDouble(new BigInteger(amfDouble).longValue());
            if (d > 0) decodedAMF.put(keys[t++], String.valueOf((long) d));
            i += 8;
            if (t == keys.length) break;
        }

        DLLINK = new Regex(result, "(rtmp[^#]+)#").getMatch(0); // first match is FLVFullLengthURL
        if (DLLINK == null || t != 3) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        String urlAppend = "&videoId=" + decodedAMF.get("videoId") + "&lineUpId=&pubId=" + decodedAMF.get("pubId") + "&playerId=" + playerId + "&affiliateId=";
        /* make rtmp url */
        String[] tmpRtmpUrl = new Regex(DLLINK, "(rtmp://[\\w\\.]+/)([\\w/]+)/\\&([\\w:\\-\\./]+)(\\&|\\?.*?)$").getRow(0);
        if (tmpRtmpUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = tmpRtmpUrl[0] + "@" + tmpRtmpUrl[1] + "@" + tmpRtmpUrl[2] + "@" + tmpRtmpUrl[3] + "@" + urlAppend;

        link.setFinalFileName(Encoding.htmlDecode(filename.trim().replaceAll("\\s", "_")) + ".mp4");
        try {
            link.setDownloadSize(SizeFormatter.getSize(decodedAMF.get("fileSize")));
        } catch (Throwable e) {
        }

        return AvailableStatus.TRUE;
    }

    private String beautifierString(byte[] b) {
        final StringBuffer sb = new StringBuffer();
        for (final byte element : b) {
            if (element < 127) {
                if (element > 31) {
                    sb.append((char) element);
                } else {
                    sb.append("#");
                }
            }
        }
        if (sb == null || sb.length() == 0) return null;
        return sb.toString().replaceAll("#+", "#");
    }

    private byte[] createAMFMessage(String a, String b) {
        String data = "0A0000000202002835616439303436623430353465336435323262663339366465376366363336383730376561356135110A6363636F6D2E627269676874636F76652E657870657269656E63652E566965776";
        data += "572457870657269656E6365526571756573741964656C69766572795479706513706C617965724B65791154544C546F6B656E19657870657269656E6365496421636F6E74656E744F76657272696465730755";
        data += "524C057FFFFFFFE000000006010601054223EDAA2F2200000903010A810353636F6D2E627269676874636F76652E657870657269656E63652E436F6E74656E744F7665727269646515636F6E74656E7449647";
        data += "319636F6E74656E7452656649641B666561747572656452656649641B636F6E74656E745265664964730D74617267657417636F6E74656E7454797065156665617475726564496413636F6E74656E74496401";
        data += "06" + getHexLength(b, true) + JDHexUtils.getHexString(b); // 0x06(String marker) + length + String b
        data += "01010617766964656F506C617965720400057FFFFFFFE0000000057FFFFFFFE0000000";
        data += "06" + getHexLength(a, true) + JDHexUtils.getHexString(a);
        return JDHexUtils.getByteArray("0003000000010046636F6D2E627269676874636F76652E657870657269656E63652E457870657269656E636552756E74696D654661636164652E67657444617461466F72457870657269656E636500022F310000" + getHexLength(JDHexUtils.toString(data), false) + data);
    }

    private String getHexLength(final String s, boolean amf3) {
        String result = Integer.toHexString(s.length() | 1);
        if (amf3) {
            result = "";
            for (int i : getUInt29(s.length() << 1 | 1)) {
                if (i == 0) break;
                result += Integer.toHexString(i);
            }
        }
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    private int[] getUInt29(int ref) {
        int[] buf = new int[4];
        if (ref < 0x80) {
            buf[0] = ref;
        } else if (ref < 0x4000) {
            buf[0] = (((ref >> 7) & 0x7F) | 0x80);
            buf[1] = ref & 0x7F;
        } else if (ref < 0x200000) {
            buf[0] = (((ref >> 14) & 0x7F) | 0x80);
            buf[1] = (((ref >> 7) & 0x7F) | 0x80);
            buf[2] = ref & 0x7F;
        } else if (ref < 0x40000000) {
            buf[0] = (((ref >> 22) & 0x7F) | 0x80);
            buf[1] = (((ref >> 15) & 0x7F) | 0x80);
            buf[2] = (((ref >> 8) & 0x7F) | 0x80);
            buf[3] = ref & 0xFF;
        } else {
            logger.warning("about.com(amf3): Integer out of range: " + ref);
        }
        return buf;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private void getAMFRequest(final Browser amf, final byte[] b, String s) {
        amf.getHeaders().put("Content-Type", "application/x-amf");
        try {
            PostRequest request = (PostRequest) amf.createPostRequest("http://c.brightcove.com/services/messagebroker/amf?playerId=" + s, (String) null);
            request.setPostBytes(b);
            amf.openRequestConnection(request);
            amf.loadConnection(null);
        } catch (Throwable e) {
            /* does not exist in 09581 */
            NOTFORSTABLE = true;
        }
    }

}