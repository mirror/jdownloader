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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

import org.appwork.utils.Regex;
import org.appwork.utils.encoding.Base64;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "viddler.com" }, urls = { "http://(www\\.)?viddler\\.com/(explore/\\w+/videos/\\d+|(player|simple)/\\w+(/)?(.+)?)" }, flags = { 0 })
public class VddlrCm extends PluginForHost {

    private static String       DLURL = null;
    private final static String KEY1  = "a2x1Y3p5aw==";
    private final static String KEY2  = "NDYzNzc5MDRjNmM4";
    private final static String IV    = "AAAAAAAAAAA=";

    public VddlrCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.viddler.com/terms-of-use/";
    }

    private String getAMFRequest(final String key, final String value) {
        byte bLen = 0x00;
        final byte[] b = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x1b, 0x76, 0x69, 0x64, 0x64, 0x6c, 0x65, 0x72, 0x47, 0x61, 0x74, 0x65, 0x77, 0x61, 0x79, 0x2e, 0x67, 0x65, 0x74, 0x56, 0x69, 0x64, 0x65, 0x6f, 0x49, 0x6e, 0x66, 0x6f, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00 };
        final byte[] b1 = new byte[] { bLen, 0x0a, 0x00, 0x00, 0x00, 0x03, 0x02, 0x00, (byte) key.length() };
        String postdata = new String(b1) + new String(key.getBytes()) + new String(new byte[] { 0x05, 0x05 });
        if (value != null) {
            final byte[] b2 = new byte[] { 0x02, 0x00, (byte) value.length() };
            postdata = new String(b1) + new String(key.getBytes()) + new String(b2) + new String(value.getBytes()) + new String(new byte[] { 0x05 });
        }
        bLen = (byte) postdata.length();
        return new String(b) + postdata;
    }

    private byte[] getBlowfish(final String strkey, final byte[] value, final boolean decrypt) {
        try {
            final byte[] iv = Base64.decode(IV);
            final Cipher c = Cipher.getInstance("Blowfish/CFB/NoPadding");
            final SecretKeySpec keySpec = new SecretKeySpec(Base64.decode(strkey), "Blowfish");
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            if (decrypt) {
                c.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            } else {
                c.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            }
            final byte[] result = c.doFinal(value);
            return result;
        } catch (final Throwable e) {
            return null;
        }
    }

    private String getLink(final String path) {
        final byte[] decrypted = getBlowfish(KEY1, JDHexUtils.getByteArray(path), true);
        final long localTime = (System.currentTimeMillis() + 300000) / 1000;
        String sid = "ec_expire=" + localTime;
        sid = "ec_secure=" + String.format("%03d", sid.length() + 14) + "&" + sid;
        final byte[] finaldecrypt = getBlowfish(KEY2, sid.getBytes(), false);
        if (decrypted == null || decrypted.length == 0) { return null; }
        final String dllink = new String(decrypted) + "?" + JDHexUtils.getHexString(finaldecrypt);
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestVideo(downloadLink);
        if (DLURL == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLURL, true, 0);
        dl.setFilenameFix(true);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        requestVideo(downloadLink);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(DLURL).getContentType().contains("html")) {
                downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) {
                br.getHttpConnection().disconnect();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private AvailableStatus requestVideo(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        final String dllink = downloadLink.getDownloadURL();
        String filename = null, key = null, value = null;

        if (!new Regex(dllink, "/(player|simple)/").matches()) {
            br.getPage(dllink);
            if (br.containsHTML("Video not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            filename = Encoding.htmlDecode(br.getRegex("title=\"(.*?)\"").getMatch(0, 3));
            if (filename == null) {
                filename = br.getRegex("<title>(.*?),.*?</title>").getMatch(0);
            }
            final String keys = br.getRegex("addVariable\\((.*?)\\)").getMatch(0);
            key = new Regex(keys, "key=(.*?)&").getMatch(0);
            value = new Regex(keys, "viewToken=(.*?)&").getMatch(0);
        } else {
            key = new Regex(dllink, "(player|simple)/(\\w+)/?").getMatch(1);
            value = null;
        }
        if (key == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        br.setFollowRedirects(true);
        br.getPage(dllink);
        final String postdata = getAMFRequest(key, value);
        final String url = "http://www.viddler.com/amfgateway.action";
        br.getHeaders().put("Content-Type", "application/x-amf");
        br.postPageRaw(url, postdata);

        final byte[] raw = br.toString().getBytes();
        if (raw == null || raw.length == 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] < 32 || raw[i] > 127) {
                raw[i] = 35; // #
            }
        }
        if (filename == null) {
            filename = new Regex(new String(raw), "title[#]+(.*?)[#]+trackbacksCnt").getMatch(0);
        }
        final String path = new Regex(new String(raw), "path[#]+\\??(.*?)[#]+type").getMatch(0);
        DLURL = getLink(path);
        if (DLURL == null || !br.containsHTML("onResult") || filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        downloadLink.setName(filename + ".flv");
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
