//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;

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

/**
 * @author raztoki
 * */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pururin.com" }, urls = { "^http://(?:www\\.)?pururin\\.com/(?:view/\\d+/\\d+/[\\-a-z0-9]+_\\d+\\.html|download/\\d+/.+)$" }, flags = { 0 })
public class PururinCom extends antiDDoSForHost {

    // raztoki embed video player template.

    private String dllink = null;

    public PururinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pururin.com/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (this.isSelfHostedContent(downloadLink) && dl.getConnection().getResponseCode() == 403) {
                if (br.containsHTML("Error: You cannot download this file because it exceeds your daily download quota.")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download Quota exceeded.", 1 * 60 * 60 * 1000);
                } else if (br.containsHTML("Error: Account required. Visit the account page to register one.")) {
                    try {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account required.", PluginException.VALUE_ID_PREMIUM_ONLY);
                    } catch (final Throwable e) {
                        if (e instanceof PluginException) {
                            throw (PluginException) e;
                        }
                    }
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Account required.");
                } else if (br.containsHTML("Error: Invalid or expired download link. Please visit the site for a new one.")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        getPage(downloadLink.getDownloadURL());
        if (this.isSelfHostedContent(downloadLink)) {
            dllink = br.getRegex("(/get/[a-zA-Z0-9]+/[^\"]+)[^>]+download-button").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String filename = new Regex(dllink, "/([^/]+)$").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setName(filename);
            return AvailableStatus.TRUE;
        } else {
            dllink = br.getRegex("\"(/f/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.urlDecode(dllink, false);
            Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(dllink);
                // only way to check for made up links... or offline is here
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    int links_length = downloadLink.getIntegerProperty("links_length", 1);
                    DecimalFormat df_links = new DecimalFormat("00");
                    if (links_length > 999) {
                        df_links = new DecimalFormat("0000");
                    } else if (links_length > 99) {
                        df_links = new DecimalFormat("000");
                    }
                    // now we have a connection to the link we should format again! and provide proper file extension.
                    final String[] fn = new Regex(getFileNameFromHeader(con), "([^/]+)(_|-)(\\d+)(\\.[a-z0-9]{3,4})$").getRow(0);
                    downloadLink.setFinalFileName(fn[0] + "-" + df_links.format(Integer.parseInt(fn[2])) + fn[3]);
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
    }

    private boolean isSelfHostedContent(final DownloadLink downloadLink) {
        if (downloadLink == null) {
            return false;
        }
        return downloadLink.getDownloadURL().matches(".+/download/\\d+/.+$");
    }

    public void getPage(final String page) throws Exception {
        super.getPage(page);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        super.prepBrowser(prepBr, host);
        prepBr.setConnectTimeout(90 * 1000);
        return prepBr;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}