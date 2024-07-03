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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.GelbooruComConfig;
import org.jdownloader.plugins.components.config.GelbooruComConfig.FilenameScheme;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GelbooruCom extends PluginForHost {
    public GelbooruCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "gelbooru.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/index\\.php\\?page=post\\&s=view\\&id=(\\d+)");
        }
        return ret.toArray(new String[0]);
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
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setCookie(getHost(), "fringeBenefits", "yup");
        final String extDefault = ".jpg";
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid + extDefault);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.getURL().contains(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("(?i)<title>([^<>\"]+) - Image View -.*?</title>").getMatch(0);
        if (title != null && false) {
            // filename can be too long
            title = fid + "_" + title;
        } else {
            title = fid;
        }
        dllink = br.getRegex("<a href=\"([^<>\"\\']+)\"[^<>]+>Original image</a>").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?[^<>\"]+)\" id=\"image\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(gelbooru\\.com//images/[^<>\"]+)\"").getMatch(0);
            }
            if (dllink == null) {
                /* 2017-02-18 */
                String imglink = br.getRegex("Resize image.*?<a href=(\"|'|)(.*?)\\1").getMatch(1);
                if (imglink == null) {
                    imglink = br.getRegex("<img alt=.*?src=(\"|'|)(.*?)\\1").getMatch(1);
                }
            }
            if (dllink == null) {
                // can be a video!
                dllink = br.getRegex("<\\s*source\\s+[^>]*src\\s*=\\s*(\"|'|)(.*?)\\1").getMatch(1);
            }
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        final GelbooruComConfig cfg = PluginJsonConfig.get(GelbooruComConfig.class);
        final FilenameScheme scheme = cfg.getPreferredFilenameScheme();
        final String originalFilename = dllink != null ? getFileNameFromURL(new URL(dllink)) : null;
        String ext = extDefault;
        if (scheme == FilenameScheme.SERVER_FILENAME && originalFilename != null) {
            link.setFinalFileName(originalFilename);
        } else if (title != null) {
            ext = getFileNameExtensionFromString(dllink, ext);
            link.setFinalFileName(this.applyFilenameExtension(title, ext));
        }
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            dllink = Encoding.htmlOnlyDecode(dllink);
            dllink = br.getURL(dllink).toExternalForm();
            if (!isDownload) {
                basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, scheme == FilenameScheme.SERVER_FILENAME && originalFilename != null ? originalFilename : title, ext);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), 1);
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

    @Override
    public Class<? extends GelbooruComConfig> getConfigInterface() {
        return GelbooruComConfig.class;
    }
}
