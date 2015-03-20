//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.net.SocketTimeoutException;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cuntriot.com", "iboner.com", "yobt.tv", "bestbigmovs.com", "coolmovs.com", "gayspower.com", "bigxvideos.com", "hdporn.in", "fetishok.com", "nastymovs.com", "angrymovs.com", "madmovs.com" }, urls = { "http://(www\\.)?cuntriot\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?iboner\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?yobt\\.tv/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?bestbigmovs\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?coolmovs\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?gayspower\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?bigxvideos\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?hdporn\\.in/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?fetishok\\.com/content/\\d+/[a-z0-9\\-]+\\.html",
        "http://(www\\.)?nastymovs\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?angrymovs\\.com/content/\\d+/[a-z0-9\\-]+\\.html", "http://(www\\.)?madmovs\\.com/content/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class YobtTv extends PluginForHost {

    private String DLLINK = null;

    public YobtTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Porn_terms_html_script V0.1 */
    /* Tags: Script, template */
    // Notes: yobt.com also belongs to these but has not been added here as it also has it's own decrypter.
    // Notes 2: Old .bismarck decrypt handling was disabled AFTER revision 24665 because it was not needed anymore (though still working)
    // because more domains were added to this plugin

    @Override
    public String getAGBLink() {
        return "http://www.yobt.tv/terms.html";
    }

    private static final boolean useCryptHandling = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("class=\"error404\"") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h2 class=\"left\">([^<>\"]*?)</hh>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h2 style=\"[^<>\"]+\">([^<>\"]*?)</h2>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (useCryptHandling) {
            br.getPage("http://www." + downloadLink.getHost() + "/freeporn/" + new Regex(downloadLink.getDownloadURL(), "yobt\\.tv/content/(\\d+)/").getMatch(0) + ".xml");
            final String bismarkishID = br.getRegex("file=\\'(.*?)\\'").getMatch(0);
            if (bismarkishID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (bismarkishID.equals("")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = decryptTheSecret(bismarkishID);
        } else {
            DLLINK = br.getRegex("\\'(?:file|video)\\'[\t\n\r ]*?:[\t\n\r ]*?(?:\"|\\')(http[^<>\"]*?)(?:\"|\\')").getMatch(0);
        }
        if (DLLINK == null || !DLLINK.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
        } catch (final SocketTimeoutException e) {
            // Timeout for stream links = dead
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String decryptTheSecret(final String bismarkishID) {
        int i = 0;
        String dec = "", plain = "";
        while (i < bismarkishID.length()) {
            dec = bismarkishID.substring(i, i + 2);
            plain += String.valueOf((char) ((dec.codePointAt(0) - 65) * 16 + dec.codePointAt(1) - 65));
            i += 2;
        }
        return plain;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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