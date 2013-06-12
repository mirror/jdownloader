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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "purevolume.com" }, urls = { "decrypted://(www\\.)?purevolume\\.com/(new/)?\\w+(/albums/[\\w\\+\\-]+)?\\&songId=[\\w\\-]+" }, flags = { 0 })
public class PurevolumeCom extends PluginForHost {

    private String  DLLINK       = null;

    private boolean NOTFORSTABLE = false;

    public PurevolumeCom(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("decrypted://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.purevolume.com/terms_of_use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String beautifierString(final Browser amf) {
        final StringBuffer sb = new StringBuffer();
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
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

    private byte[] createAMFRequest(String query) {
        if (query == null) return null;
        String data = "0A000000010200";
        data += getHexLength(query) + JDHexUtils.getHexString(query);
        return JDHexUtils.getByteArray("0000000000010018506C61796572536572766963652E676574536F6E6755726C00022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final String songId = downloadLink.getStringProperty("SONGID");
        if (songId == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        Browser amf = new Browser();
        getAMFRequest(amf, createAMFRequest(songId));
        if (NOTFORSTABLE) throw new PluginException(LinkStatus.ERROR_FATAL, "JDownloader2 is needed!");
        String result = beautifierString(amf);
        DLLINK = new Regex(result, "(http://.*?)$").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        // In case the link redirects to the finallink
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
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
        String gateway = "https://www.purevolume.com/_gateway/gateway.php";
        try {
            PostRequest request = (PostRequest) amf.createPostRequest(gateway, (String) null);
            request.setPostBytes(b);
            amf.openRequestConnection(request);
            amf.loadConnection(null);
        } catch (Throwable e) {
            /* does not exist in 09581 */
            try {
                amf.postPageRaw(gateway, b);
            } catch (Throwable e1) {
                NOTFORSTABLE = true;
            }
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "FILESIZECHECK", JDL.L("plugins.hoster.purevolume.filesizecheck", "Check file size?")).setDefaultValue(false));
    }

}