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
import java.util.Map;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "icloud.com" }, urls = { "http://iclouddecrypted\\.com/[A-Z0-9\\-]+_[a-f0-9]{42}" })
public class IcloudCom extends PluginForHost {
    public IcloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    public static final String   default_ExtensionImage = ".jpg";
    public static final String   default_ExtensionVideo = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume            = true;
    private static final int     free_maxchunks         = 0;
    private static final int     free_maxdownloads      = -1;
    private String               dllink                 = null;
    private boolean              server_issues          = false;

    @Override
    public String getAGBLink() {
        return "http://www.apple.com/legal/internet-services/icloud/ww/";
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* First check if the directlink has already been set via decrypter! */
        dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            final String type = link.getStringProperty("type", null);
            final String folderid = link.getStringProperty("folderid", null);
            if (folderid == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String photoGuid = new Regex(link.getDownloadURL(), "/([A-Z0-9\\-]+)_.+$").getMatch(0);
            final String checksum = new Regex(link.getDownloadURL(), "([a-f0-9]+)$").getMatch(0);
            final String postData = String.format("{\"photoGuids\":[\"%s\"],\"derivatives\":{\"%s\":[\"%s\"]}}", photoGuid, photoGuid, checksum);
            String host = "p43-sharedstreams.icloud.com";
            Map<String, Object> entries = null;
            boolean triedSecondHost = false;
            do {
                this.br.postPageRaw("https://p" + host + "/" + folderid + "/sharedstreams/webasseturls", postData);
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                if (entries.containsKey("X-Apple-MMe-Host")) {
                    if (triedSecondHost) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    host = (String) entries.get("X-Apple-MMe-Host");
                    triedSecondHost = true;
                    continue;
                } else {
                    break;
                }
            } while (true);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (Map<String, Object>) entries.get("items");
            entries = (Map<String, Object>) entries.get(checksum);
            dllink = getDirectlink(entries);
            String filename = getFilenameFromDirectlink(dllink);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (filename != null) {
                filename = Encoding.htmlDecode(filename);
                filename = filename.trim();
                filename = encodeUnicode(filename);
                String ext = getFileNameExtensionFromString(dllink, "video".equalsIgnoreCase(type) ? default_ExtensionVideo : default_ExtensionImage);
                if (!filename.endsWith(ext)) {
                    filename += ext;
                }
                link.setFinalFileName(filename);
            }
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                /* Do NOT use HEAD request, otherwise server will return HTTP/1.1 501 Not Implemented */
                con = br2.openGetConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                    if (filename == null) {
                        filename = getFileNameFromHeader(con);
                        link.setFinalFileName(filename);
                    }
                    /* Save directlink to save http requests in the future. */
                    link.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    public static String getDirectlink(final Map<String, Object> entries) {
        String finallink = null;
        final String server = (String) entries.get("url_location");
        final String path = (String) entries.get("url_path");
        if (server != null && path != null) {
            finallink = "https://" + server + path;
        }
        return finallink;
    }

    public static String getFilenameFromDirectlink(final String finallink) {
        String filename = null;
        if (finallink != null) {
            filename = new Regex(finallink, "/([^/]+)\\?").getMatch(0);
        }
        return filename;
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
