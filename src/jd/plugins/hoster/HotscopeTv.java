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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HotscopeTv extends PluginForHost {
    public HotscopeTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "hotscope.tv" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\w+)/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://hotscope.tv/terms";
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
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        return urlinfo.getMatch(0) + "_" + urlinfo.getMatch(1);
    }

    private final String PROPERTY_DATE               = "date";
    private final String PROPERTY_USERNAME           = "user";
    private final String PROPERTY_VIEWS              = "views";
    private final String PROPERTY_PERCENTAGE_UPVOTES = "percentage_upvotes";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("\"name\"\\s*:\\s*\"([^\"]+)").getMatch(0);
        dllink = br.getRegex("\"contentUrl\"\\s*:\\s*\"(http[^\"]+)").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setFinalFileName(title + ".mp4");
        }
        /* Packagizer properties */
        final String username = br.getRegex("<a title=\"([^\"]+)\" href=\"/channel/\\d+\"").getMatch(0);
        if (username != null) {
            link.setProperty(PROPERTY_USERNAME, Encoding.htmlDecode(username).trim());
        }
        final String numberofViews = br.getRegex("\"views\"\\s*:\\s*(\\d+)").getMatch(0);
        if (numberofViews != null) {
            link.setProperty(PROPERTY_VIEWS, numberofViews);
        }
        final String upvotesPercentage = br.getRegex("class=\"MuiTypography-root MuiTypography-body1 MuiTypography-colorTextSecondary\">(\\d+)<\\!-- -->%").getMatch(0);
        if (upvotesPercentage != null) {
            link.setProperty(PROPERTY_PERCENTAGE_UPVOTES, upvotesPercentage);
        }
        final String uploadDate = br.getRegex("\"uploadDate\"\\s*:\\s*\"([^\"]+)").getMatch(0);
        if (PROPERTY_DATE != null) {
            link.setProperty(PROPERTY_USERNAME, uploadDate);
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final URLConnectionAdapter con) throws PluginException {
        if (!this.looksLikeDownloadableContent(con)) {
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
        }
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