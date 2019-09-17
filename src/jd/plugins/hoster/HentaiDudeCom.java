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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hentaidude.com" }, urls = { "https?://(?:www\\.)?hentaidude\\.com/.*([0-9]+|ova)/" })
public class HentaiDudeCom extends antiDDoSForHost {
    public HentaiDudeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://hentaidude.com/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 - Sorry, nothing found. But feel free to jerk off to one of these videos:")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta (?:name|property)=\"og:(?:title|description)\" content=[\"']([^<>\"]*?)(?: ?\\| Hentaidude.com)").getMatch(0);
        link.setName(Encoding.htmlOnlyDecode(filename) + ".mp4");
        String[][] source = br.getRegex("action:[\r\n\t ]+'msv-get-sources',[\r\n\t ]+id:[\r\n\t ]+'([0-9]+)',[\r\n\t ]+nonce:[\r\n\t ]+'([0-9a-fA-F]+)'").getMatches();
        final PostRequest post = new PostRequest(br.getURL("/wp-admin/admin-ajax.php"));
        post.addVariable("action", "msv-get-sources");
        post.addVariable("id", source[0][0]);
        post.addVariable("nonce", source[0][1]);
        post.getHeaders().put("Origin", "https://hentaidude.com");
        post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
        sendRequest(post);
        String postResult = br.toString();
        final String[] results = HTMLParser.getHttpLinks(postResult, null);
        for (String result : results) {
            if (result.matches("https?://cdn[0-9]+.hentaidude\\.com/index.*")) {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(result));
                    final String contentType = con.getContentType();
                    if (con.isOK() && StringUtils.containsIgnoreCase(contentType, "video/mp4")) {
                        dllink = result;
                        link.setDownloadSize(con.getLongContentLength());
                        return AvailableStatus.TRUE;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } catch (PluginException e) {
                    throw e;
                } catch (Exception e) {
                    logger.log(e);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, -20, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
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
