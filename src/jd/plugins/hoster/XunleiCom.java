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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xunlei.com" }, urls = { "http://xunleidecrypted\\.com/\\d+" }, flags = { 0 })
public class XunleiCom extends PluginForHost {

    public XunleiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://kuai.xunlei.com/service-agreement";
    }

    private String DLLINK = null;

    /* TODO: Add measures to prevent captchas (e.g. re-use cookies from decrypter). */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        final String origin = downloadLink.getStringProperty("mainlink", null);
        if (origin == null) {
            return null;
        }
        br.getPage(origin);
        if (br.getURL().contains("kuai.xunlei.com/invalid")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = downloadLink.getStringProperty("decryptedfilename", null);
        final String filesize = downloadLink.getStringProperty("decryptedfilesize", null);
        downloadLink.setFinalFileName(filename);
        downloadLink.setDownloadSize(Long.parseLong(filesize));
        DLLINK = downloadLink.getStringProperty("directlink", null);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        DLLINK = checkDirectLink(downloadLink, "directlink");
        if (DLLINK == null) {
            DLLINK = updateDownloadLink(downloadLink);
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", DLLINK);
        dl.startDownload();
    }

    private String updateDownloadLink(final DownloadLink downloadLink) throws IOException, Exception {
        this.br = new Browser();
        final String fid = downloadLink.getStringProperty("decrypted_fid", null);
        try {
            final String origin = downloadLink.getStringProperty("mainlink", null);
            if (origin == null) {
                return null;
            }
            br.getPage(origin);
            handleCaptcha(downloadLink);
            final String[] links = br.getRegex("file_url=\"(https?://[a-z0-9\\.\\-]+\\.xunlei\\.com/download\\?fid=[^\"\\'<>]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                return null;
            }
            for (final String aLink : links) {
                if (aLink.contains(fid)) {
                    return aLink;
                }
            }
        } catch (final IOException e) {
            logger.severe(br.toString());
            throw e;
        }
        return null;
    }

    private boolean handleCaptcha(final DownloadLink dl) throws Exception {
        if (br.containsHTML("http://verify")) {
            logger.info("xunlei.com decrypter: found captcha...");
            for (int i = 0; i <= 3; i++) {
                final String shortkey = br.getRegex("value=\\'([^<>\"]*?)\\' name=\"shortkey\"").getMatch(0);
                final String captchaLink = br.getRegex("\"(http://verify\\d+\\.xunlei\\.com/image\\?t=[^<>\"]*?)\"").getMatch(0);
                if (captchaLink == null || shortkey == null) {
                    logger.warning("Host plugin broken for link: " + br.getURL());
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String code = getCaptchaCode(captchaLink, dl);
                br.getPage("http://kuai.xunlei.com/webfilemail_interface?v_code=" + code + "&shortkey=" + shortkey + "&ref=&action=check_verify");
                if (!br.containsHTML("http://verify")) {
                    break;
                }
            }
            if (br.containsHTML("http://verify")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            logger.info("Captcha passed!");
        }
        return true;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            String cookieid = new Regex(dllink, "\\&ck=([^<>\"]*?)\\&").getMatch(0);
            if (cookieid == null) {
                return null;
            }
            URLConnectionAdapter con = null;
            try {
                /* It's important to use a fresh browser here! */
                this.br = new Browser();
                br.setFollowRedirects(true);
                /* Important cookie, needed to re-use directlinks! */
                br.setCookie("http://xunlei.com/", "kuaichuanid", cookieid);
                /* Do NOT use Head connection here! */
                con = br.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
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