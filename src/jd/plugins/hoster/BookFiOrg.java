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

import java.io.IOException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

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
                link.setUrlDownload(link.getDownloadURL().replaceAll("(?:(www|[a-z]{2})\\.)?" + Browser.getHost(link.getPluginPatternMatcher()) + "/", "en.bookfi.net/"));
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static final String TYPE_MD5 = "https?://[^/]+/md5/([a-f0-9]{32})$";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        final String parameter = link.getPluginPatternMatcher();
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"notFound") || br.containsHTML(">\\s*If you did not find the book or it was closed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (parameter.matches(TYPE_MD5)) {
            link.setMD5Hash(new Regex(parameter, TYPE_MD5).getMatch(0));
        }
        /* We expect a redirect here */
        if (parameter.contains("/md5/") && br.getURL().contains("/md5/")) {
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
        final String filesizeStr = br.getRegex("Download\\s*\\([a-z0-9]+,\\s*(\\d+[^<>\"\\'\\)]+)\\)").getMatch(0);
        if (filesizeStr != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        }
        final String title = PluginJSonUtils.getJson(br, "title");
        if (!StringUtils.isEmpty(title)) {
            link.setName(title + ".djvu");
        }
        // if (parameter.contains("/md5/")) {
        // // now everything is ok, we should correct to a single url/file uid
        // param.setUrlDownload(br.getURL());
        // }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        String dllink = br.getRegex("(/dl/[^<>\"\\']+)").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
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
