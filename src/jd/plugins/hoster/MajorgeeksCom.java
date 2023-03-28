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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.MajorgeeksComCrawler;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { MajorgeeksComCrawler.class })
public class MajorgeeksCom extends PluginForHost {
    public MajorgeeksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.majorgeeks.com/";
    }

    private static List<String[]> getPluginDomains() {
        return MajorgeeksComCrawler.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/mg/(?:getmirror|get)/([^/]+),(\\d+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean FREE_RESUME       = true;
    private final int     FREE_MAXCHUNKS    = 0;
    private final int     FREE_MAXDOWNLOADS = -1;
    /* 2022-11-25: Those links are now handled by a crawler plugin */
    @Deprecated
    private final String  PATTERN_LEGACY    = "https?://[^/]+/files/details/.+";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        }
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        if (urlinfo.matches()) {
            return urlinfo.getMatch(0) + "_" + urlinfo.getMatch(1);
        } else {
            /* For PATTERN_LEGACY */
            try {
                return new URL(link.getPluginPatternMatcher()).getPath();
            } catch (final Exception ignore) {
            }
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Set fallback-filename */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (getFinalRedirectURL(br) == null && !br.containsHTML("alt=\"restore download\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final boolean downloadFromExternalSite = br.containsHTML(">\\s*Download@Authors Site");
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (link.getPluginPatternMatcher().matches(PATTERN_LEGACY)) {
                /**
                 * There can be multiple versions available (multiple OS and 32/64 bit). </br> Download first version of software from
                 * website.
                 */
                final String continue_url = br.getRegex("(?i)\"/?(mg/get/[^,]*,\\d+\\.html)\"[^>]*><strong>Download").getMatch(0);
                if (continue_url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(continue_url);
            }
            final String continue_url2 = getFinalRedirectURL(br);
            if (continue_url2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // br.getHeaders().put("Referer", continue_url);
            br.setFollowRedirects(false);
            br.getPage(continue_url2);
            dllink = br.getRedirectLocation();
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Sometimes this will contain an affiliate redirect URL --> Make sure to filter the final downloadurl we're looking for! */
            String[] urls = HTMLParser.getHttpLinks(dllink, "");
            if (urls.length > 1) {
                logger.info("Avoiding affiliate redirection");
                dllink = urls[urls.length - 1];
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (downloadFromExternalSite) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Downloading from external sources is impossible");
            } else {
                /* 2020-01-29: Some downloads are just broken */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download failure");
            }
        }
        String final_filename = Plugin.getFileNameFromURL(new URL(dllink));
        if (final_filename != null) {
            /* 2020-01-29: They sometimes tag their files --> Fix that */
            final_filename = final_filename.replace(" - MajorGeeks", "");
            link.setFinalFileName(final_filename);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String getFinalRedirectURL(final Browser br) {
        return br.getRegex("\"/?(index\\.php\\?ct=files\\&action=download[^\"]+)").getMatch(0);
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
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
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

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}