//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

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
public class ImageVenueCom extends PluginForHost {
    public ImageVenueCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imagevenue.com" });
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
            final String hostsPatternPart = buildHostsPatternPart(domains);
            String regex = "https?://img[0-9]+\\." + hostsPatternPart + "/img\\.php\\?(loc=[^&]+\\&)?image=.+";
            regex += "|" + getThumbnailPatternOld(hostsPatternPart);
            regex += "|" + getThumbnailPatternNew(hostsPatternPart);
            regex += "|https?://(?:www\\.)?" + hostsPatternPart + "/view/o/\\?i=[^\\&]+\\&h=[^\\&]+";
            // galleries start with GA, images with ME?
            regex += "|https?://(?:www\\.)?" + hostsPatternPart + "/(?!GA)[A-Za-z0-9]+";
            regex += "|https?://cdn-images\\." + hostsPatternPart + "/[^/]+/[^/]+/[^/]+/[A-Za-z0-9]+[^/\\?]*(\\.png|\\.jpe?g)";
            regex += "|https?://cdno-data\\." + hostsPatternPart + "/html\\.[^/]+/upload\\d+/loc\\d+/[^/]+(\\.png|\\.jpe?g)";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    private static String getThumbnailPatternOld(final String domainpart) {
        return "https?://(img[0-9]+)\\." + domainpart + "/(loc\\d+)/th_(\\d+[^/]+)";
    }

    private static String getThumbnailPatternNew(final String domainpart) {
        return "https?://cdn-thumbs\\." + domainpart + "/[^/]+/[^/]+/[^/]+/([A-Z0-9]+)_t\\.jpg";
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    private String getFilenameFromURL(final DownloadLink link) throws MalformedURLException {
        final String url = link.getPluginPatternMatcher();
        final UrlQuery query = UrlQuery.parse(url);
        String name = query.get("image");
        if (name == null) {
            name = query.get("i");
        }
        if (name == null) {
            /* Final fallback */
            name = Plugin.getFileNameFromURL(new URL(url));
        }
        return name;
    }

    private String getContentURL(final DownloadLink link) {
        final String url = link != null ? link.getPluginPatternMatcher() : null;
        final String cdnImageD = new Regex(url, "https?://cdn-images\\.[^/]*/[^/]+/[^/]+/[^/]+/([A-Za-z0-9]+)").getMatch(0);
        final Regex thumbnailOld;
        final Regex thumbnailNew;
        if (cdnImageD != null) {
            /* cdn-images to normal urls */
            return "https://www." + getHost() + "/" + cdnImageD;
        } else if ((thumbnailOld = new Regex(url, getThumbnailPatternOld("[^/]+"))).patternFind()) {
            return generateContentURLOld(thumbnailOld.getMatch(2), thumbnailOld.getMatch(0));
        } else if ((thumbnailNew = new Regex(url, getThumbnailPatternNew("[^/]+"))).patternFind()) {
            return generateContentURLNew(thumbnailNew.getMatch(0));
        } else {
            final String cdnodata[] = new Regex(url, "https?://cdno-data\\.[^/]*/html\\.([^/]+)/upload\\d+/loc\\d+/([^/]+(?:\\.png|\\.jpe?g))").getRow(0);
            if (cdnodata != null) {
                /* cdno-data to normal urls */
                return generateContentURLOld(cdnodata[1], cdnodata[0]);
            }
        }
        /* Return URL which was added by the user and assume that's already the one we want. */
        return link.getPluginPatternMatcher();
    }

    private String generateContentURLOld(final String i, final String imageServer) {
        return "https://www." + getHost() + "/view/o/?i=" + i + "&h=" + imageServer;
    }

    private String generateContentURLNew(final String imageID) {
        return "https://www." + getHost() + "/" + imageID;
    }

    private String dllink = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* Offline links should also have nice filenames */
        final String fallbackFilename = getFilenameFromURL(link);
        if (!link.isNameSet() && fallbackFilename != null) {
            link.setName(fallbackFilename);
        }
        this.br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        final String contentURL = getContentURL(link);
        br.getPage(contentURL);
        /* Error handling */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)This image does not exist on this server|<title>404 Not Found</title>|>The requested URL /img\\.php was not found on this server\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(?i)Continue to your image|Continue to ImageVenue")) {
            /* Extra step */
            br.getPage(contentURL);
        }
        dllink = br.getRegex("data-toggle=\"full\">\\s*<img src=\"(https?://[^<>\"]+)\"").getMatch(0);
        if (dllink == null) {
            /* 2023-07-24 */
            dllink = br.getRegex("<img src=\"(https?://[^\"]+)\"[^>]*id=\"main-image\"").getMatch(0);
        }
        if (dllink == null) {
            if (br.containsHTML("tempval\\.focus\\(\\)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                logger.warning("Could not find finallink reference");
            }
        }
        String filename = br.getRegex("<title>\\s*(?:ImageVenue.com\\s*-)?\\s*(.*?)\\s*</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("src=\"https?://[^\"]+\"[^>]*alt=\"([^<>\"]+)\"").getMatch(0);
        }
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                con = brc.openHeadConnection(dllink);
                final String etag = con.getRequest().getResponseHeader("etag");
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (StringUtils.equalsIgnoreCase(etag, "\"182f722b7-171e-479ab58eae440\"")) {
                    /* 2022-07-18: Special "Image removed copyright violation" dummy image. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (StringUtils.equalsIgnoreCase(etag, "\"19fdf2cd6-383c-5a4cd5b6710ed\"")) {
                    /* 2021-08-27: Special "404 not image unavailable" dummy picture. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    final long filesize = con.getCompleteContentLength();
                    if (filesize > 0) {
                        link.setVerifiedFileSize(filesize);
                    }
                }
            } finally {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}