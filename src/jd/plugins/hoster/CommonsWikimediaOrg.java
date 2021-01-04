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
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.URLHelper;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "commons.wikimedia.org" }, urls = { "https?://commons\\.wikimedia\\.org/wiki/File:.+|https?://[a-z]{2}\\.wikipedia\\.org/wiki/([^/]+/media/)?[A-Za-z0-9%]+.*" })
public class CommonsWikimediaOrg extends PluginForHost {
    public CommonsWikimediaOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private static final boolean use_api           = true;
    private String               dllink            = null;
    private static final String  TYPE_WIKIPEDIA_1  = "https?://commons\\.wikimedia\\.org/wiki/(File:.+)";
    private static final String  TYPE_WIKIPEDIA_2  = "https?://([a-z]{2})\\.wikipedia\\.org/wiki/([^/]+/media/)?([A-Za-z0-9%]+.*)";

    @Override
    public String getAGBLink() {
        return "https://wikimediafoundation.org/wiki/Terms_of_Use";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(400);
        String filename = null;
        long filesize = 0;
        String filesize_str = null;
        final String host = Browser.getHost(link.getPluginPatternMatcher());
        String url_title;
        if (link.getPluginPatternMatcher().matches(TYPE_WIKIPEDIA_1)) {
            url_title = new Regex(link.getPluginPatternMatcher(), TYPE_WIKIPEDIA_1).getMatch(0);
        } else {
            url_title = new Regex(link.getPluginPatternMatcher(), TYPE_WIKIPEDIA_2).getMatch(2);
        }
        url_title = Encoding.urlDecode(url_title, false);
        if (use_api) {
            /* Docs: https://www.mediawiki.org/wiki/API:Query */
            br.getPage("https://" + host + "/w/api.php?action=query&format=json&prop=imageinfo&titles=" + Encoding.urlEncode(url_title) + "&iiprop=timestamp%7Curl%7Csize%7Cmime%7Cmediatype%7Cextmetadata&iiextmetadatafilter=DateTime%7CDateTimeOriginal%7CObjectName%7CImageDescription%7CLicense%7CLicenseShortName%7CUsageTerms%7CLicenseUrl%7CCredit%7CArtist%7CAuthorCount%7CGPSLatitude%7CGPSLongitude%7CPermission%7CAttribution%7CAttributionRequired%7CNonFree%7CRestrictions&iiextmetadatalanguage=en&uselang=content&smaxage=300&maxage=300");
            if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("\"invalid\"")) { // ""missing" too?
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Object batchcomplete = entries.get("batchcomplete");
            if (batchcomplete == null) {
                /* No success response */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = new Regex(url_title, ".*?:(.+)").getMatch(0);
            if (filename == null) {
                filename = url_title;
            }
            dllink = PluginJSonUtils.getJsonValue(this.br, "url");
            if (StringUtils.isEmpty(dllink)) {
                if (!link.getPluginPatternMatcher().matches(TYPE_WIKIPEDIA_2)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_WIKIPEDIA_2);
                /* Fallback to PDF download */
                this.dllink = "https://" + urlinfo.getMatch(0) + ".wikipedia.org/api/rest_v1/page/pdf/" + urlinfo.getMatch(2);
                filename += ".pdf";
            }
            filesize_str = PluginJSonUtils.getJsonValue(this.br, "size");
            if (filesize_str != null) {
                filesize = Long.parseLong(filesize_str);
            }
        } else {
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /*
             * URL is different for every country e.g. https://en.wikipedia.org/wiki/File:Krak%C3%B3w_G%C5%82%C3%B3wny_(budynek_dworca).JPG,
             * https://pl.wikipedia.org/wiki/Plik:Dworzec_Krak%C3%B3w_G%C5%82%C3%B3wny.jpg
             */
            filename = br.getRegex("\"wgTitle\":\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                filename = url_title;
            }
            filesize_str = this.br.getRegex("file size: (\\d+(?:\\.\\d{1,2})? [A-Za-z]+)").getMatch(0);
            if (filesize_str == null) {
                filesize_str = this.br.getRegex("(?-i)class=\"fileInfo\">[^<]*?(\\d+(\\.\\d+)?\\s*[KMGT]B)").getMatch(0);
            }
            dllink = br.getRegex("id=\"file\"><a href=\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                /* E.g. https://commons.wikimedia.org/wiki/File:BBH_gravitational_lensing_of_gw150914.webm */
                dllink = br.getRegex("<a href=\"(https?://[^<>\"]+)\"[^<>]+>Original file</a>").getMatch(0);
            }
            if (dllink == null) {
                /* E.g. https://commons.wikimedia.org/wiki/File:Sintel_movie_720x306.ogv */
                dllink = br.getRegex("<source src=\"(https?://[^<>\"]+)\"[^<>]+data\\-title=\"Original").getMatch(0);
            }
            if (dllink == null) {
                /* E.g. https://zh.wikipedia.org/wiki/File:%E9%84%AD%E7%A7%80%E6%96%87_%E5%8E%BB%E6%84%9B%E5%90%A7.jpg */
                dllink = br.getRegex("\"fullImageLink\"\\s*id=\"file\"><a href=\"((https?)?:?//.*?)\"").getMatch(0);
            }
            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!dllink.startsWith("http")) {
                dllink = URLHelper.parseLocation(br._getURL(), dllink);
            }
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = encodeUnicode(filename);
            final String ext = getFileNameExtensionFromString(dllink, ".jpg");
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            if (filesize_str != null) {
                filesize = SizeFormatter.getSize(filesize_str);
            } else {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = brc.openHeadConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            filesize = con.getCompleteContentLength();
                        }
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        link.setFinalFileName(filename);
        if (filesize > 0) {
            link.setDownloadSize(filesize);
            link.setVerifiedFileSize(filesize);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error occured");
            }
        }
        dl.startDownload();
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
