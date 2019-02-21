//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.net.URL;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fanfox.net" }, urls = { "https?://fanfox\\.net/manga/[^/]+/(?:v[A-Za-z0-9]+/)?c[\\d\\.]+/\\d+\\.html" })
public class Mangafox extends PluginForHost {
    public Mangafox(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".jpg";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://fanfox.net/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/manga/(.+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(new URL(link.getPluginPatternMatcher()).getHost(), "isAdult", "1");
        br.getPage(link.getPluginPatternMatcher());
        /* Check if main URL is still online */
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex urlregex = new Regex(link.getPluginPatternMatcher(), "(manga/[^/]+.*?)/(\\d+)\\.html$");
        final String urlpart = urlregex.getMatch(0);
        final String page = urlregex.getMatch(1);
        final String chapterID = br.getRegex("var chapterid =(\\d+);").getMatch(0);
        if (chapterID == null || urlpart == null || page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Try to get downloadlink for simgle image */
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://fanfox.net/" + urlpart + "/chapterfun.ashx?cid=" + chapterID + "&page=" + page + "&key=");
        dllink = decodeDownloadLink(br.toString());
        final String url_filename = urlpart.replace("/", "_") + "_" + page;
        String filename = link.getFinalFileName();
        if (StringUtils.isEmpty(filename)) {
            filename = url_filename;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext;
        if (!StringUtils.isEmpty(dllink)) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(filename);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) {
        if (br.containsHTML("cannot be found|not available yet")) {
            return true;
        } else if (!br.containsHTML("class=\"reader\\-main")) {
            return true;
        }
        return false;
    }

    private String decodeDownloadLink(final String s) throws IOException {
        String decoded = null;
        final String js = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getMatch(0);
        if (js != null) {
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                engine.eval("var res = " + js + ";");
                decoded = (String) engine.get("res");
            } catch (final Exception e) {
                logger.log(e);
            }
        }
        if (decoded == null) {
            try {
                final Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
                String p = params.getMatch(0).replaceAll("\\\\", "");
                final int a = Integer.parseInt(params.getMatch(1));
                int c = Integer.parseInt(params.getMatch(2));
                // '|a|b|c|' will result in '','a','b' and 'c' and last empty will not be within array
                final String[] k = params.getMatch(3).split("\\|");
                while (c != 0) {
                    c--;
                    if (k.length > c && k[c].length() != 0) {
                        p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                    }
                }
                decoded = p;
            } catch (Exception e) {
                logger.log(e);
            }
        }
        if (decoded != null) {
            final String part1 = new Regex(decoded, "var pix=\"((https?|//)[^\"]+)\"").getMatch(0);
            final String part2 = new Regex(decoded, "var pvalue=\\[\"([^\"]+)\"").getMatch(0);
            if (part1 != null && part2 != null) {
                final String url = part1 + part2;
                final String ret = br.getURL(url).toString();
                return ret;
            }
        }
        return null;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
}
