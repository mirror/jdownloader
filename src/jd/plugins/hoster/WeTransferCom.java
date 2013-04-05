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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com" }, urls = { "https?://(www\\.)?((wtrns\\.fr|we\\.tl)/[\\w\\-]+|wetransfer\\.com/downloads/[a-z0-9]+/[a-z0-9]+)" }, flags = { 0 })
public class WeTransferCom extends PluginForHost {

    private String HASH   = null;
    private String CODE   = null;
    private String DLLINK = null;

    public WeTransferCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://wetransfer.info/terms/";
    }

    private String getAMFRequest() {
        final String data = "0A000000020200" + getHexLength(CODE) + JDHexUtils.getHexString(CODE) + "0200" + getHexLength(HASH) + JDHexUtils.getHexString(HASH);
        return JDHexUtils.toString("000000000001002177657472616E736665722E446F776E6C6F61642E636865636B446F776E6C6F616400022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        final String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // More chunks are possible for some links but not for all
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        String dlink = link.getDownloadURL();
        if (dlink.matches("http://(wtrns\\.fr|we\\.tl)/[\\w\\-]+")) {
            br.setFollowRedirects(false);
            br.getPage(dlink);
            dlink = br.getRedirectLocation();
            if (dlink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        }
        HASH = new Regex(dlink, "([0-9a-f]+)$").getMatch(0);
        CODE = new Regex(dlink, "wetransfer\\.com/downloads/([a-z0-9]+)/").getMatch(0);
        if (HASH == null || CODE == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        br.getPage("https://www.wetransfer.com/api/v1/transfers/" + CODE + "/download?recipient_id=&security_hash=" + HASH + "&password=&ie=false");
        DLLINK = br.getRegex("\"direct_link\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (DLLINK != null) {
            String filename = new Regex(Encoding.htmlDecode(DLLINK), "filename=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        } else {
            /** Old way */
            // AMF-Request
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Accept-Charset", null);
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Content-Type", "application/x-amf");
            br.getHeaders().put("Referer", "https://www.wetransfer.com/index.swf?nocache=" + String.valueOf(System.currentTimeMillis() / 1000));
            br.postPageRaw("https://v1.wetransfer.com/amfphp/gateway.php", getAMFRequest());

            /* TODO: remove me after 0.9xx public */
            br.getHeaders().put("Content-Type", null);

            // successfully request?
            final int rC = br.getHttpConnection().getResponseCode();
            if (rC != 200) {
                logger.warning("File not found! Link: " + dlink);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            final StringBuffer sb = new StringBuffer();
            for (final byte element : br.toString().getBytes()) {
                if (element < 127) {
                    if (element > 31) {
                        sb.append((char) element);
                    } else {
                        sb.append("#");
                    }
                }
            }
            final String result = sb.toString();
            if (new Regex(result, "(download_error_no_download|download_error_file_expired)").matches()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

            final String filename = new Regex(result, "#filename[#]+\\$?([^<>#]+)").getMatch(0);
            final String filesize = new Regex(result, "#size[#]+(\\d+)[#]+").getMatch(0);
            DLLINK = new Regex(result, "#awslink[#]+\\??([^<>#]+)").getMatch(0);

            if (filename == null || filesize == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}