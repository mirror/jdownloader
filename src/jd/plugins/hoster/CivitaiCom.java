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
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CivitaiCom extends PluginForHost {
    public CivitaiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://civitai.com/content/tos";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "civitai.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/images/\\d+.*");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final int     FREE_MAXCHUNKS     = 0;
    private final int     FREE_MAXDOWNLOADS  = -1;
    private final Pattern PATTERN_IMAGE      = Pattern.compile("https?://[^/]+/images/(\\d+).*", Pattern.CASE_INSENSITIVE);
    private final String  PROPERTY_DIRECTURL = "directurl";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "civitai://image/" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), PATTERN_IMAGE).getMatch(0);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String imageID = this.getFID(link);
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(imageID + ".jpg");
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 503 service unavailable");
        }
        final String json = br.getRegex("type=\"application/json\"[^>]*>(\\{\"props.*?)</script>").getMatch(0);
        final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
        final Map<String, Object> imagemap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/trpcState/json/queries/{0}/state/data");
        if (imagemap == null) {
            /* Invalid link e.g. https://civitai.com/images/1234567 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> metadata = (Map<String, Object>) imagemap.get("metadata");
        String filename = (String) imagemap.get("name");
        final Number filesize = (Number) metadata.get("size");
        if (!StringUtils.isEmpty(filename)) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        } else {
            final String mimeType = (String) imagemap.get("mimeType");
            final String ext = Plugin.getExtensionFromMimeTypeStatic(mimeType);
            if (ext != null) {
                link.setName(imageID + "." + ext);
            }
        }
        if (filesize != null) {
            link.setDownloadSize(filesize.longValue());
        }
        /**
         * 2024-03-11: Important: Do not open up the regex for original image too much or you run into risk of accidentally downloading the
         * wrong image, see: </br> https://board.jdownloader.org/showthread.php?t=95419
         */
        final String directurlOriginal = br.getRegex("class=\"mantine-it6rft\" src=\"(https?://image\\.civitai\\.com/[^\"]+/original=true/[^\"]+)").getMatch(0);
        if (directurlOriginal != null) {
            /* Best case: We can download the original file. */
            link.setProperty(PROPERTY_DIRECTURL, directurlOriginal);
        } else {
            /* 2023-09-11: Base URL hardcoded from: https://civitai.com/_next/static/chunks/pages/_app-191d571abe9dc30e.js */
            final String baseURL = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/";
            final String directurl = baseURL + imagemap.get("url") + "/width=" + metadata.get("width") + "/" + Encoding.urlEncode(filename);
            link.setProperty(PROPERTY_DIRECTURL, directurl);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find final downloadurl");
        }
        final String widthValue = new Regex(dllink, "(/width=\\d+[^/]*/)").getMatch(0);
        if (widthValue != null) {
            /* Special: Replace the 'width' part with 'original=true does in some cases grant us access to download the original image. */
            final String modifiedOriginalURL = dllink.replace(widthValue, "/original=true/");
            logger.info("Trying to download original image via modified URL: " + modifiedOriginalURL);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, modifiedOriginalURL, this.isResumeable(link, null), maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                logger.info("Failed to download original image with trick -> Download normal image");
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), maxchunks);
            }
        } else {
            /* Download the URL we have */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), maxchunks);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media?");
            }
        }
        final String filename = link.getName();
        final String ext = Plugin.getExtensionFromMimeTypeStatic(dl.getConnection().getContentType());
        if (ext != null && filename != null) {
            link.setFinalFileName(this.correctOrApplyFileNameExtension(filename, "." + ext));
        }
        dl.startDownload();
    }

    @Override
    protected boolean looksLikeDownloadableContent(URLConnectionAdapter urlConnection) {
        if (super.looksLikeDownloadableContent(urlConnection)) {
            return true;
        } else if (urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) {
            return "text/plain".equals(urlConnection.getContentType()) && urlConnection.getCompleteContentLength() > 1024;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
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