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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "libgen.info" }, urls = { "https?://(?:www\\.)?(?:libgen\\.net|golibgen\\.io)/view\\.php\\?id=\\d+|http://libgen\\.(?:in|io)/(?:[^/]+/)?(?:get|ads)\\.php\\?md5=[A-Za-z0-9]{32}(?:\\&key=[A-Z0-9]+)?|https?://(?:libgen\\.(?:net|io)|golibgen\\.io)/covers/\\d+/[^<>\"\\']*?\\.(?:jpg|jpeg|png|gif)" })
public class LibGenInfo extends PluginForHost {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "libgen.net", "libgen.io", "golibgen.io" };
    }

    public LibGenInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://libgen.info/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("libgen.net/", "golibgen.io/"));
    }

    private static final String  type_picture        = ".+/covers/\\d+/[^<>\"\\']*?\\.(?:jpg|jpeg|png)";
    public static final String   type_libgen_get     = "/get\\.php\\?md5=[A-Za-z0-9]{32}";

    private static final boolean FREE_RESUME         = false;
    private static final int     FREE_MAXCHUNKS      = 1;
    private static final int     FREE_MAXDOWNLOADS   = 2;

    private String               dllink              = null;
    private boolean              allow_html_download = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        allow_html_download = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            final String server_filename = getFileNameFromHeader(con);
            if (server_filename != null && server_filename.contains(".html")) {
                allow_html_download = true;
            }
            final boolean is_a_downloadable_file = !con.getContentType().contains("html") || allow_html_download;
            if (is_a_downloadable_file) {
                dllink = link.getDownloadURL();
                link.setDownloadSize(con.getLongContentLength());
                /* Final filename is sometimes set in decrypter */
                if (link.getFinalFileName() == null) {
                    link.setFinalFileName(Encoding.htmlDecode(server_filename));
                }
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }

        if (br.containsHTML(">There are no records to display\\.<") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null, filesize = null;
        if (link.getDownloadURL().contains("/ads.php?md5=")) {
            final String author = getBracketResult("author");
            final String title = getBracketResult("title");
            filename = author + " - " + title;
        } else {
            filename = br.getRegex("name=\"hidden0\" type=\"hidden\"\\s+value=\"([^<>\"\\']+)\"").getMatch(0);
            filesize = br.getRegex(">size\\(bytes\\)</td>[\t\n\r ]+<td>(\\d+)</td>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private String getBracketResult(final String key) {
        final String result = br.getRegex(key + "\\s*=\\s*\\{(.*?)\\},?[\r\n]+").getMatch(0);
        return result;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            if (downloadLink.getDownloadURL().contains("/ads.php?md5=")) {
                dllink = br.getRegex("<a href=(\"|')(/get\\.php\\?md5=[a-f0-9]{32}.*?)\\1").getMatch(1);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
            } else {
                Form download = br.getFormbyProperty("name", "receive");
                if (download == null) {
                    download = br.getForm(1);
                }
                if (download == null) {
                    logger.info("Could not find download form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // they have use multiple quotation marks within form input lines. This returns null values.
                download = cleanForm(download);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download, FREE_RESUME, FREE_MAXCHUNKS);
            }
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        }
        if (dl.getConnection().getContentType().contains("html") && !allow_html_download) {
            br.followConnection();
            if (br.containsHTML(">Sorry, huge and large files are available to download in local network only, try later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /**
     * If form contain both " and ' quotation marks within input fields it can return null values, thus you submit wrong/incorrect data re:
     * InputField parse(final String data). Affects revision 19688 and earlier!
     *
     * TODO: remove after JD2 goes stable!
     *
     * @author raztoki
     */
    private Form cleanForm(Form form) {
        if (form == null) {
            return null;
        }
        String data = form.getHtmlCode();
        ArrayList<String> cleanupRegex = new ArrayList<String>();
        cleanupRegex.add("(\\w+\\s*=\\s*\"[^\"]+\")");
        cleanupRegex.add("(\\w+\\s*=\\s*'[^']+')");
        for (String reg : cleanupRegex) {
            String results[] = new Regex(data, reg).getColumn(0);
            if (results != null) {
                String quote = new Regex(reg, "(\"|')").getMatch(0);
                for (String result : results) {
                    String cleanedResult = result.replaceFirst(quote, "\\\"").replaceFirst(quote + "$", "\\\"");
                    data = data.replace(result, cleanedResult);
                }
            }
        }
        Form ret = new Form(data);
        ret.setAction(form.getAction());
        ret.setMethod(form.getMethod());
        return ret;
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