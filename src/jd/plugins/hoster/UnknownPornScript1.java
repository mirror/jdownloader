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
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.decrypter.UnknownPornScript1Crawler.class })
public class UnknownPornScript1 extends PluginForHost {
    public UnknownPornScript1(PluginWrapper wrapper) {
        super(wrapper);
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.UnknownPornScript1Crawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        return jd.plugins.decrypter.UnknownPornScript1Crawler.buildAnnotationUrls(pluginDomains);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.1 */
    // Tags: For porn sites using the flowplayer videoplayer
    // other:
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.dansmovies.com/tos/";
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        dllink = null;
        final String titleFromURL = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0).replace("-", " ");
        if (!link.isNameSet()) {
            link.setName(titleFromURL + default_extension);
        }
        final String host = link.getHost();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = regexStandardTitleWithHost(host);
        if (title == null) {
            /* Fallback */
            title = titleFromURL;
        }
        final String html_clip = br.getRegex("clip: \\{(.*?)\\},").getMatch(0);
        if (html_clip != null) {
            dllink = new Regex(html_clip, "url: \\'(http[^<>\"]*?)\\'").getMatch(0);
            if (dllink.contains("torbenetwork")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /* E.g. xtwisted.com */
            dllink = br.getRegex("(?:\"|\\')(?:file|url)(?:\"|\\'):[\t\n\r ]*?(?:\"|\\')(http[^<>\"]*?\\.(?:mp4|flv))(?:\"|\\')").getMatch(0);
            if (dllink == null) {
                /* E.g. xtwisted.com */
                dllink = br.getRegex("<source src=\"(http[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (dllink == null && br.containsHTML("/embed/")) {
            br.getPage(br.getRegex("(http[^<>\"]+/embed/[^<>\"]+)\"").getMatch(0));
            dllink = br.getRegex("<source src=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        String ext = null;
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            if (dllink != null) {
                dllink = Encoding.htmlDecode(dllink);
                ext = getFileNameExtensionFromString(dllink, default_extension);
            } else {
                ext = default_extension;
            }
            link.setFinalFileName(this.applyFilenameExtension(title, ext));
        }
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, title, ext);
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*We're sorry, the content titled")) {
            /* 2017-02-18 xtwisted.com */
            return true;
        } else if (br.containsHTML("class=\"player\"[^>]*>\\s*Deleted\\s*</div>")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private String regexStandardTitleWithHost(final String host) {
        return this.br.getRegex(Pattern.compile("<title>([^<>\"]*?) \\- " + host + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript1;
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
