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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bookfi.net" }, urls = { "http://(www\\.)?([a-z]+\\.)?(?:bookfi\\.(?:org|net)|bookzz\\.org)/((book|dl)/\\d+(/[a-z0-9]+)?|md5/[A-F0-9]{32})" })
public class BookFiOrg extends antiDDoSForHost {
    // DEV NOTES
    // they share the same template
    // hosted on different IP ranges
    public BookFiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://" + this.getHost() + "/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches("http://(?:www\\.)?bookfi\\.(?:net|org)/dl/\\d+.+")) {
            final String fid = new Regex(link.getDownloadURL(), "bookfi\\.(?:net|org)/dl/(\\d+)").getMatch(0);
            link.setUrlDownload("http://bookfi.net/book/" + fid);
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("(?:www\\.)?(?:[a-z]{2}\\.)?bookfi.org/", "en.bookfi.net/"));
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String dllink = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink param) throws Exception {
        correctDownloadLink(param);
        final String parameter = param.getDownloadURL();
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.containsHTML("class=\"notFound")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (parameter.contains("/md5/")) {
            // bookfi
            String bookid = br.getRegex("<a href=\"/?(book/\\d+)\"[^>]*><h3").getMatch(0);
            if (bookid == null) {
                // bookos && bookzz
                bookid = br.getRegex("<a href=\"/?(book/\\d+/[a-z0-9]+)\"[^>]*><h3").getMatch(0);
                if (bookid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            getPage("/" + bookid);
        }
        // bookfi
        String[] info = br.getRegex("<a class=\"button active[^\"]*\" href=\"([^\"]+)\">.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
        if (info == null) {
            // bookos
            info = br.getRegex("<a class=\"button active dnthandler\" href=\"([^\"]+)\">.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
            if (info == null || info[0] == null || info[1] == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        // Goes to download link to find out filename
        String filename = br.getRegex("<h2 style=\"display:inline\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 style=\"color:#49AFD0\"  itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = info[0];
        {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                param.setFinalFileName(filename.trim() + getFileNameExtensionFromString(getFileNameFromHeader(con), "pdf"));
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                param.setVerifiedFileSize(con.getLongContentLength());
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        param.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.PDF);
        param.setDownloadSize(SizeFormatter.getSize(info[1]));
        if (parameter.contains("/md5/")) {
            // now everything is aok, we should correct to a single url/file uid
            param.setUrlDownload(br.getURL());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
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
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }
}
