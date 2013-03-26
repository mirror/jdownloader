//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.net.InetAddress;
import java.net.URL;
import java.util.logging.Level;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xunlei.com" }, urls = { "http://dl\\d+\\.[a-z]+\\d+\\.sendfile\\.vip\\.xunlei\\.com:\\d+/[^<>\"]+\\&get_uid=\\d+" }, flags = { 0 })
public class XunleiCom extends PluginForHost {

    private boolean download = false;

    public XunleiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://kuai.xunlei.com/service-agreement";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(downloadLink.getDownloadURL());
            if (con.getResponseCode() == 403) {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
                if (download == false) return AvailableStatus.UNCHECKABLE;
                /* we are trying to download the file, fetch new link and retry */
                String url = updateDownloadLink(downloadLink);
                if (url == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setUrlDownload(url);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (!con.getContentType().contains("html")) {
                String name = getFileNameFromURL(new URL(downloadLink.getDownloadURL()));
                if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(Encoding.htmlDecode(name));
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        download = true;
        requestFileInformation(downloadLink);
        String url = downloadLink.getDownloadURL();
        try {
            String host = new Regex(url, "http://(.*?)(:|/)").getMatch(0);
            InetAddress ip = InetAddress.getByName(host);
            url = url.replace("http://" + host, "http://" + ip.getHostAddress());
        } catch (final Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, -3);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String updateDownloadLink(final DownloadLink downloadLink) throws IOException, PluginException {
        final Browser br = new Browser();
        try {
            final String origin = downloadLink.getStringProperty("origin", null);
            if (origin == null) return null;
            br.setCustomCharset("utf-8");
            br.getPage(origin);
            if (br.containsHTML("http://verify")) {
                logger.info("xunlei.com decrypter: found captcha...");
                final String fid = new Regex(origin, "([A-Z]+)$").getMatch(0);
                for (int i = 0; i <= 3; i++) {
                    final String captchaLink = br.getRegex("\"(http://verify\\d+\\.xunlei\\.com/image\\?t=[^<>\"]*?)\"").getMatch(0);
                    if (captchaLink == null) {
                        logger.warning("Decrypter broken for link: " + downloadLink.getDownloadURL());
                        return null;
                    }
                    final String code = getCaptchaCode(captchaLink, downloadLink);
                    br.getPage("http://kuai.xunlei.com/webfilemail_interface?v_code=" + code + "&shortkey=" + fid + "&ref=&action=check_verify");
                    if (!br.containsHTML("http://verify")) break;
                }
                if (br.containsHTML("http://verify")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                logger.info("Captcha passed!");
            }
            final String originLink = new Regex(downloadLink.getDownloadURL(), "https?://.*?/(.*?)\\?").getMatch(0);
            final String[] links = br.getRegex("\"(http://dl\\d+\\.[a-z]\\d+\\.sendfile\\.vip\\.xunlei\\.com:\\d+/[^/<>\"]+\\?key=[a-z0-9]+\\&file_url=[^/<>\"]+\\&file_type=\\d+\\&authkey=[A-Z0-9]+\\&exp_time=\\d+&from_uid=\\d+\\&task_id=\\d+\\&get_uid=\\d+)").getColumn(0);
            if (links == null || links.length == 0) { return null; }
            for (String aLink : links) {
                if (aLink.contains(originLink)) return aLink;
            }
        } catch (final IOException e) {
            logger.severe(br.toString());
            throw e;
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}