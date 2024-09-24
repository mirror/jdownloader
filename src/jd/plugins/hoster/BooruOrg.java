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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.decrypter.BooruOrgCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { BooruOrgCrawler.class })
public class BooruOrg extends PluginForHost {
    public BooruOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    private static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.BooruOrgCrawler.getPluginDomains();
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/.*?index\\.php\\?page=post\\&s=view\\&id=(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2024-07-25: Main domain has changed from booru.org to tbib.org. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 1;
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/tos.php";
    }

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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        final String extDefault = ".jpg";
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRequest().getHtmlCode().length() <= 100) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(fid)) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        if (filename != null) {
            filename = fid + "_" + filename;
        } else {
            filename = fid;
        }
        dllink = br.getRegex("\"([^\"]+)\" id=\"image\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://img\\.booru\\.org//images/\\d+/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                // can be a video!
                dllink = br.getRegex("<\\s*source\\s+[^>]*src\\s*=\\s*(\"|'|)(.*?)\\1").getMatch(1);
            }
        }
        String ext = null;
        if (filename != null) {
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            ext = getFileNameExtensionFromString(dllink, extDefault);
            filename = applyFilenameExtension(filename, ext);
            link.setFinalFileName(filename);
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlOnlyDecode(dllink);
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, filename, ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
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
