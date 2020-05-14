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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "8muses.com" }, urls = { "https?://(?:www\\.)?8muses\\.com/(?:(comics/)?picture/([^/]+/){1,}\\d+|forum/(?:data/)?attachments/.+)" })
public class EightMusesCom extends antiDDoSForHost {
    public EightMusesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    private String              dllink      = null;
    private static final String TYPE_DIRECT = ".+8muses\\.com/forum/.+";

    @Override
    public String getAGBLink() {
        return "http://www.8muses.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        br = new Browser();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null;
        if (link.getPluginPatternMatcher().matches(TYPE_DIRECT)) {
            dllink = link.getPluginPatternMatcher();
        } else {
            getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<b>Notice</b>:")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = new Regex(link.getPluginPatternMatcher(), "8muses\\.com/(?:[^/]*/)?picture/(?:\\d+\\-)?(.+)").getMatch(0);
            filename = filename.replace("/", "_");
            final String ractive_public = n(br.getRegex("<script id=\"ractive-public\" type=\"text/plain\">\\s*(.*?)\\s*<").getMatch(0));
            final String imageDir = br.getRegex("imageDir\" value=\"(/data/.{2}/)\"").getMatch(0);
            final String imageName = br.getRegex("imageName\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String imageHost = br.getRegex("imageHost\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (imageDir != null && imageName != null) {
                dllink = imageDir + imageName;
            } else if (imageHost != null && imageName != null) {
                dllink = imageHost + "/image/fl/" + imageName;
            } else if (imageName != null) {
                /* 2018-02-09 */
                dllink = "https://www.8muses.com/image/fl/" + imageName;
            } else if (ractive_public != null) {
                final String image = new Regex(ractive_public, "\"picture\"\\s*:\\s*\\{.*?\"public.*?\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                if (image != null) {
                    dllink = "https://www.8muses.com/image/fl/" + image + ".jpg";
                }
            }
            if (ractive_public.contains("\"pictures\":[]")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = encodeUnicode(filename);
            String ext = getFileNameExtensionFromString(dllink);
            /* Make sure that we get a correct extension */
            if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
                ext = ".jpg";
            }
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        br.setFollowRedirects(true);
        if (dllink != null && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    if (con.getHeaderField("cf-bgj") != null && !link.hasProperty(BYPASS_CLOUDFLARE_BGJ)) {
                        link.setProperty(BYPASS_CLOUDFLARE_BGJ, Boolean.TRUE);
                    }
                    link.setDownloadSize(con.getLongContentLength());
                    if (filename == null) {
                        link.setFinalFileName(Plugin.getFileNameFromHeader(con));
                    }
                } else if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
                link.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static final String BYPASS_CLOUDFLARE_BGJ = "bpCfBgj";

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (link.getProperty(BYPASS_CLOUDFLARE_BGJ) != null) {
            logger.info("Apply Cloudflare BGJ bypass");
            dllink = URLHelper.parseLocation(br.getURL(dllink), "&bpcfbgj=" + System.nanoTime());
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                /* 2020-05-14: Typically this means a URL is only downloadable via account */
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                throw new AccountRequiredException();
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        dl.startDownload();
    }

    private String n(String t) {
        if (t == null) {
            return null;
        }
        if (!t.startsWith("!")) {
            return t;
        }
        final Matcher m = Pattern.compile("([\\x21-\\x7e])").matcher(t.substring(1).replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&"));
        final StringBuffer sb = new StringBuffer(t.length());
        while (m.find()) {
            final String search = m.group(1);
            if (search == null) {
                break;
            } else {
                final String replacement = String.valueOf((char) (33 + (search.codePointAt(0) + 14) % 94));
                m.appendReplacement(sb, replacement);
            }
        }
        return sb.toString();
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
}
