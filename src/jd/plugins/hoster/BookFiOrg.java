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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bookfi.net" }, urls = { "https?://(www\\.)?([a-z]+\\.)?(?:bookfi\\.(?:org|net)|bookzz\\.org|b-ok\\.org||b-ok\\.cc)/((book|dl)/\\d+(/[a-z0-9]+)?|md5/[A-F0-9]{32})" })
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
        if (link.getDownloadURL().contains("bookfi.")) {
            if (link.getDownloadURL().matches("https?://(?:www\\.)?bookfi\\.(?:net|org)/dl/\\d+.+")) {
                final String fid = new Regex(link.getDownloadURL(), "bookfi\\.(?:net|org)/dl/(\\d+)").getMatch(0);
                link.setUrlDownload("https://bookfi.net/book/" + fid);
            } else {
                link.setUrlDownload(link.getDownloadURL().replaceFirst("(?:www\\.)?(?:[a-z]{2}\\.)?bookfi.org/", "en.bookfi.net/"));
            }
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
            String bookid = br.getRegex("<a href=\"/?(book/\\d+)\".*?</a>\\s*</?h3").getMatch(0);
            if (bookid == null) {
                // bookos && bookzz
                bookid = br.getRegex("<a href=\"/?(book/\\d+/[a-z0-9]+)\".*?</a>\\s*</?h3").getMatch(0);
                if (bookid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            getPage("/" + bookid);
        }
        // bookfi
        String[] info = br.getRegex("<a class=\"button active[^\"]*\" href=\"([^\"]+)\".*?>.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
        if (info == null) {
            // bookos
            info = br.getRegex("<a class=\"button active dnthandler\" href=\"([^\"]+)\".*?>.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
            if (info == null) {
                info = br.getRegex("<a class=\"btn btn-primary dlButton\" href=\"([^\"]+)\".*?>.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
            }
            if (info == null || info[0] == null || info[1] == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        // Goes to download link to find out filename
        String filename = br.getRegex("<h2 style=\"display:inline\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 style=\"color:#49AFD0\"  itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1[^<]*itemprop=\"name\"[^<]*>\\s*([^<>\"]*?)\\s*</h1>").getMatch(0);
            }
        }
        dllink = info[0];
        {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (con.isOK() && (con.isContentDisposition() || !StringUtils.containsIgnoreCase(con.getContentType(), "text"))) {
                    final String headerFileName = getFileNameFromDispositionHeader(con);
                    if (headerFileName != null) {
                        param.setFinalFileName(headerFileName);
                    } else if (filename != null) {
                        param.setFinalFileName(filename.trim() + getFileNameExtensionFromString(getFileNameFromHeader(con), "pdf"));
                    }
                    if (con.getCompleteContentLength() > 0) {
                        param.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        param.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.PDF);
        if (param.getVerifiedFileSize() < 0) {
            param.setDownloadSize(SizeFormatter.getSize(info[1]));
        }
        if (parameter.contains("/md5/")) {
            // now everything is aok, we should correct to a single url/file uid
            param.setUrlDownload(br.getURL());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("There are more then \\d+ downloads from this IP during last")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
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
