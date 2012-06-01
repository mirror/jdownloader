//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 16015 $", interfaceVersion = 2, names = { "xaxtube.com" }, urls = { "http://(www\\.)?xaxtube\\.com/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class XaxTubeCom extends PluginForHost {

    public static class XBase64 {
        private static String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

        public static String decode(String data, final int offset) {
            if (offset > 0) {
                int a = 0;
                int b = a;
                final char[] out = new char[data.length()];
                for (int c = 0; c < data.length(); c++) {
                    out[b] = data.charAt(c);
                    b += offset;
                    if (b >= data.length()) {
                        a++;
                        b = a;
                    }
                }
                data = new String(out);
            }
            return encodedByteArray(data);
        }

        private static String encodedByteArray(final String data) {
            final int[] dataBuffer = new int[4];
            final int[] outputBuffer = new int[3];
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < data.length(); i += 4) {
                int j = 0;
                while (j < 4 && i + j < data.length()) {
                    dataBuffer[j] = BASE64_CHARS.indexOf(data.charAt(i + j));
                    j++;
                }
                outputBuffer[0] = (dataBuffer[0] << 2) + ((dataBuffer[1] & 48) >> 4);
                outputBuffer[1] = ((dataBuffer[1] & 15) << 4) + ((dataBuffer[2] & 60) >> 2);
                outputBuffer[2] = ((dataBuffer[2] & 3) << 6) + dataBuffer[3];
                for (int k = 0; k < outputBuffer.length; k++) {
                    if (dataBuffer[k + 1] == 64) {
                        break;
                    }
                    sb.append((char) outputBuffer[k]);
                }
            }
            return sb.toString();
        }

        public XBase64() throws Exception {
            throw new Exception("Base64 class is static container only");
        }

    }

    private String DLLINK = null;

    public XaxTubeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decodeExtendedBase64(final String s) {
        if (s == null || s.length() < 10) { return null; }
        return XBase64.decode(s.substring(4), "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".indexOf(s.substring(3, 4)) + 2);
    }

    @Override
    public String getAGBLink() {
        return "http://xaxtube.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://xaxtube.com/") || br.containsHTML("<title>Free Porn Videos, Sex Tube Clips, XXX Porn Movies \\- XaXTube\\.com</title>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<h1>([^<>\"\\'/]+)</h1>").getMatch(0);
        br.getPage("http://xaxtube.com/data/" + new Regex(downloadLink.getDownloadURL(), "xaxtube\\.com/(\\d+)/").getMatch(0));
        DLLINK = new Regex(decodeExtendedBase64(br.toString().trim()), "vid=\'(.*?)\'").getMatch(0);
        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".mp4";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
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

}