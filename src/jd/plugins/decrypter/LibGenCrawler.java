//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.LibGenInfo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class LibGenCrawler extends PluginForDecrypt {
    public LibGenCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        return prepBR(br, this.getHost());
    }

    public static Browser prepBR(final Browser br, final String host) {
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.setCookie(host, "lang", "en");
        return br;
    }

    private LibGenInfo hostPlugin = null;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        /* Keep in sync with hoster- and crawler plugin! */
        ret.add(new String[] { "libgen.gs", "libgen.lc", "libgen.rocks", "libgen.li", "libgen.org", "libgen.vg", "libgen.io", "gen.lib.rus.ec", "booksdl.org", "libgen.pm", "libgen.rs", "libgen.is", "libgen.st", "library.lol", "libgen.fun", "llhlf.com" });
        return ret;
    }

    public static final List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("libgen.lc");
        deadDomains.add("libgen.org");
        deadDomains.add("gen.lib.rus.ec");
        deadDomains.add("libgen.io");
        deadDomains.add("booksdl.org");
        deadDomains.add("library.lol");
        deadDomains.add("libgen.fun");
        return deadDomains;
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    public static String findMd5hashInURL(final String url) {
        if (url == null) {
            return null;
        }
        String md5 = null;
        try {
            md5 = UrlQuery.parse(url).get("md5");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (md5 == null) {
            md5 = new Regex(url, "(?i)https?://[^/]+/ads([a-f0-9]{32})").getMatch(0);
        }
        return md5;
    }

    private void ensureInitHosterplugin() throws PluginException {
        if (this.hostPlugin == null) {
            this.hostPlugin = (LibGenInfo) getNewPluginForHostInstance(this.getHost());
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Always use current host */
        String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String addedLinkDomain = Browser.getHost(contenturl, true);
        final List<String> deadDomains = getDeadDomains();
        String domainToUse = addedLinkDomain;
        if (deadDomains.contains(addedLinkDomain)) {
            domainToUse = this.getHost();
            contenturl = contenturl.replaceFirst(Pattern.quote(addedLinkDomain), domainToUse);
        }
        final Regex editionRegex = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/edition\\.php\\?id=(\\d+)");
        final Regex seriesRegex = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/series\\.php\\?id=(\\d+)");
        final Regex fileIDRegex = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/file\\.php\\?id=(\\d+)");
        if (editionRegex.patternFind()) {
            final String editionID = editionRegex.getMatch(0);
            /* They're nice and provide a public json view/API for programmers. */
            br.getPage("https://" + domainToUse + "/json.php?object=e&addkeys=*&ids=" + editionID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (entries.containsKey("error")) {
                /* E.g. {"error":"No Request keys"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> edition = (Map<String, Object>) entries.get(editionID);
            final Map<String, Object> files = (Map<String, Object>) edition.get("files");
            if (files == null || files.size() == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.ensureInitHosterplugin();
            final Iterator<Entry<String, Object>> iterator = files.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                final Map<String, Object> bookmap = (Map<String, Object>) entry.getValue();
                final String md5 = bookmap.get("md5").toString();
                final DownloadLink book = this.createDownloadlink(generateSingleFileDownloadurl(domainToUse, md5));
                book.setDefaultPlugin(this.hostPlugin);
                ret.add(book);
            }
        } else if (seriesRegex.patternFind()) {
            /* Crawl all items of a series */
            final String seriesID = seriesRegex.getMatch(0);
            br.getPage("https://" + domainToUse + "/json.php?object=s&addkeys=*&ids=" + seriesID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (entries.containsKey("error")) {
                /* E.g. {"error":"No Request keys"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> series = (Map<String, Object>) entries.get(seriesID);
            final Map<String, Object> editions = (Map<String, Object>) series.get("editions");
            final Iterator<Entry<String, Object>> iterator = editions.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> editionentry = iterator.next();
                ret.add(this.createDownloadlink("https://" + domainToUse + "/edition.php?id=" + editionentry.getKey()));
            }
        } else if (fileIDRegex.patternFind()) {
            final String fileID = fileIDRegex.getMatch(0);
            br.getPage("https://" + domainToUse + "/json.php?object=f&addkeys=*&ids=" + fileID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Object resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            if (!(resp instanceof Map)) {
                /* E.g. empty array */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = (Map<String, Object>) resp;
            if (entries.containsKey("error")) {
                /* E.g. {"error":"No Request keys"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> bookmap = (Map<String, Object>) entries.get(fileID);
            ret.addAll(processBookJson(bookmap, domainToUse));
        } else {
            final String md5 = findMd5hashInURL(contenturl);
            if (md5 == null) {
                /* Unsupported link */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            contenturl = this.generateSingleFileDownloadurl(domainToUse, md5);
            ensureInitHosterplugin();
            final DownloadLink book = this.createDownloadlink(generateSingleFileDownloadurl(domainToUse, md5));
            book.setDefaultPlugin(this.hostPlugin);
            final boolean useAPI = true;
            if (useAPI) {
                /* Let hosterplugin process single link */
                ret.add(book);
            } else {
                /* Website mode */
                /* TODO: Decide if this is still needed. If so: Add settings for API/mirrors/cover_url */
                br.getPage(contenturl);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.containsHTML("(?i)entry not found in the database")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String title = br.getRegex(md5 + "[^>]*>([^<>\"]+)<").getMatch(0);
                if (title != null) {
                    title = Encoding.htmlDecode(title).trim();
                }
                final String[] urls;
                String mirrorsHTML = br.getRegex("(?i)>\\s*Mirrors:\\s*</font></td>(.*?)</td></tr>").getMatch(0);
                if (mirrorsHTML != null) {
                    urls = HTMLParser.getHttpLinks(mirrorsHTML, br.getURL());
                } else {
                    /* Fallback */
                    urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
                }
                /* Add selfhosted mirror to host plugin */
                jd.plugins.hoster.LibGenInfo.parseFileInfoWebsite(book, this.br);
                book.setAvailable(true);
                ret.add(book);
                if (book.isNameSet()) {
                    /* This is the better packagename! */
                    title = book.getName();
                }
                if (urls.length > 0) {
                    for (final String url : urls) {
                        if (this.canHandle(url) && url.contains(md5)) {
                            ret.add(createDownloadlink(url));
                        }
                    }
                }
                final String cover_art_url = br.getRegex("(?i)(/covers/\\d+/[^<>\"\\']+\\.(?:jpg|jpeg|png|gif))").getMatch(0);
                if (cover_art_url != null) {
                    final DownloadLink cover = createDownloadlink(DirectHTTP.createURLForThisPlugin(br.getURL(cover_art_url).toExternalForm()));
                    if (title != null) {
                        final String ext = getFileNameExtensionFromString(cover_art_url, ".jpg");
                        if (ext != null) {
                            String filename_cover = correctOrApplyFileNameExtension(title, ext);
                            cover.setFinalFileName(filename_cover);
                        }
                    }
                    cover.setAvailable(true);
                    ret.add(cover);
                }
                final FilePackage fp = FilePackage.getInstance();
                if (title != null) {
                    title = Encoding.htmlDecode(title).trim();
                    fp.setName(title);
                } else {
                    /* Fallback */
                    fp.setName(md5);
                }
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> processBookJson(final Map<String, Object> bookmap, final String domainToUse) throws PluginException {
        ensureInitHosterplugin();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String md5 = bookmap.get("md5").toString();
        final DownloadLink book = this.createDownloadlink(generateSingleFileDownloadurl(domainToUse, md5));
        this.hostPlugin.parseFileInfoAPI(book, bookmap);
        book.setDefaultPlugin(this.hostPlugin);
        book.setAvailable(true);
        ret.add(book);
        return ret;
    }

    private String generateSingleFileDownloadurl(final String domain, final String md5) {
        return "https://" + domain + "/ads.php?md5=" + md5;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}