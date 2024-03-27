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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornxpNet extends PluginForHost {
    public PornxpNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* Connection stuff */
    private static final boolean free_resume                   = true;
    private static final int     free_maxchunks                = 0;
    private String               dllink                        = null;
    private final String         PROPERTY_TAGS_COMMA_SEPARATED = "tags_comma_separated";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pornxp.net", "pornxp.cc", "pornxp.org" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/videos/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://pornxp.org/legal/terms";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        final String videoid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(videoid + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL()) && !br.getURL().contains(videoid)) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("class=\"player_details\"[^>]*><h1>([^<]+)</h1>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setFinalFileName(title + ".mp4");
        }
        final String tagsHTML = br.getRegex("class=\"tags\">(.*?)</a> </div></div>").getMatch(0);
        if (tagsHTML != null) {
            final String[] tags = new Regex(tagsHTML, "/tags/([^\"]+)").getColumn(0);
            if (tags != null && tags.length > 0) {
                final StringBuilder tagsCommaSeparated = new StringBuilder();
                for (final String tag : tags) {
                    if (tagsCommaSeparated.length() > 0) {
                        tagsCommaSeparated.append(",");
                    }
                    tagsCommaSeparated.append(Encoding.htmlDecode(tag));
                }
                logger.info("tagsCommaSeparated[" + tags.length + "] = " + tagsCommaSeparated);
                link.setProperty(PROPERTY_TAGS_COMMA_SEPARATED, tagsCommaSeparated.toString());
            } else {
                logger.warning("Failed to find tags");
            }
        } else {
            logger.warning("Failed to find tagsHTML");
        }
        /* Find highest quality */
        final String[] qualityurls = br.getRegex("<source src=\"([^\"]+)\"[^>]*type=\"video/mp4\"").getColumn(0);
        if (qualityurls != null && qualityurls.length > 0) {
            int maxHeight = -1;
            for (final String qualityurl : qualityurls) {
                final String qualityheightStr = new Regex(qualityurl, "(\\d+)\\.mp4$").getMatch(0);
                if (qualityheightStr != null) {
                    final int thisQualityHeight = Integer.parseInt(qualityheightStr);
                    if (thisQualityHeight > maxHeight) {
                        maxHeight = thisQualityHeight;
                        dllink = qualityurl;
                    }
                }
                if (this.dllink == null) {
                    dllink = qualityurl;
                }
            }
            if (!StringUtils.isEmpty(dllink)) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(this.dllink);
                    handleConnectionErrors(br, con);
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    final String ext = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                    if (ext != null && title != null) {
                        link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + ext));
                    }
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
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