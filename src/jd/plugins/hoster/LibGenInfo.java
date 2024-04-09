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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
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
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.LibGenCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { PornportalCom.class })
public class LibGenInfo extends PluginForHost {
    public LibGenInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        return LibGenCrawler.prepBR(br, this.getHost());
    }

    @Override
    public String rewriteHost(final String host) {
        /* Main domain may change frequently. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    public String getAGBLink() {
        return "http://libgen.lc/";
    }

    public static List<String[]> getPluginDomains() {
        return LibGenCrawler.getPluginDomains();
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
            /* Links get pushed in via crawler plugin. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getUniqueFileID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getUniqueFileID(final DownloadLink link) {
        return getMd5Hash(link);
    }

    private String getMd5Hash(final DownloadLink link) {
        if (link == null) {
            return null;
        } else {
            return LibGenCrawler.findMd5hashInURL(link.getPluginPatternMatcher());
        }
    }

    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 2;
    private static final String  TYPE_DIRECT       = "(?i)https?://[^/]+/(comics/.+|get\\.php\\?.+)";
    private static final String  TYPE_ADS          = "(?i)https?://[^/]+/ads\\.php\\?md5=([A-Fa-f0-9]{32})";
    public static final String   PROPERTY_BOOK_ID  = "book_id";

    private String getContentURL(final DownloadLink link) {
        String contenturl = link.getPluginPatternMatcher().replaceFirst("(?i)http://", "https://");
        final String addedLinkDomain = Browser.getHost(contenturl, true);
        final List<String> deadDomains = LibGenCrawler.getDeadDomains();
        if (deadDomains.contains(addedLinkDomain)) {
            contenturl = contenturl.replaceFirst(Pattern.quote(addedLinkDomain), this.getHost());
        }
        return contenturl;
    }

    private String getBookID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_BOOK_ID);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        /* @developer: Disable this if API handling fails for unknown reasons. */
        final boolean allowUseAPI = true;
        final String storedBookID = this.getBookID(link);
        /* 2024-04-09: Internal bookID is needed to obtain information from API. */
        if (allowUseAPI && storedBookID != null) {
            /* 2023-11-17 */
            return requestFileInformationAPI(link);
        } else {
            requestFileInformationWebsite(link);
            if (storedBookID == null && allowUseAPI) {
                /* Check if we can use the API now */
                final String newBookID = this.getBookID(link);
                if (newBookID != null) {
                    logger.info("BookID has been found in website handling -> Obtaining status via API");
                    return requestFileInformationAPI(link);
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link) throws Exception {
        final String md5 = getMd5Hash(link);
        final String bookID = getBookID(link);
        if (md5 == null && bookID == null) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        link.setMD5Hash(md5);
        final String contenturl = getContentURL(link);
        final UrlQuery query = new UrlQuery();
        query.add("object", "f");
        query.add("fields", "Title,Author,MD5,sha1,edition,id,extension,year,filename,filesize,locator");
        query.add("addkeys", "*");
        if (bookID != null) {
            query.add("ids", bookID);
        } else {
            query.add("md5", md5);
        }
        br.getPage("https://" + Browser.getHost(contenturl, true) + "/json.php?" + query.toString());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Object responseO = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
        if (!(responseO instanceof Map)) {
            /* E.g. response is empty array -> Item offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        /* First and only item in this map is the one we want. */
        final Map<String, Object> book = (Map<String, Object>) entries.entrySet().iterator().next().getValue();
        parseFileInfoAPI(link, book);
        return AvailableStatus.TRUE;
    }

    public void parseFileInfoAPI(final DownloadLink link, final Map<String, Object> book) {
        final String md5 = book.get("md5").toString();
        final String extension = (String) book.get("extension");
        final String filesizeBytesStr = book.get("filesize").toString();
        final String internalPath = book.get("locator").toString();
        final String filename;
        if (internalPath.contains("\\")) {
            filename = new Regex(internalPath, "\\\\([^\\\\]+)$").getMatch(0);
        } else {
            filename = internalPath;
        }
        if (!StringUtils.isEmpty(filename)) {
            if (extension != null) {
                link.setFinalFileName(this.applyFilenameExtension(filename, "." + extension));
            } else {
                link.setFinalFileName(filename);
            }
        } else {
            /* Fallback */
            if (extension != null) {
                link.setName(md5 + "." + extension);
            } else {
                /* Final fallback */
                link.setName(md5 + ".pdf");
            }
        }
        if (filesizeBytesStr != null && filesizeBytesStr.matches("\\d+")) {
            link.setVerifiedFileSize(Long.parseLong(filesizeBytesStr));
        }
        link.setMD5Hash(md5);
    }

    @Deprecated
    private AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        final String md5 = getMd5Hash(link);
        link.setMD5Hash(md5);
        final String contenturl = getContentURL(link);
        /* Old/Website handling */
        if (contenturl.matches(TYPE_DIRECT)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(contenturl);
                if (this.looksLikeDownloadableContent(con)) {
                    /* File is direct-downloadable */
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    link.setFinalFileName(Plugin.getFileNameFromDispositionHeader(con));
                    return AvailableStatus.TRUE;
                } else {
                    br.followConnection();
                    /* Either redirect to supported pattern such as "/ads.php..." or offline. */
                    if (!this.canHandle(br.getURL())) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        parseFileInfoWebsite(link, this.br);
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else if (contenturl.matches(TYPE_ADS)) {
            br.getPage(contenturl);
            if (br.containsHTML("(?i)>\\s*File not found in DB")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            parseFileInfoWebsite(link, this.br);
        } else {
            br.getPage("http://" + this.getHost() + "/item/index.php?md5=" + md5);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            parseFileInfoWebsite(link, this.br);
        }
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfoWebsite(final DownloadLink link, final Browser br) throws MalformedURLException {
        final String md5FromURL = UrlQuery.parse(link.getPluginPatternMatcher()).get("md5");
        final String filesizeBytesStr = br.getRegex("\\((\\d+) B\\)").getMatch(0);
        final String author = br.getRegex("author\\s*=\\s*\\{([^\\}]+)\\}").getMatch(0);
        final String title = br.getRegex("title\\s*=\\s*\\{([^\\}]+)\\}").getMatch(0);
        final String series = br.getRegex("series\\s*=\\s*\\{([^\\}]+)\\}").getMatch(0);
        final String bookID = br.getRegex("book:\\{(\\d+)").getMatch(0);
        // String sha256hash = br.getRegex("SHA256</th><td>([A-Fa-f0-9 ]+)").getMatch(0);
        // if (sha256hash != null) {
        // sha256hash = sha256hash.replace(" ", "").trim();
        // }
        // final String year = br.getRegex("year\\s*=\\s*\\{(\\d+)\\}").getMatch(0);
        // final String publisher = br.getRegex("publisher\\s*=\\s*\\{([^\\}]+)\\}").getMatch(0);
        if (bookID != null) {
            link.setProperty(PROPERTY_BOOK_ID, bookID);
        }
        String filename = null;
        String ext = null;
        final String defaultExt = ".pdf";
        if (author != null && series != null && title != null) {
            filename = Encoding.htmlDecode(author).trim() + " - " + Encoding.htmlDecode(series).trim() + " - " + Encoding.htmlDecode(title).trim();
        } else if (author != null && title != null) {
            filename = Encoding.htmlDecode(author).trim() + " - " + Encoding.htmlDecode(title).trim();
        } else if (title != null) {
            filename = Encoding.htmlDecode(title).trim();
        } else if (series != null) {
            /*
             * Rare case: Only series name available. This may also sometimes happen when the uploader sets title as series-name by mistake.
             */
            filename = Encoding.htmlDecode(series).trim();
        }
        if (ext == null) {
            /* Fallback */
            ext = defaultExt;
        }
        if (filename == null) {
            filename = br.getRegex("ed2k://\\|file\\|([^\\|]+)\\|\\d+\\|").getMatch(0);
        }
        if (filename != null) {
            link.setName(filename);
        } else if (md5FromURL != null && !link.isNameSet()) {
            /* Fallback */
            link.setName(md5FromURL + ext);
        }
        if (filesizeBytesStr != null) {
            link.setVerifiedFileSize(Long.parseLong(filesizeBytesStr));
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink;
        if (this.looksLikeDownloadableContent(br.getHttpConnection())) {
            /* Direct-URL */
            dllink = br.getURL();
        } else {
            dllink = findDownloadurl(br);
            if (dllink == null) {
                /* Access TYPE_ADS URL if that hasn't already happened. */
                if (br.getURL() == null || !br.getURL().matches(TYPE_ADS)) {
                    br.getPage("/ads.php?md5=" + this.getMd5Hash(link));
                    dllink = findDownloadurl(br);
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML(">\\s*Sorry, huge and large files are available to download in local network only, try later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            } else if (br.containsHTML("too many or too often downloads\\.")) {
                final String wait = br.getRegex("wait for (\\d+)hrs automatic amnesty").getMatch(0);
                if (wait != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many downloads", Integer.parseInt(wait) * 60 * 60 * 1001l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many downloads", 1 * 60 * 60 * 1001l);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to a file");
            }
        }
        final String headerFilename = Plugin.getFileNameFromHeader(dl.getConnection());
        if (headerFilename != null) {
            /* Do some corrections */
            String finalFilename = Encoding.htmlDecode(headerFilename);
            final String removeMe = new Regex(finalFilename, "(?i)( - libgen\\.[a-z0-9]+)\\.(epub|pdf)$").getMatch(0);
            if (removeMe != null) {
                finalFilename = finalFilename.replaceFirst(Pattern.quote(removeMe), "");
            }
            link.setFinalFileName(finalFilename);
        }
        dl.startDownload();
    }

    private String findDownloadurl(final Browser br) {
        /** 2023-12-19: They've crippled the direct-URLs on purpose so we'll first fix the html source and then regex that link. */
        final String correctedHTML = br.getRequest().getHtmlCode().replace("\\", "/");
        String dllink = new Regex(correctedHTML, "<a\\s*href\\s*=\\s*(\"|')((?:https?:)?(?://[\\w\\-\\./]+)?/?get\\.php\\?md5=[a-f0-9]{32}.*?)\\1").getMatch(1);
        if (dllink == null) {
            /* 2024-04-09 */
            dllink = new Regex(correctedHTML, "\"(https?://download\\.[^/]+/[^\"]+)").getMatch(0);
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}