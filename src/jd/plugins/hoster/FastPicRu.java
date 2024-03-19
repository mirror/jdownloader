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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FastPicRu extends PluginForHost {
    public FastPicRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* We're only downloading small files so 1 chunk is enough. */
    private static final int free_maxchunks = 1;
    private String           dllink         = null;

    @Override
    public String getAGBLink() {
        return "https://fastpic.ru/";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fastpic.org", "fastpic.ru" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.|i\\d+\\.)?" + buildHostsPatternPart(domains) + "/(?:(?:full)?view/.*?_?([^<>\"/]+)\\.html|big/[^/]+/[^/]+/[^/]+/_?([a-f0-9]{32}\\.[A-Za-z]+[^\"]*))");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        String ret = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (ret == null) {
            ret = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        }
        return ret;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String title = getFID(link);
        title = Encoding.htmlDecode(title);
        title = title.trim();
        final String extDefault = ".jpg";
        if (!link.isNameSet()) {
            link.setName(this.correctOrApplyFileNameExtension(title, extDefault));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        dllink = null;
        if (false) {
            final GetRequest request = new GetRequest(link.getPluginPatternMatcher()) {
                @Override
                protected String getSuggestedAcceptHeader(URL url) {
                    // text/html will cause redirect to website
                    return DEFAULTACCEPTHEADER;
                }
            };
            br.getPage(request);
        } else {
            final URLConnectionAdapter con = br.openGetConnection(link.getPluginPatternMatcher());
            try {
                // may return direct image or website
                if (StringUtils.contains(con.getContentType(), "text/html")) {
                    br.followConnection();
                } else {
                    handleConnectionErrors(br, con);
                    dllink = con.getRequest().getUrl();
                }
            } catch (PluginException e) {
                logger.log(e);
            } finally {
                con.disconnect();
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (dllink == null) {
            dllink = br.getRegex("<span class=\"text-muted d-lg-none\">нажмите для увеличения</span>\\s*<img src=\"(https://[^\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://[a-z0-9]+\\.[^/]+/big/[^/]+/[^/]+/[^/]+/_?[a-f0-9]{32}\\.[A-Za-z]+[^\"]*)\"").getMatch(0);
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                final String ext = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                if (ext != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + ext));
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
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (con.getURL().toExternalForm().contains("/not_found.gif")) {
            /* https://static.fastpic.org/not_found.gif */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Image broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
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
