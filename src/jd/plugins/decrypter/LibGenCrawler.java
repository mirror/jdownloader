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
import jd.http.Request;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class LibGenCrawler extends PluginForDecrypt {
    public LibGenCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        /* Keep in sync with hoster- and crawler plugin! */
        ret.add(new String[] { "libgen.gs", "libgen.lc", "libgen.rocks", "libgen.li", "libgen.org", "gen.lib.rus.ec", "libgen.io", "booksdl.org", "libgen.pm", "libgen.rs", "libgen.is", "libgen.st", "library.lol", "libgen.fun" });
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/(item/index\\.php\\?md5=[A-Fa-f0-9]{32}|edition\\.php\\?id=\\d+|series\\.php\\?id=\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Always use current host */
        String addedurl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String addedLinkDomain = Browser.getHost(addedurl, true);
        final List<String> deadDomains = getDeadDomains();
        String domainToUse = addedLinkDomain;
        if (deadDomains.contains(addedLinkDomain)) {
            domainToUse = this.getHost();
            addedurl = addedurl.replaceFirst(Pattern.quote(addedLinkDomain), domainToUse);
        }
        final Regex editionRegex = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/edition\\.php\\?id=(\\d+)");
        final Regex seriesRegex = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/series\\.php\\?id=(\\d+)");
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
            final Iterator<Entry<String, Object>> iterator = files.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> file = iterator.next();
                final String md5 = ((Map<String, Object>) file.getValue()).get("md5").toString();
                final DownloadLink book = this.createDownloadlink("https://" + domainToUse + "/get.php?md5=" + md5);
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
        } else {
            final String md5 = UrlQuery.parse(addedurl).get("md5");
            if (md5 == null) {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie(this.getHost(), "lang", "en");
            br.setCustomCharset("utf-8");
            /* Allow redirects to other of their domains */
            br.setFollowRedirects(true);
            br.getPage(addedurl);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)entry not found in the database")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String title = br.getRegex(md5 + "[^>]*>([^<>\"]+)<").getMatch(0);
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
            }
            final String[] mirrors;
            String mirrorsHTML = br.getRegex("(?i)>\\s*Mirrors:\\s*</font></td>(.*?)</td></tr>").getMatch(0);
            if (mirrorsHTML != null) {
                mirrors = HTMLParser.getHttpLinks(mirrorsHTML, br.getURL());
            } else {
                /* Fallback */
                mirrors = HTMLParser.getHttpLinks(br.toString(), br.getURL());
            }
            /* Add selfhosted mirror to host plugin */
            final DownloadLink selfhosted = this.createDownloadlink("https://" + domainToUse + "/ads.php?md5=" + md5);
            jd.plugins.hoster.LibGenInfo.parseFileInfo(selfhosted, this.br);
            selfhosted.setAvailable(true);
            ret.add(selfhosted);
            if (selfhosted.isNameSet()) {
                /* This is the better packagename! */
                title = selfhosted.getName();
            }
            if (mirrors.length > 0) {
                for (final String mirror : mirrors) {
                    if (this.canHandle(mirror)) {
                        /* Skip URLs that would go back into this crawler. */
                        continue;
                    }
                    ret.add(createDownloadlink(mirror));
                }
            }
            final String cover_url = br.getRegex("(/covers/\\d+/[^<>\"\\']+\\.(?:jpg|jpeg|png|gif))").getMatch(0);
            if (cover_url != null) {
                final DownloadLink dl = createDownloadlink(Request.getLocation(cover_url, br.getRequest()));
                if (title != null) {
                    final String ext = getFileNameExtensionFromString(cover_url, ".jpg");
                    if (ext != null) {
                        String filename_cover = correctOrApplyFileNameExtension(title, ext);
                        dl.setFinalFileName(filename_cover);
                    }
                }
                dl.setAvailable(true);
                ret.add(dl);
            }
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}